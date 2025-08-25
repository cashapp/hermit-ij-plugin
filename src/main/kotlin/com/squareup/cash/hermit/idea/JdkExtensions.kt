package com.squareup.cash.hermit.idea

/**
 * JDK Extensions for IntelliJ IDEA - TEMPORARILY DISABLED for GoLand compatibility
 * 
 * The IntelliJ IDEA specific classes (JavaSdk, ProjectJdkTable) 
 * don't exist in GoLand, causing verification failures.
 * 
 * This functionality is disabled until we can implement platform-specific loading.
 */

// All JDK extension functions are temporarily disabled for GoLand compatibility
// Original functionality: 
// - fun Sdk.setForProject(project: Project)
// - fun HermitPackage.newSdk(): Sdk  
// - fun HermitPackage.setSdk(project: Project): Sdk