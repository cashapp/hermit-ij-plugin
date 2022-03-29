package com.squareup.cash.hermit.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.squareup.cash.hermit.HermitPackage

fun Sdk.setForProject(project: Project) {
    ProjectRootManager.getInstance(project).projectSdk = this
}

fun HermitPackage.getSdk(): Sdk {
    val installed = findInstalledSdk()
    if (installed != null) {
        return installed
    }
    val sdk = this.newSdk()
    ProjectJdkTable.getInstance().addJdk(sdk)
    return sdk
}

fun HermitPackage.newSdk(): Sdk {
    val type = JavaSdkImpl()
    val sdk = ProjectJdkImpl(sdkName(), type, path, version)
    type.setupSdkPaths(sdk)
    return sdk
}

fun HermitPackage.findInstalledSdk(): Sdk? {
    return ProjectJdkTable.getInstance().findJdk(sdkName())
}