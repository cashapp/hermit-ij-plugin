package com.squareup.cash.hermit.execution

/**
 * Applied to vanilla java executions - TEMPORARILY DISABLED for GoLand compatibility
 * 
 * The IntelliJ IDEA specific classes (RunConfigurationExtension, JavaParameters) 
 * don't exist in GoLand, causing verification failures.
 * 
 * This functionality is disabled until we can implement platform-specific loading.
 */
class HermitIdeaEnvProvider {
    // Disabled implementation - would extend RunConfigurationExtension in IDEA-only environment
    // Original functionality: Updates Java runtime environment with Hermit environment variables
}