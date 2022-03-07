package com.squareup.cash.hermit

import com.intellij.testFramework.JavaProjectTestCase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

abstract class HermitProjectTestCase : JavaProjectTestCase() {
    protected var hermitScriptPath: Path? = null

    protected fun updateVFS() {
        synchronizeTempDirVfs(projectDirOrFile.parent)
        waitForAppLeakingThreads(1000, TimeUnit.MILLISECONDS)
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