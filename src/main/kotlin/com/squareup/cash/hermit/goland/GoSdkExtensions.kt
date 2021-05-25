package com.squareup.cash.hermit.goland

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.goide.sdk.GoSdkService
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.HermitPackage

fun HermitPackage.setSdk(project: Project): GoSdk {
    val new = GoSdkImpl("file://" + this.path, this.version, this.path + "/src/runtime/internal/sys/zversion.go")
    GoSdkService.getInstance(project).setSdk(new)
    return new
}

