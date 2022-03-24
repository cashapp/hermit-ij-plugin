package com.squareup.cash.hermit.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.squareup.cash.hermit.*

@Service
class HermitJdkUpdater : HermitPropertyHandler {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun handle(hermitPackage: HermitPackage, project: Project) {
       if (hermitPackage.type == PackageType.JDK) {
           val projectSdk = project.projectSdk()
           if (hermitPackage.sdkName() != projectSdk?.name) {
               log.debug("setting project (" + project.name + ") SDK to " + hermitPackage.logString())
               ApplicationManager.getApplication()?.runWriteAction {
                   hermitPackage.getSdk().setForProject(project)
               }
               UI.showInfo(project, "Hermit", "Switching to SDK ${hermitPackage.displayName()}")
           }
           if (projectSdk != null && hermitPackage.path != projectSdk.homePath) {
               log.debug("updating (" + project.name + ") SDK (" + projectSdk.name + ") path from " + (projectSdk.homePath ?: "<null>") + " to " + hermitPackage.path)
               ApplicationManager.getApplication()?.runWriteAction {
                   ProjectJdkTable.getInstance().updateJdk(projectSdk, hermitPackage.newSdk())
               }
           }
       }
    }
}