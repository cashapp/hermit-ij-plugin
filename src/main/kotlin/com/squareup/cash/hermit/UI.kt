package com.squareup.cash.hermit

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
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

    fun explainProjectIsNotTrusted(project: Project) {
        val message = "Untrusted Project: Running in safe mode."
        val notification = Notification("", "Hermit", message, NotificationType.ERROR)

        notification.addAction(object : NotificationAction("More Information") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                BrowserUtil.open("https://www.jetbrains.com/help/idea/project-security.html")
                notification.expire()
            }
        })

        Notifications.Bus.notify(notification, project)
    }
}