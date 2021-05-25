package com.squareup.cash.hermit.execution

import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.task.ExecuteRunConfigurationTask
import com.squareup.cash.hermit.Hermit
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Gradle Task executions. Note, that application executions using Gradle go via HermitGradleAppEnvProvider
 */
class HermitGradleTaskEnvProvider : GradleExecutionEnvironmentProvider {
    override fun isApplicable(task: ExecuteRunConfigurationTask?): Boolean {
        if (task != null && task.runProfile is GradleRunConfiguration) {
            val profile = task.runProfile as GradleRunConfiguration
            return Hermit(profile.project).hasHermit()
        }
        return false
    }

    override fun createExecutionEnvironment(project: Project?, task: ExecuteRunConfigurationTask?, executor: Executor?): ExecutionEnvironment? {
        if (project == null || task == null || executor == null || task.settings == null) {
            return null
        }
        val profile = task.runProfile as GradleRunConfiguration
        val taskSettings = profile.settings

        // We can not update task.settings directly, as these are saved back to the UI later
        val settings = ExternalSystemTaskExecutionSettings()
        settings.isPassParentEnvs = taskSettings.isPassParentEnvs
        settings.env = Hermit(project).environment().patch(taskSettings.env)
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
        settings.externalProjectPath = taskSettings.externalProjectPath
        settings.taskNames = taskSettings.taskNames

        return ExternalSystemUtil.createExecutionEnvironment(profile.project, GradleConstants.SYSTEM_ID, settings, executor.id)
    }
}