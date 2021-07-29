package com.squareup.cash.hermit.execution

import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import com.squareup.cash.hermit.Hermit
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

/**
 * Application executions going to the Gradle executor
 */
class HermitGradleAppEnvProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean {
        if (task != null && task.runProfile is GradleRunConfiguration) {
            val profile = task.runProfile as GradleRunConfiguration
            return Hermit(profile.project).hasHermit()
        }
        return false
    }

    override fun createExecutionEnvironment(project: Project?, task: ExecuteRunConfigurationTask?, executor: Executor?): ExecutionEnvironment? {
        if (project == null || task == null || executor == null) {
            return null
        }
        val environment = delegateProvider(task) ?.let {
            it.createExecutionEnvironment(project, task, executor)
        }

        if (environment?.runProfile is GradleRunConfiguration) {
            val config = environment.runProfile as GradleRunConfiguration
            config.settings.env = Hermit(project).environment().patch(config.settings.env)
        }
        return environment
    }

    /**
     * Rely on the existing GradleApplicationEnvironmentProvider to give us the initial config
     */
    private fun delegateProvider(task: ExecuteRunConfigurationTask): GradleExecutionEnvironmentProvider? {
        val extensions = GradleExecutionEnvironmentProvider.EP_NAME.extensions()
        return extensions
            .filter { provider -> provider !== this && provider.isApplicable(task) }
            .findFirst().orElse(null)
    }
}