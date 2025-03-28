package com.squareup.cash.hermit.action

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.Hermit


class EnableHermitAction : NotificationAction("") {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        Hermit(e.project!!).enable()
        notification.expire()
    }
}

class BackgroundableWrapper(project: Project, title: String, private val task: Runnable)
    : Task.Backgroundable(project, title, true) {

    override fun run(indicator: ProgressIndicator) {
        task.run()
    }
}