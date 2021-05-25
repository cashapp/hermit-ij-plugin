package com.squareup.cash.hermit.execution

import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.Hermit
import org.jetbrains.plugins.terminal.LocalTerminalCustomizer

class HermitShellCommandCustomiser : LocalTerminalCustomizer() {
    override fun customizeCommandAndEnvironment(
        project: Project,
        command: Array<out String>,
        envs: MutableMap<String, String>
    ): Array<String> {
        if (Hermit(project).hasHermit()) {
            Hermit(project).environment().addTo(envs)
            // IntelliJ terminal injects a custom .zshenv that prepends this to the path to force
            // GO SDK binary to be first in the path.
            envs.remove("_INTELLIJ_FORCE_PREPEND_PATH")
        }
        return super.customizeCommandAndEnvironment(project, command, envs)
    }
}