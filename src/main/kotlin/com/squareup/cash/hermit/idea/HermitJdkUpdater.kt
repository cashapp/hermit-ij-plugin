package com.squareup.cash.hermit.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.util.Disposer
import com.squareup.cash.hermit.*

class HermitJdkUpdater : HermitPropertyHandler {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun handle(hermitPackage: HermitPackage, project: Project) {
       if (hermitPackage.type == PackageType.JDK) {
           val projectSdk = project.projectSdk()
           if (hermitPackage.sdkName() != projectSdk?.name) {
               log.debug("setting project (" + project.name + ") SDK to " + hermitPackage.logString())
               ApplicationManager.getApplication()?.runWriteAction {
                   val installed = hermitPackage.findInstalledSdk()
                   val sdk = if (installed != null) {
                       installed
                   } else {
                       val new = hermitPackage.newSdk()
                       ProjectJdkTable.getInstance().addJdk(new)
                       new
                   }
                   sdk.setForProject(project)
               }
               UI.showInfo(project, "Hermit", "Switching to SDK ${hermitPackage.displayName()}")
           }
           val currentSdk = project.projectSdk()
           // Previous versions of the plugin created SDKs with the version unset.
           // If so, update the SDK with one with version set correctly
           val unVersionedSDK = (currentSdk?.versionString ?: "") == ""
           if (currentSdk != null && (hermitPackage.path != currentSdk.homePath || unVersionedSDK)) {
               log.debug("updating (" + project.name + ") SDK (" + currentSdk.name + ") path from " + (currentSdk.homePath ?: "<null>") + " to " + hermitPackage.path)
               ApplicationManager.getApplication()?.runWriteAction {
                   val sdk = hermitPackage.newSdk()
                   ProjectJdkTable.getInstance().updateJdk(currentSdk, sdk)
               }
           }
       }
    }
}
