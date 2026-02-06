package com.squareup.cash.hermit.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.Hermit
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings

/**
 * Injects Hermit environment variables (including JAVA_HOME) into Gradle task executions.
 *
 * Without this, the IDE's own JAVA_HOME leaks into Gradle daemon processes because
 * [GradleExecutionSettings.getEnv] is empty by default and the parent process environment
 * is inherited.
 */
class HermitGradleEnvProvider : GradleTaskManagerExtension {

    override fun configureTasks(
        projectPath: String,
        id: ExternalSystemTaskId,
        settings: GradleExecutionSettings,
        gradleVersion: GradleVersion?
    ) {
        val project = id.findProject() ?: return
        injectHermitEnvironment(project, settings, "Gradle task execution")
    }

    override fun cancelTask(
        id: ExternalSystemTaskId,
        listener: ExternalSystemTaskNotificationListener
    ): Boolean = false

    internal companion object {
        private val log: Logger = Logger.getInstance(HermitGradleEnvProvider::class.java)

        /**
         * Injects Hermit environment variables into [settings] if the [project] has an active
         * Hermit environment. This is the shared logic used by both task execution and sync.
         */
        fun injectHermitEnvironment(project: Project, settings: GradleExecutionSettings, context: String) {
            val hermit = Hermit(project)
            if (!hermit.hasHermit() || hermit.hermitStatus() == Hermit.HermitStatus.Disabled) return

            val env = hermit.environment().variables()
            if (env.isNotEmpty()) {
                log.debug("injecting ${env.size} Hermit environment variables into $context")
                settings.withEnvironmentVariables(env)
            }
        }
    }
}
