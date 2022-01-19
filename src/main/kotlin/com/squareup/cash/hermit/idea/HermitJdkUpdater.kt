package com.squareup.cash.hermit.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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
       }
    }
}