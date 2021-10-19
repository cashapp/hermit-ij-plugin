package com.squareup.cash.hermit.ui.statusbar

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
      if ( Hermit(project).hasHermit() && !Hermit(project).isHermitEnabled() ) {
        UI.askToEnableHermit(project)
      }
    }
  }

  override fun getText(): String {
    return if ( Hermit(project).isHermitEnabled() ) {
      "Hermit enabled"
    } else {
      "Hermit disabled"
    }
  }

  override fun getAlignment(): Float {
    return 0.0f
  }
}
