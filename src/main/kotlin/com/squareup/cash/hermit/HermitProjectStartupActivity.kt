package com.squareup.cash.hermit

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class HermitProjectStartupActivity: ProjectActivity {
  override suspend fun execute(project: Project) {
    Hermit(project).open()

    // Ensure HermitCleanupService is initialized and disposal is registered
    project.getService(HermitCleanupService::class.java)
  }
}