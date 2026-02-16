package com.squareup.cash.hermit.goland

import com.goide.sdk.GoSdkService
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.HermitPackage
import com.squareup.cash.hermit.HermitPropertyHandler
import com.squareup.cash.hermit.PackageType
import com.squareup.cash.hermit.UI

@Service
class HermitGoEnvUpdater : HermitPropertyHandler {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun handle(hermitPackage: HermitPackage, project: Project) {
        if (hermitPackage.type == PackageType.Go) {
            try {
                val sdkService = GoSdkService.getInstance(project);
                val sdk = sdkService.getSdk(null);

                if (
                    sdk.homeUrl.isEmpty()
                    || hermitPackage.version != sdk.version
                    || hermitPackage.goURL() != sdk.homeUrl
                    // If the current sdk has the same version,but it was not available before Hermit downloaded it,
                    // set it again to force re-indexing, and marking it as a valid one
                    || !sdk.isValid
                ) {
                    log.debug("setting project (" + project.name + ") GoSDK to " + hermitPackage.logString())
                    UI.showInfo(project, "Hermit", "Switching to SDK ${hermitPackage.displayName()}")
                    hermitPackage.setSdk(project)
                }
            } catch (e: Exception) {
                log.warn("Failed to update Go SDK - Go plugin may not be properly loaded: ${e.message}")
                // This can happen when Go plugin dependencies are not met (e.g., missing Ultimate modules)
                // The plugin should continue to work for other features even if Go SDK management fails
            }
        }
    }
}