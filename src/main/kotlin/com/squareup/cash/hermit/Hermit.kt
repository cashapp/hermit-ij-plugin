package com.squareup.cash.hermit

import com.google.common.collect.ImmutableMap
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.squareup.cash.hermit.action.BackgroundableWrapper
import com.squareup.cash.hermit.ui.statusbar.HermitStatusBarWidget
import com.squareup.cash.hermit.ui.statusbar.HermitStatusBarWidgetFactory
import java.util.concurrent.ConcurrentHashMap


/**
 * HermitState maintains the information about the hermit environment of each active project
 */
object Hermit {
    private val log: Logger = Logger.getInstance(this.javaClass)

    enum class HermitStatus {
        Disabled,
        Enabled,
        Failed
    }

    private val HANDLER_EP_NAME: ExtensionPointName<HermitPropertyHandler> =
        ExtensionPointName.create<HermitPropertyHandler>("org.squareup.cash.hermit.idea-plugin.property-handler")

    private val projects = ConcurrentHashMap<String, State>()

    operator fun invoke(project: Project): State {
        val state = project.projectFilePath?.let { projects.getOrPut(it, { State(project) }) }
        // projectFilePath can be null for the default project. Return an empty state for it.
        return state ?: State(project)
    }

    fun remove(project: Project) {
        log.debug(project.name + ": closing project")
        project.projectFilePath?.let { projects.remove(it) }
    }

    /**
     * State maintains the Hermit state of a single project
     */
    class State(private val project: Project) {
        // Is this project a hermit enabled project?
        private var isHermitProject = false
        // Has the project been opened in the plugin?
        private var isHermitOpened = false
        // Current status of the Hermit integration
        private var status = HermitStatus.Disabled

        private var properties: HermitProperties? = null

        fun open() {
            val props = PropertiesComponent.getInstance(project)
            val hermitEnabled = props.getBoolean(PropertyID.HermitEnabled, false)
            this.isHermitProject = project.hasHermit()

            if (this.isHermitProject && !this.isHermitOpened) {
                log.debug("opening project " + project.name)
                this.isHermitOpened = true
                if (hermitEnabled) {
                    log.debug(project.name + ": hermit enabled in the project")
                    this.runInstall()
                } else {
                    log.debug(project.name + ": hermit disabled in the project")
                    setStatus(HermitStatus.Disabled)
                    UI.askToEnableHermit(project)
                }
            } else if (!this.isHermitProject) {
                log.debug(project.name + ": no hermit detected for " + project.name)
                setStatus(HermitStatus.Disabled)
            }
            this.refreshUI()
        }

        fun enable() {
            PropertiesComponent.getInstance(project).setValue(PropertyID.HermitEnabled, true)
            this.runInstall()
        }

        fun installAndUpdate() {
            if (this.status != HermitStatus.Disabled) {
                runInstall()
            }
        }

        private fun runInstall() {
            log.debug("installing hermit packages")
            val task = BackgroundableWrapper(project, "Installing Hermit Packages", Runnable {
                when (val result = project.installHermitPackages()) {
                    is Failure -> {
                        log.warn(project.name + ": installing hermit packages failed: " + result.a)
                        UI.showError(project, result.a)
                        setStatus(HermitStatus.Failed)
                    }
                    is Success -> {
                        log.info(project.name + ": installing hermit packages succeeded")
                        // We need to enable hermit in a Write enabled thread
                        ApplicationManager.getApplication().invokeLater {
                            setStatus(HermitStatus.Enabled)
                            Hermit(project).update()
                        }
                    }
                }
            })
            ProgressManager.getInstance().run(task)
        }

        private fun setStatus(newStatus: HermitStatus) {
            this.status = newStatus
            this.refreshUI()
        }

        private fun refreshUI() {
            val statusBarWidgetsManager = project.getService(StatusBarWidgetsManager::class.java)
            ApplicationManager.getApplication().invokeLater {
                statusBarWidgetsManager.updateWidget(HermitStatusBarWidgetFactory::class.java)

                WindowManager.getInstance().getStatusBar(project)?.updateWidget(HermitStatusBarWidget.ID)
            }
        }

        private fun update() {
            this.clear()
            this.isHermitProject = project.hasHermit()

            if (this.isHermitProject && this.status == HermitStatus.Enabled) {
                log.debug(project.name + ": updating hermit status from disk")
                when(val prop =  project.hermitProperties()) {
                    is Failure -> {
                        log.warn(project.name + ": updating hermit status failed: " + prop.a)
                        this.isHermitProject = false
                        UI.showError(project, prop.a)
                        setStatus(HermitStatus.Failed)
                    }
                    is Success -> {
                        log.info(project.name + ": updating hermit status succeeded: " + prop.b.logString())
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

        fun hermitStatus(): HermitStatus {
            return this.status
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