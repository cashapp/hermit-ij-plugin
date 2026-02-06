package com.squareup.cash.hermit.gradle

import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

/**
 * Injects Hermit environment variables into Gradle sync/resolve operations.
 *
 * This complements [HermitGradleEnvProvider] which handles task executions.
 * Together they ensure that Hermit environment variables (including `JAVA_HOME`)
 * override the IDE's inherited environment in all Gradle operations.
 */
class HermitGradleProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun preImportCheck() {
        val project = resolverCtx.externalSystemTaskId.findProject() ?: return
        HermitGradleEnvProvider.injectHermitEnvironment(project, resolverCtx.settings, "Gradle sync")
    }
}
