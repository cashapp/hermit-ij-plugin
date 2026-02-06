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
    private const val NOTIFICATION_GROUP = "Hermit"

    fun showError(project: Project, error: String?) {
        val message = error ?: ""
        val notification = Notification(NOTIFICATION_GROUP, "Hermit Error", message, NotificationType.ERROR)
        Notifications.Bus.notify(notification, project)
    }

    fun showInfo(project: Project, title: String, content: String?) {
        val message = content ?: ""
        val notification = Notification(NOTIFICATION_GROUP, title, message, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification, project)
    }

    fun askToEnableHermit(project: Project) {
        val actions = ActionManager.getInstance()
        val notification = Notification(
            NOTIFICATION_GROUP,
            "Hermit",
            "Hermit environment detected. Enable Hermit support?",
            NotificationType.INFORMATION
        ).addAction(actions.getAction(ActionID.enableHermit))
         .addAction(actions.getAction(ActionID.dontEnableHermit))

        Notifications.Bus.notify(notification, project)
    }

    fun explainProjectIsNotTrusted(project: Project) {
        val message = "Untrusted Project: hermit disabled."
        val notification = Notification(NOTIFICATION_GROUP, "Hermit", message, NotificationType.ERROR)

        notification.addAction(object : NotificationAction("More Information") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                BrowserUtil.open("https://www.jetbrains.com/help/idea/project-security.html")
                notification.expire()
            }
        })

        Notifications.Bus.notify(notification, project)
    }
}
