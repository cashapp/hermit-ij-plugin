package com.squareup.cash.hermit

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.testFramework.JavaProjectTestCase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

abstract class HermitProjectTestCase : JavaProjectTestCase() {
    protected var hermitScriptPath: Path? = null

    /**
     * Synchronizes the VFS with the on-disk state of the project temp directory.
     *
     * The native file watcher doesn't monitor test temp directories, so we manually mark the
     * tree dirty before refreshing. We also load all children after each refresh so that future
     * refreshes do full directory scans (vs partial scans that miss new files).
     */
    protected fun updateVFS() {
        val vDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectDirOrFile.parent)
        if (vDir != null) {
            (vDir as NewVirtualFile).markDirtyRecursively()
            vDir.refresh(false, true)
            ensureChildrenLoaded(vDir)
        }
        waitForAppLeakingThreads(1000, TimeUnit.MILLISECONDS)
    }

    private fun ensureChildrenLoaded(dir: VirtualFile) {
        for (child in dir.children) {
            if (child.isDirectory) {
                ensureChildrenLoaded(child)
            }
        }
    }

    protected fun withHermit(hermit: AbstractHermit) {
        val dir = projectDirOrFile.parent
        val bin = Files.createDirectories(dir.resolve("bin"))
        val permission = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"))
        hermitScriptPath = bin.resolve("hermit")

        if (Files.exists(hermitScriptPath!!)) {
            Files.delete(hermitScriptPath!!)
        }
        Files.createFile(hermitScriptPath!!, permission)
        hermit.writeTo(hermitScriptPath!!)

        updateVFS()
    }

    protected fun waitAppThreads() {
        waitForAppLeakingThreads(1000, TimeUnit.MILLISECONDS)
    }

    override fun setUpProject() {
        super.setUpProject()

        // Create the project root dir
        Files.createDirectories(projectDirOrFile.parent)
        updateVFS()
    }
}