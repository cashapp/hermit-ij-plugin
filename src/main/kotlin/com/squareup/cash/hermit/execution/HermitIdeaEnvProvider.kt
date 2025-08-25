package com.squareup.cash.hermit.execution

import com.intellij.openapi.diagnostic.Logger
import com.squareup.cash.hermit.Hermit

/**
 * Applied to vanilla java executions - Uses reflection for GoLand compatibility
 */
class HermitIdeaEnvProvider {
    private val log: Logger = Logger.getInstance(this.javaClass)

    // These methods are called by IntelliJ via reflection when the extension is registered
    
    fun isApplicableFor(configuration: Any): Boolean {
        return try {
            // Use reflection to get the project from RunConfigurationBase
            val getProject = configuration.javaClass.getMethod("getProject")
            val project = getProject.invoke(configuration) as? com.intellij.openapi.project.Project
            project?.let { Hermit(it).hasHermit() } ?: false
        } catch (e: Exception) {
            log.debug("Failed to check if configuration is applicable: ${e.message}")
            false
        }
    }

    fun updateJavaParameters(configuration: Any, params: Any, settings: Any?) {
        try {
            // Get project from configuration
            val getProject = configuration.javaClass.getMethod("getProject")
            val project = getProject.invoke(configuration) as? com.intellij.openapi.project.Project
            
            if (project != null) {
                // Get environment map from JavaParameters
                val getEnv = params.javaClass.getMethod("getEnv")
                @Suppress("UNCHECKED_CAST")
                val env = getEnv.invoke(params) as? MutableMap<String, String>
                
                if (env != null) {
                    // Update environment with Hermit variables
                    val hermitEnv = Hermit(project).environment().patch(env)
                    env.clear()
                    env.putAll(hermitEnv)
                }
            }
        } catch (e: Exception) {
            log.debug("Failed to update Java parameters: ${e.message}")
        }
    }
}