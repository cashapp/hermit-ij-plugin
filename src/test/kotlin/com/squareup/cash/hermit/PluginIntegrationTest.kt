package com.squareup.cash.hermit

import com.goide.sdk.GoSdkService
import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.ClosingFuture
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.castSafelyTo
import com.squareup.cash.hermit.gradle.GradleUtils
import com.squareup.cash.hermit.ui.statusbar.HermitStatusBarPresentation
import com.squareup.cash.hermit.ui.statusbar.HermitStatusBarWidget
import junit.framework.TestCase

class PluginIntegrationTest : HermitProjectTestCase() {
    fun `test it negatively detects hermit correctly`() {
        TestCase.assertFalse(project.hasHermit())
    }

    fun `test it positively detects hermit correctly`() {
        withHermit(FakeHermit(emptyList()))
        Hermit(project).enable()
        TestCase.assertTrue(project.hasHermit())
    }

    fun `test it reads the env variables correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "", "root", mapOf(Pair("FOO", "BAR"))))))
        Hermit(project).enable()
        waitAppThreads()

        TestCase.assertEquals(ImmutableMap.of("FOO", "BAR"), Hermit(project).environment().variables())
    }

    fun `test it reloads the configuration when it changes`() {
        withHermit(FakeHermit(emptyList()))
        Hermit(project).enable()
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "", "root", mapOf(Pair("FOO", "BARBAR"))))))
        waitAppThreads()

        TestCase.assertEquals(ImmutableMap.of("FOO", "BARBAR"), Hermit(project).environment().variables())
    }

    fun `test it works if hermit is initialised after opening`() {
        Hermit(project).open()
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "", "root", mapOf(Pair("FOO", "BAR"))))))
        Hermit(project).enable()
        waitAppThreads()

        TestCase.assertEquals(ImmutableMap.of("FOO", "BAR"), Hermit(project).environment().variables())
    }

    fun `test it sets the JDK home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("openjdk", "1.0", "", "/root", emptyMap()))))
        Hermit(project).enable()
        waitAppThreads()

        val sdk = ProjectRootManager.getInstance(project).projectSdk!!
        TestCase.assertEquals("/root", sdk.homePath)

        ApplicationManager.getApplication()?.runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) }
    }

    fun `test it sets the GoSDK home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("go", "1.0", "","/root", emptyMap()))))
        Hermit(project).enable()
        waitAppThreads()

        TestCase.assertEquals("file:///root", GoSdkService.getInstance(project).getSdk(null).homeUrl)
    }

    fun `test it sets the Gradle home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("gradle", "1.0", "","/root", emptyMap()))))
        Hermit(project).enable()
        waitAppThreads()

        TestCase.assertEquals("/root", GradleUtils.findGradleProjectSettings(project)?.gradleHome)
    }

    fun `test it sets the Gradle JDK home correctly, if both JDK and Gradle are present`() {
        withHermit(FakeHermit(listOf(
            TestPackage("gradle", "1.0", "","/root", emptyMap()),
            TestPackage("openjdk", "1.0", "","/root", emptyMap())
        )))
        Hermit(project).enable()
        waitAppThreads()

        TestCase.assertEquals("/root", GradleUtils.findGradleProjectSettings(project)?.gradleHome)
        TestCase.assertEquals(ExternalSystemJdkUtil.USE_PROJECT_JDK, GradleUtils.findGradleProjectSettings(project)?.gradleJvm)

        val sdk = ProjectRootManager.getInstance(project).projectSdk!!
        ApplicationManager.getApplication()?.runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) }
    }

    fun `test it formats channel based JDK names correctly`() {
        withHermit(FakeHermit(listOf(
            TestPackage("openjdk", "", "test","/root", emptyMap())
        )))
        Hermit(project).enable()
        waitAppThreads()

        val sdk = ProjectRootManager.getInstance(project).projectSdk!!
        TestCase.assertEquals("Hermit (openjdk@test)", sdk.name)

        ApplicationManager.getApplication()?.runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) }
    }

    fun `test it formats version based JDK names correctly`() {
        withHermit(FakeHermit(listOf(
            TestPackage("openjdk", "1.0", "","/root", emptyMap())
        )))
        Hermit(project).enable()
        waitAppThreads()

        val sdk = ProjectRootManager.getInstance(project).projectSdk!!
        TestCase.assertEquals("Hermit (openjdk-1.0)", sdk.name)

        ApplicationManager.getApplication()?.runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) }
    }

    fun `test it shows the Hermit status as enabled if Hermit is enabled for the project`() {
        withHermit(FakeHermit(emptyList()))
        Hermit(project).open()
        Hermit(project).enable()
        waitAppThreads()

        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(HermitStatusBarWidget.ID)!!
        val presentation = widget.presentation as HermitStatusBarPresentation
        TestCase.assertEquals(presentation.text, "Hermit enabled")
    }

    fun `test it shows the Hermit status as disabled if Hermit is not enabled for the project`() {
        withHermit(FakeHermit(emptyList()))
        Hermit(project).open()
        waitAppThreads()

        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(HermitStatusBarWidget.ID)!!
        val presentation = widget.presentation as HermitStatusBarPresentation
        TestCase.assertEquals(presentation.text, "Hermit disabled")
    }

    fun `test it does not shows the Hermit status if there is no Hermit in the project`() {
        Hermit(project).open()
        waitAppThreads()

        val widget = WindowManager.getInstance().getStatusBar(project)?.getWidget(HermitStatusBarWidget.ID)
        TestCase.assertNull(widget)
    }
}