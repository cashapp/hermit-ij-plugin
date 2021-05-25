package com.squareup.cash.hermit.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.tools.ToolsCustomizer
import com.squareup.cash.hermit.Hermit
import java.io.File

class HermitToolsCustomiser : ToolsCustomizer() {
    override fun customizeCommandLine(dataContext: DataContext, commandLine: GeneralCommandLine): GeneralCommandLine {
        // We need to clone the command line to avoid writing changes back to the original configuration
        val command = GeneralCommandLine()
            .withCharset(commandLine.charset)
            .withEnvironment(commandLine.environment)
            .withExePath(commandLine.exePath)
            .withInput(commandLine.inputFile)
            .withParameters(commandLine.parametersList.parameters)
            .withParentEnvironmentType(commandLine.parentEnvironmentType)
            .withRedirectErrorStream(commandLine.isRedirectErrorStream)
            .withWorkDirectory(commandLine.workDirectory)

        val project = dataContext.getData(PlatformDataKeys.PROJECT)
        if (project != null && Hermit(project).hasHermit()) {
            if (command.exePath.indexOf(File.separatorChar) == -1 && Hermit(project).isHermitManagedBin(command.exePath)) {
                // We need to prepend the hermit bin environment to the command to work around OSX fix at
                // GeneralCommandLine::validateAndPrepareCommandLine
                command.withExePath(Hermit(project).binDir() + File.separatorChar + command.exePath)
            }
            val newenv = Hermit(project).environment().patch(command.environment)
            return command.withEnvironment(newenv)
        }
        return command
    }
}