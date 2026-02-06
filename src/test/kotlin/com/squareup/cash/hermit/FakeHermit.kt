package com.squareup.cash.hermit

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

abstract class AbstractHermit {
    abstract fun writeTo(path: Path)
}

data class TestPackage(val name: String, val version: String, val channel: String, val root: String, val env: Map<String, String>)

data class RealHermit(val root: Path, val packages: List<String>) : AbstractHermit() {
    override fun writeTo(path: Path) {
        "hermit init .".runCommand(root)
        packages.forEach { install(it) }
    }

    fun install(pkg: String) {
        "bin/hermit install $pkg".runCommand(root)
    }
}

object BrokenHermit : AbstractHermit() {
    override fun writeTo(path: Path) {
        Files.writeString(path,
            """#!/bin/bash
              totally broken!
            """.trimMargin())
    }
}

data class FakeHermit(val packages: List<TestPackage>) : AbstractHermit() {
    override fun writeTo(path: Path) {
        val packageList = packages.joinToString("\n") { "echo \"${it.name}\"" }
      val envList = packages
        .flatMap { it.env.entries }
        .joinToString("\n") { entry -> "echo \"${entry.key}=${entry.value}\"" }
      val infoList = JsonArray(packages
            .map { p -> JsonObject(mapOf(
                "Reference" to JsonObject(mapOf(
                    "Name" to JsonPrimitive(p.name),
                    "Version" to JsonPrimitive(p.version),
                    "Channel" to JsonPrimitive(p.channel)
                )),
                "Root" to JsonPrimitive(p.root),
                "Description" to JsonPrimitive("description")
            )) }).toString()

        val listBlock = if (packageList.isNotEmpty()) """
            if [ $1 == "list" ]; then
            $packageList
            fi
        """.trimIndent() else ""

        val envBlock = if (envList.isNotEmpty()) """
            if [ $1 == "env" ]; then
            $envList
            fi
        """.trimIndent()
        else ""

        Files.writeString(path,
            """#!/bin/bash
              $listBlock
              $envBlock
              |if [ $1 == "info" ]; then
              |echo '$infoList'
              |fi
            """.trimMargin())
    }
}

fun String.runCommand(workingDir: Path) {
    val builder = ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir.toFile())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)

    builder.environment()["HERMIT_ENV"] = workingDir.toString()
    builder.start().waitFor(60, TimeUnit.SECONDS)
}
