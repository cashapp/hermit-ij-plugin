package com.squareup.cash.hermit

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project

object UI {
    fun showError(project: Project, error: String?) {
        val message = error ?: ""
        val notification = Notification("", "Hermit Error", message, NotificationType.ERROR)
        Notifications.Bus.notify(notification, project)
    }

    fun showInfo(project: Project, title: String, content: String?) {
        val message = content ?: ""
        val notification = Notification("", title, message, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification, project)
    }

    fun askToEnableHermit(project: Project) {
        val actions = ActionManager.getInstance()
        val notification = Notification(
            "",
            "Hermit",
            "Hermit environment detected. Enable Hermit support?",
            NotificationType.INFORMATION
        ).addAction(actions.getAction(ActionID.enableHermit))
         .addAction(actions.getAction(ActionID.dontEnableHermit))

        Notifications.Bus.notify(notification, project)
    }
}