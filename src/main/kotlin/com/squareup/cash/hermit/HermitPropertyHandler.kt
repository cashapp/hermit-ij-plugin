package com.squareup.cash.hermit

import com.intellij.openapi.project.Project

interface HermitPropertyHandler {
    fun handle(hermitPackage: HermitPackage, project: Project)
}