package com.squareup.cash.hermit.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.squareup.cash.hermit.Hermit

class DontEnableHermitAction : NotificationAction("") {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        notification.expire()
    }
}