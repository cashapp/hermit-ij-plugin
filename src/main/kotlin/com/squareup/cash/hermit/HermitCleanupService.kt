package com.squareup.cash.hermit

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class HermitCleanupService(private val project: Project) : Disposable {
    override fun dispose() {
        Hermit.remove(project)
    }
}