package com.squareup.cash.hermit.gradle

import com.intellij.execution.target.value.TargetValue
import com.squareup.cash.hermit.FakeHermit
import com.squareup.cash.hermit.Hermit
import com.squareup.cash.hermit.HermitProjectTestCase
import com.squareup.cash.hermit.PackageType
import com.squareup.cash.hermit.TestPackage
import junit.framework.TestCase
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class HermitGradleExecutionAwareTest : HermitProjectTestCase() {
  private val aware = HermitGradleExecutionAware()

  private fun gradleRoot(): Path {
    val dir = projectDirOrFile.parent.resolve("fake-gradle-home")
    Files.createDirectories(dir)
    return dir
  }

  private fun <T> localValue(targetValue: TargetValue<T>): T? {
    return targetValue.localValue.blockingGet(0)
  }

  @Test fun `test getBuildLayoutParameters returns null when hermit has no gradle package`() {
    withHermit(FakeHermit(listOf(TestPackage("openjdk", "21", "", "/nonexistent/jdk/path", emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getBuildLayoutParameters(project, projectDirOrFile.parent)
    assertNull(params)
  }

  @Test fun `test getBuildLayoutParameters returns null when hermit is not enabled`() {
    val params = aware.getBuildLayoutParameters(project, projectDirOrFile.parent)
    assertNull(params)
  }

  @Test
  fun `test getBuildLayoutParameters returns hermit gradle home when gradle package exists`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getBuildLayoutParameters(project, projectDirOrFile.parent)!!
    assertEquals(root, localValue(params.gradleHome!!))
  }

  @Test fun `test getDefaultBuildLayoutParameters returns null when no gradle package`() {
    withHermit(FakeHermit(emptyList()))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getDefaultBuildLayoutParameters(project)
    assertNull(params)
  }

  @Test
  fun `test getDefaultBuildLayoutParameters returns hermit gradle home when gradle package exists`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getDefaultBuildLayoutParameters(project)!!
    assertEquals(root, localValue(params.gradleHome!!))
  }

  @Test fun `test isGradleInstallationHomeDir returns true for hermit gradle home`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    assertTrue(aware.isGradleInstallationHomeDir(project, root))
  }

  @Test fun `test isGradleInstallationHomeDir returns false for non-hermit path`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    assertFalse(aware.isGradleInstallationHomeDir(project, Path.of("/some/other/path")))
  }

  @Test fun `test isGradleInstallationHomeDir returns false when hermit has no gradle`() {
    withHermit(FakeHermit(emptyList()))
    Hermit(project).enable()
    waitAppThreads()

    assertFalse(aware.isGradleInstallationHomeDir(project, Path.of("/some/path")))
  }

  @Test fun `test gradlePackage returns the gradle package after enable`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val pkg = Hermit(project).findPackage(PackageType.Gradle)!!
    TestCase.assertEquals("gradle", pkg.name)
    TestCase.assertEquals("9.3.1", pkg.version)
    TestCase.assertEquals(root.toString(), pkg.path)
  }

  @Test fun `test gradlePackage returns null when no gradle package`() {
    withHermit(FakeHermit(listOf(TestPackage("openjdk", "21", "", "/nonexistent/jdk/path", emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    assertNull(Hermit(project).findPackage(PackageType.Gradle))
  }

  @Test fun `test build layout parameters provides gradle user home`() {
    val root = gradleRoot()
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", root.toString(), emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getBuildLayoutParameters(project, projectDirOrFile.parent)!!
    val userHome = localValue(params.gradleUserHomePath)
    assertNotNull(userHome)
  }

  @Test fun `test getBuildLayoutParameters returns null when gradle path does not exist`() {
    val nonExistent = "/tmp/hermit-test-nonexistent-gradle-home-${System.nanoTime()}"
    withHermit(FakeHermit(listOf(TestPackage("gradle", "9.3.1", "", nonExistent, emptyMap()))))
    Hermit(project).enable()
    waitAppThreads()

    val params = aware.getBuildLayoutParameters(project, projectDirOrFile.parent)
    assertNull(params)
  }
}
