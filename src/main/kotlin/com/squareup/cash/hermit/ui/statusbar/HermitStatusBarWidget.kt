package com.squareup.cash.hermit.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget.WidgetPresentation
import com.intellij.openapi.wm.impl.status.EditorBasedWidget

class HermitStatusBarWidget(project: Project) : EditorBasedWidget(project) {
    override fun ID(): String {
      return ID
    }

    override fun getPresentation(): WidgetPresentation? {
      return HermitStatusBarPresentation(project)
    }

  companion object {
    const val ID = "HermitStatusBarWidget"
  }
}
