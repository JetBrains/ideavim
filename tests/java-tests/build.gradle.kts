import org.jetbrains.intellij.platform.gradle.TestFrameworkType

/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

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
  testImplementation(testFixtures(project(":"))) // The root project
  testImplementation("org.junit.vintage:junit-vintage-engine:5.10.5")

  intellijPlatform {
    // Snapshots don't use installers
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions-installers
    val useInstaller = "EAP-SNAPSHOT" !in ideaVersion

    create(ideaType, ideaVersion, useInstaller)
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
    bundledPlugins("com.intellij.java")

    // [VERSION UPDATE] 2025.1+
    // Dirty hack: in 2025.1+ the yaml plugin is extracted. So, the tests that use YAML don't work.
    // This fixes the bug, but the proper fix should be provided by the gradle plugin
    //   Internal information: https://jetbrains.slack.com/archives/C01F9VBU064/p1738921964822329
    when (ideaVersion) {
      "2024.2.1" -> {
        bundledPlugins("org.jetbrains.plugins.yaml")
      }

      "LATEST-EAP-SNAPSHOT" -> {
        bundledModule("intellij.yaml.editing") // Note: This should be "org.jetbrains.plugins.yaml" plugin instead of module
      }

      else -> error("Unsupported version: $ideaVersion")
    }
    instrumentationTools()
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

tasks {
  test {
    useJUnitPlatform()

    // Set teamcity env variable locally to run additional tests for leaks.
    // By default, this test runs on TC only, but this test doesn't take a lot of time,
    //   so we can turn it on for local development
    if (environment["TEAMCITY_VERSION"] == null) {
      println("Set env TEAMCITY_VERSION to X to enable project leak checks from the platform")
      environment("TEAMCITY_VERSION" to "X")
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
