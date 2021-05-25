package com.squareup.cash.hermit

data class HermitProperties(val env: Map<String,String>, val packages: List<HermitPackage>)

enum class PackageType { JDK, Go, Gradle, Unknown }

data class HermitPackage(val name: String, val version: String, val path: String, val type: PackageType) {
    fun sdkName(): String {
        return "Hermit ($name-$version)"
    }
}