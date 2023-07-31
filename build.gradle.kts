import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import java.util.EnumSet

group = "com.squareup.cash.hermit"
version = project.properties["version"] ?: "1.0-SNAPSHOT"

plugins {
  id("idea")
  id("java")
  kotlin("kapt") version "1.8.10"
  id("org.jetbrains.intellij") version "1.15.0"
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32"
}

// region Build, dependencies

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_11
}

repositories {
  mavenCentral()
}

val kotlinVersion = "1.8.10"
val arrowVersion = "0.11.0"

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
  implementation("io.arrow-kt:arrow-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
  kapt("io.arrow-kt:arrow-meta:$arrowVersion")
}

tasks {
  compileKotlin {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
  }

  test {
    systemProperty("idea.force.use.core.classloader", "true")
  }
}
// endregion

// region IJ Plugin setup

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

intellij {
  version.set(product.sdkVersion)
  type.set("IU")
  plugins.set(
    listOf(
      "gradle",
      "java",
      "terminal",
      "org.jetbrains.plugins.go:${product.goPluginVersion}",
      // Needed by Go plugin. See https://github.com/JetBrains/gradle-intellij-plugin/issues/1056
      "org.intellij.intelliLang"
    )
  )
}
tasks {
  runIde {
    // Uncomment this, and set your path accordingly, if you want to debug on GoLand
    // ideDirectory "/Users/juho/Library/Application Support/JetBrains/Toolbox/apps/Goland/ch-0/203.6682.164/GoLand.app/Contents"
  }

  patchPluginXml {
    sinceBuild.set(project.properties["IIC.from.version"] as String)
    val versionSuffix = when(product.releaseType) {
      "release" -> ""
      else -> "-${product.releaseType}"
    }
    version.set("${System.getenv("IJ_PLUGIN_VERSION")}${versionSuffix}") // IJ_PLUGIN_VERSION env var available in CI
  }

  runPluginVerifier {
    // These need to match the versions from
    // https://data.services.jetbrains.com/products?fields=code,name,releases.downloads,releases.version,releases.build,releases.type&code=IIC,IIE,GO
    ideVersions.set(
      listOf(
        "IIC-${project.properties["IIC.from.version"] as String}",
        "GO-${project.properties["GO.from.version"] as String}",
        "IIC-${product.intellijVersion}",
        "GO-${product.golandVersion}"
      )
    )
    failureLevel.set(
      EnumSet.complementOf(
        EnumSet.of(
          // skipping missing dependencies as com.intellij.java provided by IJ raises a false warning
          RunPluginVerifierTask.FailureLevel.MISSING_DEPENDENCIES,
          // skipping experimental API usage, as delaying Gradle execution relies on experimental GradleExecutionAware.
          // if the API changes, we should be able to detect that in our tests when a new version comes out.
          RunPluginVerifierTask.FailureLevel.EXPERIMENTAL_API_USAGES,
          // we do not fail on deprecated API usages, as we want to support older versions where the API has
          // not been deprecate yet, and the newer API not available
          RunPluginVerifierTask.FailureLevel.DEPRECATED_API_USAGES,
          // TODO: fix these
          RunPluginVerifierTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
        )
      )
    )
  }

  publishPlugin {
    token.set(System.getenv("JETBRAINS_TOKEN")) // JETBRAINS_TOKEN env var available in CI
  }

// See https://youtrack.jetbrains.com/issue/KTIJ-782
  buildSearchableOptions {
    enabled = false
  }
}

// endregion
