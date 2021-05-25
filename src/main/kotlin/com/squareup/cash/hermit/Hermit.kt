package com.squareup.cash.hermit

import com.google.common.collect.ImmutableMap
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.action.BackgroundableWrapper
import java.util.concurrent.ConcurrentHashMap
import com.intellij.openapi.extensions.ExtensionPointName


/**
 * HermitState maintains the information about the hermit environment of each active project
 */
object Hermit {
    private val HANDLER_EP_NAME: ExtensionPointName<HermitPropertyHandler> =
        ExtensionPointName.create<HermitPropertyHandler>("org.squareup.cash.hermit.idea-plugin.property-handler")

    private val projects = ConcurrentHashMap<String, State>()

    operator fun invoke(project: Project): State {
        val state = project.projectFilePath?.let { projects.getOrPut(it, { State(project) }) }
        // projectFilePath can be null for the default project. Return an empty state for it.
        return state ?: State(project)
    }

    fun remove(project: Project) {
        project.projectFilePath?.let { projects.remove(it) }
    }

    /**
     * State maintains the Hermit state of a single project
     */
    class State(private val project: Project) {
        // Is this project a hermit enabled project?
        private var isHermitProject = false
        // Has the user agreed to enable the Hermit environment
        private var isHermitEnabled = false
        // Has the project been opened in the plugin?
        private var isHermitOpened = false

        private var properties: HermitProperties? = null

        fun open() {
            val props = PropertiesComponent.getInstance(project)
            this.isHermitEnabled = props.getBoolean(PropertyID.HermitEnabled, false)
            this.isHermitProject = project.hasHermit()

            if (this.isHermitProject) {
                if (!this.isHermitOpened) {
                    this.isHermitOpened = true
                    if (this.isHermitEnabled) {
                        this.installAndUpdate()
                    } else {
                        UI.askToEnableHermit(project)
                    }
                }
            }
        }

        fun enable() {
            PropertiesComponent.getInstance(project).setValue(PropertyID.HermitEnabled, true)
            this.isHermitEnabled = true
            this.installAndUpdate()
        }

        fun installAndUpdate() {
            if (this.isHermitEnabled) {
                val task = BackgroundableWrapper(project, "Installing Hermit Packages", Runnable {
                    when (val result = project.installHermitPackages()) {
                        is Failure -> {
                            UI.showError(project, result.a)
                        }
                        is Success -> {
                            // We need to enable hermit in a Write enabled thread
                            ApplicationManager.getApplication().invokeLater {
                                Hermit(project).update()
                            }
                        }
                    }
                })
                ProgressManager.getInstance().run(task)
            }
        }

        private fun update() {
            this.clear()
            this.isHermitProject = project.hasHermit()

            if (this.isHermitProject && this.isHermitEnabled) {
                when(val prop =  project.hermitProperties()) {
                    is Failure -> {
                        this.isHermitProject = false
                        UI.showError(project, prop.a)
                    }
                    is Success -> {
                        this.properties = prop.b
                        prop.b.packages.forEach { updateHandlers(it) }
                    }
                }
            }
        }

        fun environment(): Env {
            return Env(this.properties?.env ?: emptyMap())
        }

        fun hasHermit(): Boolean {
            return this.isHermitProject
        }

        fun binDir(): String {
            return project.binDir()?.path ?: ""
        }

        fun isHermitManagedBin(exe: String): Boolean {
            val bin = project.binDir()
            return bin?.findChild(exe)?.exists() ?: false
        }

        private fun clear() {
            this.properties = null
            this.isHermitProject = false
        }

        private fun updateHandlers(hermitPackage: HermitPackage) {
            HANDLER_EP_NAME.extensions.forEach {
                it.handle(hermitPackage, project)
            }
        }
    }

    /**
     * Env encapsulates the environment variables of a Hermit project
     */
    class Env(private val env: Map<String, String>) {
        fun patch(old: Map<String, String>): Map<String, String> {
            val new = HashMap<String,String>()
            new.putAll(old)
            addTo(new)
            return new
        }

        fun addTo(to: MutableMap<String, String>?) {
            to?.putAll(env)
        }

        fun variables(): Map<String,String> {
            return ImmutableMap.copyOf(env)
        }
    }
}