package com.squareup.cash.hermit.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

object GradleUtils {
    // copied from com.android.tools.idea.gradle.util.GradleProjectSettingsFinder
    fun findGradleProjectSettings(project: Project): GradleProjectSettings? {
        val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID) as GradleSettings
        val state = settings.state!!
        val allProjectsSettings = state.linkedExternalProjectsSettings
        return getFirstNotNull(allProjectsSettings)
    }

    fun insertNewProjectSettings(project: Project, projectSettings: GradleProjectSettings) {
        val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID) as GradleSettings
        projectSettings.externalProjectPath = project.basePath
        settings.linkProject(projectSettings)
    }

    // copied from com.android.tools.idea.gradle.util.GradleProjectSettingsFinder
    private fun getFirstNotNull(allProjectSettings: Set<GradleProjectSettings?>?): GradleProjectSettings? {
        return allProjectSettings?.stream()?.filter { settings: GradleProjectSettings? -> settings != null }
            ?.findFirst()
            ?.orElse(null)
    }
}