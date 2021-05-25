package com.squareup.cash.hermit

import com.goide.sdk.GoSdkService
import com.google.common.collect.ImmutableMap
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.squareup.cash.hermit.gradle.GradleUtils
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
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "root", mapOf(Pair("FOO", "BAR"))))))
        Hermit(project).enable()
        TestCase.assertEquals(ImmutableMap.of("FOO", "BAR"), Hermit(project).environment().variables())
    }

    fun `test it reloads the configuration when it changes`() {
        withHermit(FakeHermit(emptyList()))
        Hermit(project).enable()
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "root", mapOf(Pair("FOO", "BARBAR"))))))

        TestCase.assertEquals(ImmutableMap.of("FOO", "BARBAR"), Hermit(project).environment().variables())
    }

    fun `test it works if hermit is initialised after opening`() {
        Hermit(project).open()
        withHermit(FakeHermit(listOf(TestPackage("name", "version", "root", mapOf(Pair("FOO", "BAR"))))))
        Hermit(project).enable()
        TestCase.assertEquals(ImmutableMap.of("FOO", "BAR"), Hermit(project).environment().variables())
    }

    fun `test it sets the JDK home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("openjdk", "1.0", "/root", emptyMap()))))
        Hermit(project).enable()
        val sdk = ProjectRootManager.getInstance(project).projectSdk!!

        TestCase.assertEquals("/root", sdk.homePath)

        ApplicationManager.getApplication()?.runWriteAction { ProjectJdkTable.getInstance().removeJdk(sdk) }
    }

    fun `test it sets the GoSDK home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("go", "1.0", "/root", emptyMap()))))
        Hermit(project).enable()

        TestCase.assertEquals("file:///root", GoSdkService.getInstance(project).getSdk(null).homeUrl)
    }

    fun `test it sets the Gradle home correctly`() {
        withHermit(FakeHermit(listOf(TestPackage("gradle", "1.0", "/root", emptyMap()))))
        Hermit(project).enable()

        TestCase.assertEquals("/root", GradleUtils.findGradleProjectSettings(project)?.gradleHome)
    }
}