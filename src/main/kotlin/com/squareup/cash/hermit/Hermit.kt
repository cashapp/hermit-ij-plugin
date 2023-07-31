package com.squareup.cash.hermit

import arrow.core.flatMap
import com.google.common.collect.ImmutableMap
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.ThreeState
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
        Disabled, // Hermit is not in use
        Installing, // Installing Hermit packages to the system
        Enabled, // Hermit has been started correctly
        Failed // Hermit initialisation has failed
    }

    private val HANDLER_EP_NAME: ExtensionPointName<HermitPropertyHandler> =
        ExtensionPointName("org.squareup.cash.hermit.idea-plugin.property-handler")

    private val projects = ConcurrentHashMap<String, HermitState>()

    operator fun invoke(project: Project): HermitState {
        val hermitState = project.projectFilePath?.let { projects.getOrPut(it) { HermitState(project) } }
        // projectFilePath can be null for the default project. Return an empty state for it.
        return hermitState ?: HermitState(project)
    }

    fun remove(project: Project) {
        log.debug(project.name + ": closing project")
        project.projectFilePath?.let { projects.remove(it) }
    }

    fun allProjects(): Collection<HermitState> = projects.values

    /**
     * State maintains the Hermit state of a single project
     */
    class HermitState(val project: Project) {
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

            if (this.isHermitOpened) {
                this.refreshUI()
                return
            }

            log.info("opening project " + project.name)
            if (this.isHermitProject) {
                log.info("enabling Hermit for " + project.name)
                this.isHermitOpened = true
                if (hermitEnabled) {
                    log.debug(project.name + ": hermit enabled in the project")
                    this.runInstall()
                } else {
                    log.debug(project.name + ": hermit disabled in the project")
                    setStatus(HermitStatus.Disabled)
                    when (project.isTrustedForHermit()) {
                        ThreeState.YES -> { 
                            log.debug(project.name + ": project trusted, enabling")
                            this.enable()
                         }
                        ThreeState.NO -> { 
                            log.debug(project.name + ": project not trusted, skipping")
                        }
                        ThreeState.UNSURE -> { 
                            log.debug(project.name + ": cannot verify if project can be trusted, asking user instead")
                            UI.askToEnableHermit(project) 
                        }
                    }
                }
            } else {
                log.info(project.name + ": no hermit detected for " + project.name)
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
            log.info("installing hermit packages")
            setStatus(HermitStatus.Installing)
            val task = BackgroundableWrapper(project, "Installing Hermit Packages") {
                when (val result = project.installHermitPackages()) {
                    is Failure -> {
                        log.warn(project.name + ": installing hermit packages failed: " + result.a)
                        UI.showError(project, result.a)
                        setStatus(HermitStatus.Failed)
                    }
                    is Success -> {
                        log.info(project.name + ": installing hermit packages succeeded")
                        // Installing may finish after the project has been closed.
                        if (!project.isDisposed) {
                            // We need to enable hermit in a Write-enabled thread
                            ApplicationManager.getApplication().invokeLater {
                                setStatus(HermitStatus.Enabled)
                                Hermit(project).update()
                            }
                        }
                    }
                }
            }
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
                log.info(project.name + ": updating hermit status from disk")
                when(val res =  project.hermitProperties().flatMap { prop -> project.hermitVersion().map { Pair(it, prop) }}) {
                    is Failure -> {
                        log.warn(project.name + ": updating hermit status failed: " + res.a)
                        this.isHermitProject = false
                        UI.showError(project, res.a)
                        setStatus(HermitStatus.Failed)
                    }
                    is Success -> {
                        log.info(project.name + ": Hermit version: " + res.b.first)
                        log.info(project.name + ": updating hermit status succeeded: " + res.b.second.logString())
                        this.properties = res.b.second
                        res.b.second.packages.forEach { updateHandlers(it) }
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