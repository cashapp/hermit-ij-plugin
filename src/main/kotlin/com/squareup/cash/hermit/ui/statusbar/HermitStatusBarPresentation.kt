package com.squareup.cash.hermit.ui.statusbar

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import com.squareup.cash.hermit.Hermit
import com.squareup.cash.hermit.UI
import java.awt.event.MouseEvent

class HermitStatusBarPresentation(val project: Project) : StatusBarWidget.TextPresentation {
  override fun getTooltipText(): String? {
    return "Hermit status"
  }

  override fun getClickConsumer(): Consumer<MouseEvent>? {
    return Consumer<MouseEvent> {
      if (Hermit(project).hasHermit()) {
        val status = Hermit(project).hermitStatus()
        if (status == Hermit.HermitStatus.Disabled) {
          UI.askToEnableHermit(project)
        } else if (status == Hermit.HermitStatus.Failed) {
          ApplicationManager.getApplication().invokeLater {
            Hermit(project).installAndUpdate()
          }
        }
      }
    }
  }

  override fun getText(): String {
    return when (Hermit(project).hermitStatus()) {
      Hermit.HermitStatus.Enabled -> "Hermit enabled"
      Hermit.HermitStatus.Disabled -> "Hermit disabled"
      Hermit.HermitStatus.Failed -> "Hermit failed"
      Hermit.HermitStatus.Installing -> "Hermit installing"
    }
  }

  override fun getAlignment(): Float {
    return 0.0f
  }
}
