package com.squareup.cash.hermit.gradle

import com.intellij.execution.target.value.TargetValue
import com.intellij.openapi.project.Project
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.service.GradleInstallationManager
import org.jetbrains.plugins.gradle.service.execution.BuildLayoutParameters
import org.jetbrains.plugins.gradle.service.execution.gradleUserHomeDir
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

private val GRADLE_JAR_PATTERN = Pattern.compile(System.getProperty("gradle.pattern.core.jar", "gradle-(core-)?(\\d.*)\\.jar"))

/**
 * [BuildLayoutParameters] that points to a hermit-managed Gradle installation.
 * The [gradleUserHomePath] resolution matches IntelliJ's [LocalBuildLayoutParameters]:
 * project Gradle settings first, then GRADLE_USER_HOME env/property, then ~/.gradle.
 */
class HermitBuildLayoutParameters(
    private val hermitGradleHome: Path,
    project: Project
) : BuildLayoutParameters {

    override val gradleHome: TargetValue<Path> = TargetValue.fixed(hermitGradleHome)

    override val gradleVersion: GradleVersion? by lazy {
        detectGradleVersion(hermitGradleHome)
    }

    override val gradleUserHomePath: TargetValue<Path> by lazy {
        val serviceDir = GradleSettings.getInstance(project).serviceDirectoryPath
        val userHome = if (serviceDir != null) {
            Path.of(serviceDir)
        } else {
            gradleUserHomeDir().toPath()
        }
        TargetValue.fixed(userHome)
    }
}

/**
 * Detects the Gradle version from a Gradle home directory by inspecting jar filenames in `lib/`.
 * Inlined from [GradleInstallationManager] to avoid binary compatibility issues with the Companion
 * object across older IntelliJ versions.
 */
private fun detectGradleVersion(gradleHome: Path): GradleVersion? {
    val libs = gradleHome.resolve("lib")
    if (!Files.isDirectory(libs)) return null

    val versionString = try {
        Files.list(libs).use { children ->
            children
                .map { GRADLE_JAR_PATTERN.matcher(it.fileName.toString()) }
                .filter { it.matches() }
                .map { it.group(2) }
                .findFirst()
                .orElse(null)
        }
    } catch (_: Exception) {
        null
    } ?: return null

    return try {
        GradleVersion.version(versionString)
    } catch (_: IllegalArgumentException) {
        // GradleVersion.version(gradleVersion) might throw exception for custom Gradle versions
        // https://youtrack.jetbrains.com/issue/IDEA-216892
        null
    }
}
