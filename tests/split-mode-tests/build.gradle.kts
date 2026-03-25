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
  mavenCentral()

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

  // Kodein DI is required at runtime by IDE Starter
  testImplementation("org.kodein.di:kodein-di-jvm:7.31.0")
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
  // Disabled so split-mode-tests are excluded from `gradle test` (they're slow and need a full IDE)
  test {
    useJUnitPlatform()
    enabled = false
  }

  register<Test>("testSplitMode") {
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
    val distDir = project(":").layout.buildDirectory.dir("distributions")
    dependsOn(project(":").tasks.named("buildPlugin"))
    jvmArgumentProviders.add(CommandLineArgumentProvider {
      val dir = distDir.get().asFile
      val pluginZip = dir.listFiles()?.firstOrNull { it.name.endsWith(".zip") }
        ?: error("No plugin ZIP found in ${dir.absolutePath}. Run :buildPlugin first.")
      listOf("-Dideavim.plugin.path=${pluginZip.absolutePath}")
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
