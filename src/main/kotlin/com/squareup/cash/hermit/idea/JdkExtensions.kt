package com.squareup.cash.hermit.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.ProjectRootManager
import com.squareup.cash.hermit.HermitPackage

fun Sdk.setForProject(project: Project) {
    ProjectRootManager.getInstance(project).projectSdk = this
}

fun HermitPackage.newSdk(): Sdk {
    val jdkType = JavaSdkImpl.getInstance()
    val sdk = JavaSdk.getInstance().createJdk(sdkName(), path)
    //jdkType.setupSdkPaths(sdk)
    return sdk
}

fun HermitPackage.findInstalledSdk(): Sdk? {
    return ProjectJdkTable.getInstance().findJdk(sdkName())
}