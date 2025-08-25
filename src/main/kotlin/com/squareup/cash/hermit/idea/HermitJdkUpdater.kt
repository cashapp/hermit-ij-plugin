package com.squareup.cash.hermit.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.*

/**
 * JDK Updater for IntelliJ IDEA - Uses runtime class loading for GoLand compatibility
 */
class HermitJdkUpdater : HermitPropertyHandler {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun handle(hermitPackage: HermitPackage, project: Project) {
        if (hermitPackage.type == PackageType.JDK) {
            try {
                updateJdkSafely(hermitPackage, project)
            } catch (e: ClassNotFoundException) {
                log.debug("JDK management not available in this IDE (likely GoLand): ${e.message}")
            } catch (e: Exception) {
                log.warn("Failed to update JDK: ${e.message}")
            }
        }
    }

    private fun updateJdkSafely(hermitPackage: HermitPackage, project: Project) {
        // Use reflection to avoid direct references to IDEA-specific classes
        val projectSdk = getProjectSdk(project)
        val currentSdkName = getSdkName(projectSdk)
        if (hermitPackage.sdkName() != currentSdkName) {
            log.debug("setting project (${project.name}) SDK to ${hermitPackage.logString()}")
            ApplicationManager.getApplication()?.runWriteAction {
                val installed = findInstalledSdkSafely(hermitPackage) 
                val sdk = installed ?: createAndRegisterSdk(hermitPackage)
                setProjectSdk(project, sdk)
            }
        }
    }

    private fun getProjectSdk(project: Project): Any? {
        return try {
            val projectRootManager = Class.forName("com.intellij.openapi.roots.ProjectRootManager")
            val getInstance = projectRootManager.getMethod("getInstance", Project::class.java)
            val instance = getInstance.invoke(null, project)
            val getProjectSdk = projectRootManager.getMethod("getProjectSdk")
            getProjectSdk.invoke(instance)
        } catch (e: Exception) {
            null
        }
    }

    private fun getSdkName(sdk: Any?): String? {
        return try {
            if (sdk == null) null
            else {
                val getName = sdk.javaClass.getMethod("getName")
                getName.invoke(sdk) as? String
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findInstalledSdkSafely(hermitPackage: HermitPackage): Any? {
        return try {
            val projectJdkTable = Class.forName("com.intellij.openapi.projectRoots.ProjectJdkTable")
            val getInstance = projectJdkTable.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            val findJdk = projectJdkTable.getMethod("findJdk", String::class.java)
            findJdk.invoke(instance, hermitPackage.sdkName())
        } catch (e: Exception) {
            null
        }
    }

    private fun createAndRegisterSdk(hermitPackage: HermitPackage): Any {
        val javaSdk = Class.forName("com.intellij.openapi.projectRoots.JavaSdk")
        val getInstance = javaSdk.getMethod("getInstance")
        val sdkInstance = getInstance.invoke(null)
        val createJdk = javaSdk.getMethod("createJdk", String::class.java, String::class.java)
        val sdk = createJdk.invoke(sdkInstance, hermitPackage.sdkName(), hermitPackage.path)
        
        // Register the SDK
        val projectJdkTable = Class.forName("com.intellij.openapi.projectRoots.ProjectJdkTable")
        val getTableInstance = projectJdkTable.getMethod("getInstance")
        val tableInstance = getTableInstance.invoke(null)
        val addJdk = projectJdkTable.getMethod("addJdk", Class.forName("com.intellij.openapi.projectRoots.Sdk"))
        addJdk.invoke(tableInstance, sdk)
        
        return sdk
    }

    private fun setProjectSdk(project: Project, sdk: Any) {
        val projectRootManager = Class.forName("com.intellij.openapi.roots.ProjectRootManager")
        val getInstance = projectRootManager.getMethod("getInstance", Project::class.java)
        val instance = getInstance.invoke(null, project)
        val setProjectSdk = projectRootManager.getMethod("setProjectSdk", Class.forName("com.intellij.openapi.projectRoots.Sdk"))
        setProjectSdk.invoke(instance, sdk)
    }
}