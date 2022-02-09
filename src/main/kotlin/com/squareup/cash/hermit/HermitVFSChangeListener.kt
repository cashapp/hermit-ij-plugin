package com.squareup.cash.hermit

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class HermitVFSChangeListener : BulkFileListener {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun after(events: MutableList<out VFileEvent>) {
        val needsUpdating = HashMap<String, Project>()
        events.forEach {
            val file = it.file
            val project = ProjectLocator.getInstance().guessProjectForFile(file)
            if (project != null && file != null && isHermitChange(project, file)) {
                needsUpdating[project.name] = project
            }
        }

        needsUpdating.forEach {
            log.info("hermit configuration change detected at " + it.value.name)
            Hermit(it.value).open()
            Hermit(it.value).installAndUpdate()
        }
    }

    private fun isHermitChange(project: Project, file: VirtualFile): Boolean {
        val root = project.guessProjectDir()
        // Check if we are in a bin/ directory
        if (root == null || file.parent == null || file.parent.name != "bin") {
            return false
        }
        // Check if we are at root bin/ directory
        if (file.parent.parent != null && file.parent.parent != root) {
            return false
        }
        val isPackageChange = file.name.endsWith(".pkg") && file.name.startsWith(".")
        val isHermitScriptChange = file.name == "hermit" || file.name == "hermit.hcl"

        return isPackageChange || isHermitScriptChange
    }
}