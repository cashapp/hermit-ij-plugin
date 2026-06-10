package com.squareup.cash.hermit.gradle

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.HermitPackage
import com.squareup.cash.hermit.HermitPropertyHandler
import com.squareup.cash.hermit.PackageType
import com.squareup.cash.hermit.PropertyID
import com.squareup.cash.hermit.UI
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import java.io.File
import java.nio.file.Path

class GradleConfigUpdater : HermitPropertyHandler {
    private val log: Logger = Logger.getInstance(this.javaClass)

    private fun notifyGradleUpdate(project: Project, message: String) {
        UI.showInfo(project, "Hermit", message)
    }

    override fun handle(hermitPackage: HermitPackage, project: Project) {
        if (hermitPackage.type == PackageType.Gradle) {
            val settings = GradleUtils.findGradleProjectSettings(project)
            ApplicationManager.getApplication()?.runWriteAction {
                if (settings == null) {
                    log.debug("creating new project (" + project.name + ")  gradle config for " + hermitPackage.logString())
                    val newSettings = GradleProjectSettings()
                    newSettings.gradleHome = hermitPackage.path
                    newSettings.distributionType = DistributionType.LOCAL
                    GradleUtils.insertNewProjectSettings(project, newSettings)
                    rememberHermitGradleHome(project, hermitPackage.path)
                    notifyGradleUpdate(project, "Switching to Gradle ${hermitPackage.displayName()}")
                } else if (!isUpToDate(settings, hermitPackage)) {
                    log.debug("updating project (" + project.name + ")  gradle config to " + hermitPackage.logString())
                    settings.gradleHome = hermitPackage.path
                    settings.distributionType = DistributionType.LOCAL
                    rememberHermitGradleHome(project, hermitPackage.path)
                    notifyGradleUpdate(project, "Switching to Gradle ${hermitPackage.displayName()}")
                }
            }
        } else if (hermitPackage.type == PackageType.JDK) {
            // If the project uses a Hermit managed JDK, use the project JDK with Gradle
            // The project JDK is set accordingly in the HermitJdkUpdater
            val settings = GradleUtils.findGradleProjectSettings(project)
            ApplicationManager.getApplication()?.runWriteAction {
                if (settings == null) {
                    log.debug("creating project (" + project.name + ") gradle JDK config for " + hermitPackage.logString())
                    val newSettings = GradleProjectSettings()
                    newSettings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
                    GradleUtils.insertNewProjectSettings(project, newSettings)
                } else if (!isUpToDate(settings, hermitPackage)) {
                    log.debug("updating project (" + project.name + ") gradle JDK config to " + hermitPackage.logString())
                    settings.gradleJvm = ExternalSystemJdkUtil.USE_PROJECT_JDK
                }
            }
        }
    }

    /**
     * When hermit no longer manages a Gradle package, the local Gradle distribution the plugin
     * previously configured is left behind. The IDE then tries to build with that stale installation
     * instead of the project's Gradle wrapper, which fails when it points at a removed or wrong
     * version. Revert back to the Gradle wrapper, but only if the configured local distribution was
     * one the plugin set up from hermit, so we never touch a local install the user chose.
     */
    override fun reconcile(packages: List<HermitPackage>, project: Project) {
        if (packages.any { it.type == PackageType.Gradle }) return

        val settings = GradleUtils.findGradleProjectSettings(project) ?: return
        if (settings.distributionType != DistributionType.LOCAL) return

        val home = settings.gradleHome
        if (!isFromHermit(project, home)) return

        ApplicationManager.getApplication()?.runWriteAction {
            log.debug("reverting project (" + project.name + ") gradle config from stale hermit local home '" + home + "' to the wrapper")
            settings.gradleHome = null
            settings.distributionType = DistributionType.DEFAULT_WRAPPED
            forgetHermitGradleHome(project)
        }
        notifyGradleUpdate(project, "Reverting to the Gradle wrapper")
    }

    private fun isUpToDate(settings: GradleProjectSettings, pkg: HermitPackage): Boolean {
        return settings.gradleHome == pkg.path && settings.distributionType == DistributionType.LOCAL
    }

    /**
     * Whether the configured local Gradle home came from hermit. True if it matches the home the
     * plugin recorded when it last configured Gradle (newer configs), or if it lives under a hermit
     * state directory (older configs the plugin set up before it recorded the path).
     *
     * Hermit extracts packages to `<hermit-state>/pkg/<selector>`, where `<hermit-state>` is
     * `$HERMIT_STATE_DIR` or the OS user cache dir + `/hermit`. See cashapp/hermit env.go.
     */
    private fun isFromHermit(project: Project, home: String?): Boolean {
        if (home.isNullOrBlank()) return false
        val recorded = PropertiesComponent.getInstance(project).getValue(PropertyID.HermitGradleHome)
        if (home == recorded) return true

        val homePath = try {
            File(home).toPath().normalize()
        } catch (_: Exception) {
            return false
        }
        return hermitStateDirs().any { stateDir ->
            try {
                homePath.startsWith(stateDir.normalize())
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun hermitStateDirs(): List<Path> {
        val dirs = mutableListOf<Path>()
        System.getenv("HERMIT_STATE_DIR")?.takeIf { it.isNotBlank() }?.let { dirs.add(Path.of(it)) }
        val userHome = System.getProperty("user.home")
        if (!userHome.isNullOrBlank()) {
            // Linux / XDG default user cache dir
            val xdgCache = System.getenv("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() } ?: "$userHome/.cache"
            dirs.add(Path.of(xdgCache, "hermit"))
            // macOS default user cache dir
            dirs.add(Path.of(userHome, "Library", "Caches", "hermit"))
        }
        return dirs
    }

    private fun rememberHermitGradleHome(project: Project, home: String) {
        PropertiesComponent.getInstance(project).setValue(PropertyID.HermitGradleHome, home)
    }

    private fun forgetHermitGradleHome(project: Project) {
        PropertiesComponent.getInstance(project).unsetValue(PropertyID.HermitGradleHome)
    }
}