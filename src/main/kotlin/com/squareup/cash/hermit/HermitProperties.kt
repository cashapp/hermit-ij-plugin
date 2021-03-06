package com.squareup.cash.hermit

import com.google.gson.Gson

data class HermitProperties(val env: Map<String,String>, val packages: List<HermitPackage>) {
    fun logString(): String {
        return Gson().toJson(this)
    }
}

enum class PackageType { JDK, Go, Gradle, Unknown }

data class HermitPackage(val name: String, val version: String, val path: String, val type: PackageType, val channel: String) {
    fun displayName(): String {
        if (version != "") {
            return "$name-$version"
        }
        return "$name@$channel"
    }

    fun sdkName(): String {
        return "Hermit (${this.displayName()})"
    }

    fun logString(): String {
        return Gson().toJson(this)
    }
}