package com.squareup.cash.hermit.gradle

import com.intellij.openapi.application.ApplicationManager
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
            if (settings == null) {
                ApplicationManager.getApplication()?.runWriteAction {

                    val settings = GradleProjectSettings()
                    settings.gradleHome = hermitPackage.path
                    settings.distributionType = DistributionType.LOCAL
                    GradleUtils.insertNewProjectSettings(project, settings)
                }
            } else if (!isUpToDate(settings, hermitPackage)) {
                ApplicationManager.getApplication()?.runWriteAction {
                    settings.gradleHome = hermitPackage.path
                    settings.distributionType = DistributionType.LOCAL
                }
                UI.showInfo(project, "Hermit", "Switching to Gradle ${hermitPackage.name}:${hermitPackage.version}")
            }
        }
    }

    private fun isUpToDate(settings: GradleProjectSettings, pkg: HermitPackage): Boolean {
        return settings.gradleHome == pkg.path && settings.distributionType == DistributionType.LOCAL
    }
}