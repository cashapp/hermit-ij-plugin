package com.squareup.cash.hermit

import com.jetbrains.rd.util.string.printToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path

data class TestPackage(val name: String, val version: String, val root: String, val env: Map<String, String>)

data class FakeHermit(val packages: List<TestPackage>) {
    fun writeTo(path: Path) {
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
                    "Version" to JsonPrimitive(p.version)
                )),
                "Root" to JsonPrimitive(p.root),
                "Description" to JsonPrimitive("description")
            )) }).toString()

        val listBlock = """
            if [ ${'$'}1 == "list" ]; then
            $packageList
            fi
        """.trimIndent()

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