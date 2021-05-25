package com.squareup.cash.hermit.execution

import com.goide.execution.extension.GoExecutorExtension
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.squareup.cash.hermit.Hermit

class HermitGoExecutor : GoExecutorExtension() {
    override fun getExtraEnvironment(
        project: Project,
        module: Module?,
        currentEnvironment: MutableMap<String, String>
    ): MutableMap<String, String> {
        val mutable = HashMap<String,String>()
        mutable.putAll(Hermit(project).environment().variables())
        return mutable
    }
}