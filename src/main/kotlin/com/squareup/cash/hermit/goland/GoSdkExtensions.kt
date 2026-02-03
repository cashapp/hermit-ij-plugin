package com.squareup.cash.hermit.goland

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.HermitPackage

fun HermitPackage.setSdk(project: Project): GoSdk {
    val new = GoSdkImpl(this.goURL(), this.version, null)
    GoSdkService.getInstance(project).setSdk(new)
    return new
}

fun HermitPackage.goURL(): String {
    return "file://" + this.path
}