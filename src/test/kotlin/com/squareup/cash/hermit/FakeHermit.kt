package com.squareup.cash.hermit

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractHermit {
    abstract fun writeTo(path: Path)
}

data class TestPackage(val name: String, val version: String, val channel: String, val root: String, val env: Map<String, String>)

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
        val packageList = packages
            .map { "echo \"${it.name}\"" }
            .joinToString("\n")
        val envList = packages
            .flatMap { it.env.entries }
            .map { entry ->  "echo \"${entry.key}=${entry.value}\""}
            .joinToString("\n")
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
            if [ ${'$'}1 == "list" ]; then
            $packageList
            fi
        """.trimIndent() else ""

        val envBlock = if (envList.isNotEmpty()) """
            if [ ${'$'}1 == "env" ]; then
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