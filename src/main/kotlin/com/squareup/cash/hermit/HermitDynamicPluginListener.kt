package com.squareup.cash.hermit

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.ProjectManager

class HermitDynamicPluginListener : DynamicPluginListener {
    // Called when the plugin is installed
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString == Plugin.ID) {
            val projects = ProjectManager.getInstance().openProjects
            projects.forEach { Hermit(it).open() }
        }
    }

    override fun pluginsLoaded() {
        // Empty implementation for compatibility
    }
    
    override fun beforePluginsLoaded() {
        // Empty implementation for compatibility  
    }
}