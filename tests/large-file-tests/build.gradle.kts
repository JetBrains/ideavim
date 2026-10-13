/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation("org.kodein.di:kodein-di-jvm:7.33.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }
    testFramework(TestFrameworkType.Starter)
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

tasks {
  // Disabled so large-file-tests are excluded from `gradle test` (they're slow and need a full IDE)
  test {
    useJUnitPlatform()
    enabled = false
  }

  register<Test>("testLargeFiles") {
    group = "verification"
    useJUnitPlatform()

    testClassesDirs = sourceSets["test"].output.classesDirs
    // Include both the standard test classpath and the intellijPlatform test classpath
    // (which contains ide-starter, driver-sdk, driver-client, etc.)
    classpath = sourceSets["test"].runtimeClasspath +
            configurations.getByName("intellijPlatformTestClasspath")

    // Pass the plugin ZIP path so tests can install IdeaVim into the launched IDE.
    // Resolve the distributions dir at configuration time to avoid capturing Project references
    // (which break the configuration cache).
    // `-PideavimPluginZip=/path/to/IdeaVIM.zip` runs the tests against another build, e.g. a released version, to
    // compare performance across versions. `-PideavimPluginZip=none` runs them without IdeaVim, as a baseline.
    val distDir = project(":").layout.buildDirectory.dir("distributions")
    val customPluginZip = providers.gradleProperty("ideavimPluginZip")
    if (!customPluginZip.isPresent) {
      dependsOn(project(":").tasks.named("buildPlugin"))
    }
    jvmArgumentProviders.add(CommandLineArgumentProvider {
      val pluginPath = customPluginZip.orNull?.let { if (it == "none") it else File(it).absolutePath } ?: run {
        val dir = distDir.get().asFile
        val pluginZip = dir.listFiles()?.firstOrNull { it.name.endsWith(".zip") }
          ?: error("No plugin ZIP found in ${dir.absolutePath}. Run :buildPlugin first.")
        pluginZip.absolutePath
      }
      listOf("-Dideavim.plugin.path=$pluginPath")
    })

    // `-PrustProject=/path/to/ash` enables the RustRover tests, which run on a checkout of https://github.com/ash-rs/ash
    val rustProject = providers.gradleProperty("rustProject")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
      rustProject.orNull?.let { listOf("-Dideavim.rust.project=${File(it).absolutePath}") } ?: emptyList()
    })

    // Metrics for TeamCity, see IdeaVimLargeFileTestBase.publishMetric
    val metricsDir = layout.buildDirectory.dir("metrics")
    doFirst { metricsDir.get().asFile.deleteRecursively() }
    jvmArgumentProviders.add(CommandLineArgumentProvider {
      listOf("-Dideavim.metrics.dir=${metricsDir.get().asFile.absolutePath}")
    })

    // Always run, never use cache
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }

    testLogging {
      events("passed", "skipped", "failed")
      showStandardStreams = true
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}
