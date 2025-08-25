package com.squareup.cash.hermit.idea

/**
 * JDK Updater for IntelliJ IDEA - TEMPORARILY DISABLED for GoLand compatibility
 * 
 * The IntelliJ IDEA specific classes (ProjectJdkTable, ProjectJdkImpl) 
 * don't exist in GoLand, causing verification failures.
 * 
 * This functionality is disabled until we can implement platform-specific loading.
 */

// Disabled implementation - would implement HermitPropertyHandler in IDEA-only environment  
// Original functionality: Updates project JDK when Hermit JDK packages are detected
class HermitJdkUpdater {
    // Placeholder class to prevent compilation errors
}