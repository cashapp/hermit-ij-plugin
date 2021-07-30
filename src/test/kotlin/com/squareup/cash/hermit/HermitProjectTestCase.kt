package com.squareup.cash.hermit

import com.intellij.testFramework.HeavyPlatformTestCase
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit

abstract class HermitProjectTestCase : HeavyPlatformTestCase() {
    protected var hermitScriptPath: Path? = null

    protected fun updateVFS() {
        synchronizeTempDirVfs(projectDirOrFile.parent)
        waitForAppLeakingThreads(1000, TimeUnit.MILLISECONDS)
    }

    protected fun withHermit(hermit: FakeHermit) {
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
}
