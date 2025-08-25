import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import kotlin.math.sin

group = "com.squareup.cash.hermit"
version = project.properties["version"] ?: "1.0-SNAPSHOT"

plugins {
  id("java")
  kotlin("kapt") version "2.2.0"
  id("org.jetbrains.intellij.platform") version "2.7.2"

  id("org.jetbrains.kotlin.jvm") version "2.2.0"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

// region Build, dependencies

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

data class Product(
  val releaseType: String, // identifier for this product
  val sdkVersion: String, // the version string passed to the intellij sdk gradle plugin
  val goPluginVersion: String, // a specific version for the go plugin
  val intellijVersion: String,
  val golandVersion: String,
)

val products = listOf(
  Product(
    releaseType = "release",
    sdkVersion = properties["IIC.release.version"] as String,
    goPluginVersion = properties["IIC.release.go_plugin.version"] as String,
    intellijVersion = properties["IIC.release.version"] as String,
    golandVersion = properties["GO.release.version"] as String,
  ),
  Product(
    releaseType = "eap",
    // "<major version>-EAP-SNAPSHOT"
    sdkVersion = "${(properties["IIC.eap.version"] as String).split(".")[0]}-EAP-SNAPSHOT",
    goPluginVersion = properties["IIC.eap.go_plugin.version"] as String,
    intellijVersion = properties["IIC.eap.version"] as String,
    golandVersion = properties["GO.eap.version"] as String,
  ),
)
val product = products.first { it.releaseType == (System.getenv("RELEASE_TYPE") ?: "release") }


val verifyOldVersions = System.getenv("VERIFY_VERSIONS") == "old"

val kotlinVersion = "2.2.0"
val arrowVersion = "0.11.0"

dependencies {
  intellijPlatform {
    intellijIdeaUltimate(product.sdkVersion) {
      useInstaller = false
    }
    pluginVerifier("1.394")
    plugins(
      "org.jetbrains.plugins.go:${product.goPluginVersion}"
    )
    bundledPlugins(
      "com.intellij.gradle",
      "com.intellij.java",
      "com.intellij.properties",
      // Needed by Go plugin. See https://github.com/JetBrains/gradle-intellij-plugin/issues/1056
      "org.intellij.intelliLang"
    )
    testFramework(TestFrameworkType.Bundled, product.sdkVersion)
  }

  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
  implementation("io.arrow-kt:arrow-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
  kapt("io.arrow-kt:arrow-meta:$arrowVersion")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
}

tasks {
  test {
    systemProperty("idea.force.use.core.classloader", "true")
    maxHeapSize = "2g"
  }
  
  verifyPlugin {
    // Plugin verification configuration
  }
}

intellijPlatform {
  version = version
  projectName = project.name

  instrumentCode = false // We don't need to scan codebase for jetbrains annotations

  //type.set("IU")

  pluginVerification {
    failureLevel = listOf(
      org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
      org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
    )
    // These need to match the versions from
    // https://data.services.jetbrains.com/products?fields=code,name,releases.downloads,releases.version,releases.build,releases.type&code=IIC,IIE,GO
    if (verifyOldVersions) {
      ides {
        select {
          types = listOf(IntelliJPlatformType.IntellijIdeaUltimate)
          sinceBuild = project.properties["IIC.from.version"] as String
          untilBuild = project.properties["IIC.from.version"] as String
        }
        select {
          types = listOf(IntelliJPlatformType.GoLand)
          sinceBuild = project.properties["GO.from.version"] as String
          untilBuild = project.properties["GO.from.version"] as String
        }
      }
    } else {
      ides {
        select {
          types = listOf(IntelliJPlatformType.IntellijIdeaUltimate)
          sinceBuild = product.intellijVersion
          untilBuild = product.intellijVersion
        }
        select {
          types = listOf(IntelliJPlatformType.GoLand)
          sinceBuild = product.golandVersion
          untilBuild = product.golandVersion
        }
      }
    }
  }
}

tasks {
  patchPluginXml {
    sinceBuild.set(project.properties["IIC.from.version"] as String)
    val versionSuffix = when(product.releaseType) {
      "release" -> ""
      else -> "-${product.releaseType}"
    }
    version = "${System.getenv("IJ_PLUGIN_VERSION")}${versionSuffix}" // IJ_PLUGIN_VERSION env var available in CI
  }

  publishPlugin {
    token.set(System.getenv("JETBRAINS_TOKEN")) // JETBRAINS_TOKEN env var available in CI
  }
}
