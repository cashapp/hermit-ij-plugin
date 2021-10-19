package com.squareup.cash.hermit.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.squareup.cash.hermit.Hermit

class HermitStatusBarWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "Hermit Status"
  }

  override fun isAvailable(project: Project): Boolean {
    return Hermit(project).hasHermit()
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return HermitStatusBarWidget(project)
  }

  override fun disposeWidget(project: StatusBarWidget) {
  }

  override fun canBeEnabledOn(project: StatusBar): Boolean {
    return true
  }

  companion object {
    const val ID = "HermitStatusBarWidgetFactory"
  }
}
