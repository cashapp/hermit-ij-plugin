package com.squareup.cash.hermit

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener


class HermitProjectManagerListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        Hermit(project).open()
    }

    override fun projectClosing(project: Project) {
        Hermit.remove(project)
    }
}