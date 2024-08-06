package com.squareup.cash.hermit.execution

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.squareup.cash.hermit.Hermit

/**
 * Applied to vanilla java executions
 */
class HermitIdeaEnvProvider : RunConfigurationExtension() {
    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        return Hermit(configuration.project).hasHermit()
    }

    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any,
        params: JavaParameters,
        settings: RunnerSettings?
    ) {
        val project = configuration.project
        params.env = Hermit(project).environment().patch(params.env)
    }
}