package com.squareup.cash.hermit.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.squareup.cash.hermit.FakeHermit
import com.squareup.cash.hermit.Hermit
import com.squareup.cash.hermit.HermitProjectTestCase
import com.squareup.cash.hermit.TestPackage
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test

class HermitGradleEnvProviderTest : HermitProjectTestCase() {
  private val provider = HermitGradleEnvProvider()
  private val javaHome = System.getProperty("java.home")

  override fun tearDown() {
    // Remove any JDKs registered by Hermit to avoid SDK leak failures
    ApplicationManager.getApplication()?.runWriteAction {
      val jdkTable = ProjectJdkTable.getInstance()
      jdkTable.allJdks
        .filter { it.name.startsWith("Hermit") }
        .forEach { jdkTable.removeJdk(it) }
    }
    super.tearDown()
  }

  private fun taskId(): ExternalSystemTaskId {
    return ExternalSystemTaskId.create(
      GradleConstants.SYSTEM_ID,
      ExternalSystemTaskType.EXECUTE_TASK,
      project
    )
  }

  // -- configureTasks tests (task execution path) --

  @Test fun `test configureTasks injects hermit env vars into settings`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home",
        "HERMIT_BIN" to "/project/bin"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    provider.configureTasks("", taskId(), settings, null)

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
    assertEquals("/project/bin", settings.env["HERMIT_BIN"])
  }

  @Test fun `test configureTasks does not modify settings when hermit is not present`() {
    val settings = GradleExecutionSettings()
    provider.configureTasks("", taskId(), settings, null)

    assertTrue(settings.env.isEmpty())
  }

  @Test fun `test configureTasks does not modify settings when hermit has no env vars`() {
    withHermit(FakeHermit(listOf(
      TestPackage("gradle", "9.3.1", "", "/gradle/path", emptyMap())
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    provider.configureTasks("", taskId(), settings, null)

    assertTrue(settings.env.isEmpty())
  }

  @Test fun `test configureTasks preserves existing env vars in settings`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    settings.withEnvironmentVariables(mapOf("EXISTING_VAR" to "existing_value"))
    provider.configureTasks("", taskId(), settings, null)

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
    assertEquals("existing_value", settings.env["EXISTING_VAR"])
  }

  @Test fun `test configureTasks overrides existing JAVA_HOME with hermit value`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    settings.withEnvironmentVariables(mapOf("JAVA_HOME" to "/user/old/jdk"))
    provider.configureTasks("", taskId(), settings, null)

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
  }

  @Test fun `test configureTasks injects multiple env vars from multiple packages`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home"
      )),
      TestPackage("gradle", "9.3.1", "", "/gradle/path", mapOf(
        "GRADLE_HOME" to "/hermit/gradle/home"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    provider.configureTasks("", taskId(), settings, null)

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
    assertEquals("/hermit/gradle/home", settings.env["GRADLE_HOME"])
  }

  // -- injectHermitEnvironment tests (shared logic used by both task execution and sync) --

  @Test fun `test injectHermitEnvironment injects env vars`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home",
        "HERMIT_ENV" to "/project"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    HermitGradleEnvProvider.injectHermitEnvironment(project, settings, "test")

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
    assertEquals("/project", settings.env["HERMIT_ENV"])
  }

  @Test fun `test injectHermitEnvironment skips when hermit is not present`() {
    val settings = GradleExecutionSettings()
    HermitGradleEnvProvider.injectHermitEnvironment(project, settings, "test")

    assertTrue(settings.env.isEmpty())
  }

  @Test fun `test injectHermitEnvironment overrides existing JAVA_HOME`() {
    withHermit(FakeHermit(listOf(
      TestPackage("openjdk", "21", "", javaHome, mapOf(
        "JAVA_HOME" to "/hermit/jdk/home"
      ))
    )))
    Hermit(project).enable()
    waitAppThreads()

    val settings = GradleExecutionSettings()
    settings.withEnvironmentVariables(mapOf("JAVA_HOME" to "/user/old/jdk"))
    HermitGradleEnvProvider.injectHermitEnvironment(project, settings, "test")

    assertEquals("/hermit/jdk/home", settings.env["JAVA_HOME"])
  }
}
