package com.squareup.cash.hermit.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.Hermit
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.service.execution.BuildLayoutParameters
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionAware

class HermitGradleExecutionAware: GradleExecutionAware {
  private val log: Logger = Logger.getInstance(this.javaClass)

  override fun prepareExecution(
    task: ExternalSystemTask,
    externalProjectPath: String,
    isPreviewMode: Boolean,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  ) {
    // If hermit is installing packages, pause the Gradle initialisation thread until the installation is done.
    // This prevents Gradle failing for invalid JDK if the underlying JDK is not available on the disk when the project
    // is opened.
    if (Hermit(project).hermitStatus() == Hermit.HermitStatus.Installing) {
      log.debug("waiting for 'hermit install' to be finished before continuing with Gradle preparation")

      val start = System.currentTimeMillis()
      var waitMS = 10L
      while (true) {
        Thread.sleep(waitMS)
        if (Hermit(project).hermitStatus() != Hermit.HermitStatus.Installing) {
          break
        }
        if (waitMS < 500L) waitMS *= 2
        if (System.currentTimeMillis() - start > TIMEOUT_MS) {
          log.warn("timed out while waiting for 'hermit install' to finish")
          break
        }
      }
      log.debug("done waiting for 'hermit install'")
    }
  }

  @ApiStatus.OverrideOnly
  override fun getBuildLayoutParameters(project: Project, projectPath: java.nio.file.Path): BuildLayoutParameters? {
    return null
  }

  // Older API with String parameter - keep for compatibility
  override fun getBuildLayoutParameters(project: Project, projectPath: String): BuildLayoutParameters? = null

  override fun getDefaultBuildLayoutParameters(project: Project): BuildLayoutParameters? = null

  // Newer API with Path parameter
  @ApiStatus.OverrideOnly  
  override fun isGradleInstallationHomeDir(project: Project, homePath: java.nio.file.Path): Boolean {
    return false
  }

  // Older API with String parameter - keep for compatibility
  override fun isGradleInstallationHomeDir(project: Project, homePath: String): Boolean = false

  companion object {
    const val TIMEOUT_MS = 120000
  }
}