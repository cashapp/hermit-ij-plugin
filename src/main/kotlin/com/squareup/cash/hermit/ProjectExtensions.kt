package com.squareup.cash.hermit

import arrow.core.Either
import arrow.core.extensions.list.foldable.nonEmpty
import arrow.core.flatMap
import arrow.core.computations.either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.traverse
import arrow.core.fix
import arrow.core.getOrElse
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThreeState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.lang.reflect.InvocationTargetException

/**
 * Does the project have a hermit installation?
 */
fun Project.hasHermit(): Boolean {
    return this.binDir()?.findChild("hermit")?.exists() ?: false
}

fun Project.binDir(): VirtualFile? {
    val root = this.guessProjectDir()
    return root?.findChild("bin")
}

fun Project.projectSdk(): Sdk? {
    return ProjectRootManager.getInstance(this).projectSdk
}

/**
 * Calls the Hermit binary to fetch the current active configuration.
 */
fun Project.hermitProperties(): Result<HermitProperties> {
    if ( !hasHermit() ) {
        return success(HermitProperties(emptyMap(), emptyList()))
    }
    return runBlocking<Result<HermitProperties>> { either {
        val packages = runHermit("list", "-s")
            .map { it.stdout().readLines() }
            .flatMap { packagesFor(it) }
            .bind()

        val env = runHermit("env", "-r")
            .map { it.stdout().readLines() }
            .map { environmentFrom(it) }
            .bind()

        HermitProperties(env, packages)
    } }
}

fun Project.installHermitPackages(): Result<Unit> {
    return if ( !hasHermit() ) {
        success(Unit)
    } else {
        runHermit("install").map{}
    }
}

private fun environmentFrom(lines: List<String>): HashMap<String, String> {
    return lines.fold(HashMap()) { vars, line ->
        val res = line.split("=", limit = 2)
        vars[res[0]] = res[1]
        vars
    }
}

private fun Project.packagesFor(refs: List<String>): Result<List<HermitPackage>> {
    return if ( refs.nonEmpty() ) {
        runHermit("info", "--json", *refs.toTypedArray())
            .map { it.stdout().readText() }
            .flatMap { hermitPackages(Json.parseToJsonElement(it)) }
    } else {
        success(emptyList())
    }
}

private fun hermitPackages(js: JsonElement): Result<List<HermitPackage>> {
    return js.asArray().flatMap { array ->
        array.traverse(Either.applicative()) { elem ->
            runBlocking<Result<HermitPackage>> { either {
                val obj = elem.asObject().bind()
                val ref = obj.field("Reference").flatMap { it.asObject() }.bind()
                val name = ref.field("Name").flatMap { it.asPrimitive() }.map { it.nullableString() }.bind()
                val version = ref.field("Version").flatMap { it.asPrimitive() }.map { it.nullableString() }.getOrElse { "" }
                val channel = ref.field("Channel").flatMap { it.asPrimitive() }.map { it.nullableString() }.getOrElse { "" }
                val root = obj.field("Root").flatMap { it.asPrimitive() }.map { it.nullableString() }.bind()
                val type = when(name) {
                    "go" -> PackageType.Go
                    "openjdk" -> PackageType.JDK
                    "graalvm" -> PackageType.JDK
                    "gradle" -> PackageType.Gradle
                    else -> PackageType.Unknown
                }
                HermitPackage(name, version, root, type, channel)
            }}
        }.fix().map { it.fix() }
    }
}

private fun Process.stdout(): BufferedReader {
    return  this.inputStream.bufferedReader()
}

private fun Project.runHermit(vararg args: String): Result<Process> {
    if ( !this.hasHermit() ) {
        return failure("No Hermit found in the project")
    }

    val cmd = "./hermit"
    val binDir = this.binDir()?.toNioPath()?.toFile()
    val commandLine = GeneralCommandLine(cmd, *args)
    commandLine.workDirectory = binDir
    commandLine.environment["HERMIT_ENV"] = binDir?.parent
    val process = commandLine.createProcess()
    val exitCode = try {
        process.waitFor()
    } catch (e: Exception) {
        return failure("Exception while running $cmd <br/>" + (e.message ?: e.javaClass.simpleName))
    }
    return if ( exitCode != 0 ) {
        failure("Error while running $cmd <br/>" + process.errorStream.bufferedReader().readText())
    } else {
        success(process)
    }
}


/**
 * HACK:
 * This is using reflection, as the API is not available publicly yet.
 * https://github.com/cashapp/hermit-ij-plugin/issues/37
 */
fun Project.isTrustedForHermit(): ThreeState {
    try {
        val trustedProjects = Class.forName("com.intellij.ide.impl.TrustedProjects")
        val isTrusted = trustedProjects.methods.find { it.name == "isTrusted" }!!
        val result = isTrusted.invoke(trustedProjects, this)
        if (result !is Boolean) {
            return ThreeState.UNSURE
        }

        return ThreeState.fromBoolean(result)
    } catch (e: Exception) {
        when(e) {
            is ClassNotFoundException,
            is IllegalAccessException,
            is IllegalArgumentException,
            is InvocationTargetException -> {
                return ThreeState.UNSURE
            }
            else -> {
                throw e
            }
        }
    }
}