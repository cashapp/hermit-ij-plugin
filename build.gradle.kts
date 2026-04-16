import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import kotlin.math.sin

group = "com.squareup.cash.hermit"
version = project.properties["version"] ?: "1.0-SNAPSHOT"

plugins {
  id("java")
  id("org.jetbrains.intellij.platform") version "2.14.0"

  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

// region Build, dependencies

tasks.withType<JavaCompile> {
  options.release = 21
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
    sdkVersion = properties["IIU.release.version"] as String,
    goPluginVersion = properties["IIU.release.go_plugin.version"] as String,
    intellijVersion = properties["IIU.release.version"] as String,
    golandVersion = properties["GO.release.version"] as String,
  ),
  Product(
    releaseType = "eap",
    // "<major version>-EAP-SNAPSHOT"
    sdkVersion = "${(properties["IIU.eap.version"] as String).split(".")[0]}-EAP-SNAPSHOT",
    goPluginVersion = properties["IIU.eap.go_plugin.version"] as String,
    intellijVersion = properties["IIU.eap.version"] as String,
    golandVersion = properties["GO.eap.version"] as String,
  ),
)
val product = products.first { it.releaseType == (System.getenv("RELEASE_TYPE") ?: "release") }

val verifyOldVersions = System.getenv("VERIFY_VERSIONS") == "old"

val kotlinVersion = "2.3.20"
val arrowVersion = "2.2.2.1"

dependencies {
  intellijPlatform {
    intellijIdea(product.sdkVersion) { useInstaller = false }
    plugins(
      "org.jetbrains.plugins.go:${product.goPluginVersion}"
    )
    bundledPlugins(
      "com.intellij.gradle",
      "com.intellij.java",
      "com.intellij.properties",
      // Needed by Go plugin. See https://github.com/JetBrains/gradle-intellij-plugin/issues/1056
      "org.intellij.intelliLang",
      // Required by the Go plugin; ships as a bundled plugin in the unified IDE.
      "com.intellij.modules.ultimate",
    )
    // Required transitive dependencies of the Go plugin that aren't auto-resolved.
    // See https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1930
    testBundledPlugins("com.intellij.modules.json")
    testBundledModule("intellij.platform.vcs.impl")
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Plugin.Java)
  }

  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  implementation("io.arrow-kt:arrow-core:$arrowVersion")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    freeCompilerArgs = listOf(
      "-Xjvm-default=all-compatibility",
      "-Xjdk-release=21",
    )
  }
}

tasks {
  test {
    systemProperty("idea.force.use.core.classloader", "true")
  }
}

intellijPlatform {
  version = version
  projectName = project.name

  instrumentCode = false // We don't need to scan codebase for jetbrains annotations

  //type.set("IU")

  pluginVerification {
    // These need to match the versions from
    // https://data.services.jetbrains.com/products?fields=code,name,releases.downloads,releases.version,releases.build,releases.type&code=IIU,GO
    if (verifyOldVersions) {
      ides {
        select {
          // Use IntellijIdeaUltimate for pre-unification builds (251 and earlier)
          types = listOf(IntelliJPlatformType.IntellijIdeaUltimate)
          sinceBuild = project.properties["IIU.from.version"] as String
          untilBuild = project.properties["IIU.from.version"] as String
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
          types = listOf(IntelliJPlatformType.IntellijIdea)
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
    sinceBuild.set(project.properties["IIU.from.version"] as String)
    val versionSuffix = when(product.releaseType) {
      "release" -> ""
      else -> "-${product.releaseType}"
    }
    version = "${System.getenv("IJ_PLUGIN_VERSION")}${versionSuffix}" // IJ_PLUGIN_VERSION env var available in CI
  }

  publishPlugin {
    token.set(System.getenv("JETBRAINS_TOKEN")) // JETBRAINS_TOKEN env var available in CI

    // Configure channel based on release type
    channels.set(listOf(
      when(product.releaseType) {
        "release" -> "default"
        "eap" -> "eap"
        else -> "default"
      }
    ))
  }
}
