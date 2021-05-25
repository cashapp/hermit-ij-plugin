package com.squareup.cash.hermit

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class HermitVFSChangeListener : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        val needsUpdating = HashMap<String, Project>()
        events.forEach {
            val file = it.file
            val project = ProjectLocator.getInstance().guessProjectForFile(file)
            if (project != null && file != null && isBinChange(project, file)) {
                needsUpdating[project.name] = project
            }
        }

        needsUpdating.forEach {
            Hermit(it.value).open()
            Hermit(it.value).installAndUpdate()
        }
    }

    private fun isBinChange(project: Project, file: VirtualFile): Boolean {
        val root = project.guessProjectDir()
        if (root == null || (file.parent == null && file.name != "bin") || (file.parent.name != "bin" && file.name != "bin")) {
            return false
        }
        return (file.parent.parent != null && file.parent.parent == root) || (file.name == "bin" && file.parent == root)
    }
}