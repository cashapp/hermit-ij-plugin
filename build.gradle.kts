import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.EnumSet

plugins {
  id("idea")
  id("java")
  kotlin("kapt") version "1.8.10"
  id("org.jetbrains.intellij") version "1.13.3"
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32"
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_11
}

// The latest supported versions. Note, these are updated automatically from update-major-versions.sh
val IIC_VERSION = "231.8109.175"
val GO_VERSION = "231.8109.46"
// Unfortunately the GoLand releases do not completely match the Go plugin releases
val GO_PLUGIN_VERSION = IIC_VERSION
// The oldest supported versions.
val IIC_FROM_VERSION = "222.4554.10"
val GO_FROM_VERSION = "222.4554.12"

val kotlin_version = "1.8.10"

group = "com.squareup.cash.hermit"
version = project.properties["version"] ?: "1.0-SNAPSHOT"

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "11"
  kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
}

intellij {
  // Note: The IntelliJ version below needs to match the go plugin version as defined here:
  // https://plugins.jetbrains.com/plugin/9568-go/versions
  version.set(IIC_VERSION)
  type.set("IU")
  plugins.set(
    listOf(
      "gradle",
      "java",
      "terminal",
      "org.jetbrains.plugins.go:$GO_PLUGIN_VERSION",
      // needed by Go plugin. See https://github.com/JetBrains/gradle-intellij-plugin/issues/1056
      "org.intellij.intelliLang"
    )
  )
}

tasks.withType<RunIdeTask> {
  // Uncomment this, and set your path accordingly, if you want to debug on GoLand
  // ideDirectory "/Users/juho/Library/Application Support/JetBrains/Toolbox/apps/Goland/ch-0/203.6682.164/GoLand.app/Contents"
}

tasks.withType<PatchPluginXmlTask> {
  sinceBuild.set(IIC_FROM_VERSION)
  version.set(System.getenv("IJ_PLUGIN_VERSION"))
}

tasks.withType<RunPluginVerifierTask> {
  // These need to match the versions from
  // https://data.services.jetbrains.com/products?fields=code,name,releases.downloads,releases.version,releases.build,releases.type&code=IIC,IIE,GO
  ideVersions.set(listOf("IIC-$IIC_FROM_VERSION", "GO-$GO_FROM_VERSION", "IIC-$IIC_VERSION", "GO-$GO_VERSION"))
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

tasks.withType<PublishPluginTask> {
  token.set(System.getenv("JETBRAINS_TOKEN"))
}

val ARROW_VERSION = "0.11.0"

dependencies {
  implementation("io.arrow-kt:arrow-core:$ARROW_VERSION")
  implementation("io.arrow-kt:arrow-syntax:$ARROW_VERSION")
  kapt("io.arrow-kt:arrow-meta:$ARROW_VERSION")
}

// See https://youtrack.jetbrains.com/issue/KTIJ-782
tasks.buildSearchableOptions {
  enabled = false
}

tasks.test {
  systemProperty("idea.force.use.core.classloader", "true")
}
