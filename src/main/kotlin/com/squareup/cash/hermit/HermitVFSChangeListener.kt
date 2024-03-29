package com.squareup.cash.hermit

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class HermitVFSChangeListener(val project: Project) : BulkFileListener {
    private val log: Logger = Logger.getInstance(this.javaClass)

    override fun after(events: MutableList<out VFileEvent>) {
        var needsUpdating = false
        events.forEach {
            it.file?.let { file ->
                log.debug("Checking if file [${file.path}] is in projects " +
                    "${Hermit.allProjects().map { state -> state.project.name }}")
                // The project might have been disposed since looking up Hermit.allProjects.
                log.debug("Project [${project.name}] is disposed: [${project.isDisposed}]")
                if (!project.isDisposed && isHermitChange(project, file)) {
                    log.debug("Project [${project.name}] needs updating")
                    needsUpdating = true
                }
            }
        }

        if (needsUpdating) {
            log.info("hermit configuration change detected at " + project.name)
            Hermit(project).open()
            Hermit(project).installAndUpdate()
        }
    }

    private fun isHermitChange(project: Project, file: VirtualFile): Boolean {
        val root = project.guessProjectDir()

        // Check if we are in a bin/ directory
        if (root == null || file.parent == null || (file.parent.name != "bin" && file.name != "bin")) {
            return false
        }
        // Check if we are at root bin/ directory
        if (file.parent.parent != null && file.parent.parent != root && file.parent != root) {
            return false
        }
        val isPackageChange = file.name.endsWith(".pkg") && file.name.startsWith(".")
        val isHermitScriptChange = file.name == "hermit" || file.name == "hermit.hcl"
        val isHermitBinCreation = file.name == "bin"

        return isPackageChange || isHermitScriptChange || isHermitBinCreation
    }
}