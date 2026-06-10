package com.squareup.cash.hermit

import com.intellij.openapi.project.Project

interface HermitPropertyHandler {
    fun handle(hermitPackage: HermitPackage, project: Project)

    /**
     * Called once per update with the full set of packages, after every package has been handled.
     * Handlers can use this to reconcile configuration that should be removed when hermit no longer
     * manages a given package type (e.g. reverting a stale Gradle config). Defaults to a no-op.
     */
    fun reconcile(packages: List<HermitPackage>, project: Project) {}
}