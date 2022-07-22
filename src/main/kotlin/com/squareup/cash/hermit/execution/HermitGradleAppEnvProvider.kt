package com.squareup.cash.hermit.execution

import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import com.squareup.cash.hermit.Hermit
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Application executions going to the Gradle executor
 */
class HermitGradleAppEnvProvider : GradleExecutionEnvironmentProvider {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean {
        if (task != null && task.runProfile is GradleRunConfiguration) {
            val profile = task.runProfile as GradleRunConfiguration
            return Hermit(profile.project).hasHermit()
        }
        return false
    }

    override fun createExecutionEnvironment(project: Project?, task: ExecuteRunConfigurationTask?, executor: Executor?): ExecutionEnvironment? {
        log.info("creating gradle execution environment for project: ${project?.name}, task: ${task?.presentableName}, executor: ${executor?.id} ")
        if (project == null || task == null || executor == null) {
            return null
        }
        var environment = delegateProvider(task) ?.let {
            it.createExecutionEnvironment(project, task, executor)
        }

        // No registered environment provider. See if there is a hard coded environment for this execution type
        // This might return null if the runner being used has not had its id registered in
        // ExternalSystemUtil.RUNNER_IDS
        if (environment == null) {
            log.debug("attempting ExternalSystemUtil.createExecutionEnvironment")
            val profile = task.runProfile as GradleRunConfiguration
            val settings = profile.settings.clone()
            environment = ExternalSystemUtil.createExecutionEnvironment(profile.project, profile.settings.externalSystemId, settings, executor.id)
        }

        // Finally, create environment from the runner, if available
        // This is here to make sure we can inject the environment variables to the environment
        // that would otherwise be created in ExecutionEnvironmentBuilder after this
        // extension point is called.
        // Without this, for example the code coverage runs would not get the Hermit environment
        if (environment == null) {
            log.debug("attempting to get a Runner")
            val runner = ProgramRunner.getRunner(executor.id, task.runProfile)
            if (runner != null && task.settings != null) {
                environment = ExecutionEnvironment(executor, runner, task.settings!!, project)
            }
        }

        if (environment?.runProfile is GradleRunConfiguration) {
            log.debug("configuring the environment")
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