package com.squareup.cash.hermit.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.HermitPackage
import com.squareup.cash.hermit.HermitPropertyHandler
import com.squareup.cash.hermit.PackageType
import com.squareup.cash.hermit.UI
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

class GradleConfigUpdater : HermitPropertyHandler {
    override fun handle(hermitPackage: HermitPackage, project: Project) {
        if (hermitPackage.type == PackageType.Gradle) {
            val settings = GradleUtils.findGradleProjectSettings(project)
            ApplicationManager.getApplication()?.runWriteAction {
                if (settings == null) {
                    val newSettings = GradleProjectSettings()
                    newSettings.gradleHome = hermitPackage.path
                    newSettings.distributionType = DistributionType.LOCAL
                    GradleUtils.insertNewProjectSettings(project, newSettings)
                } else if (!isUpToDate(settings, hermitPackage)) {
                    settings.gradleHome = hermitPackage.path
                    settings.distributionType = DistributionType.LOCAL
                }
            }
            UI.showInfo(project, "Hermit", "Switching to Gradle ${hermitPackage.displayName()}")
        } else if (hermitPackage.type == PackageType.JDK) {
            // If the project uses a Hermit managed JDK, use the project JDK with Gradle
            // The project JDK is set accordingly in the HermitJdkUpdater
            val settings = GradleUtils.findGradleProjectSettings(project)
            ApplicationManager.getApplication()?.runWriteAction {
                if (settings == null) {
                    val newSettings = GradleProjectSettings()
                    newSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
                    GradleUtils.insertNewProjectSettings(project, newSettings)
                } else if (!isUpToDate(settings, hermitPackage)) {
                    settings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
                }
            }
        }
    }

    private fun isUpToDate(settings: GradleProjectSettings, pkg: HermitPackage): Boolean {
        return settings.gradleHome == pkg.path && settings.distributionType == DistributionType.LOCAL
    }
}