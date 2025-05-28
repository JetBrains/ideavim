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
  testImplementation("org.junit.vintage:junit-vintage-engine:5.12.2")

  intellijPlatform {
    // Snapshots don't use installers
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions-installers
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      // Using Rider as a target IntelliJ Platform with `useInstaller = true` is currently not supported, please set `useInstaller = false` instead. See: https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1852
      useInstaller = false
    }

    create(ideaType, ideaVersion, useInstaller)
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
    bundledPlugins("com.intellij.java", "org.jetbrains.plugins.yaml")
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

tasks {
  test {
    useJUnitPlatform()

    // Set teamcity env variable locally to run additional tests for leaks.
    println("Project leak checks: If you experience project leaks on TeamCity that doesn't reproduce locally")
    println("Uncomment the following line in build.gradle to enable leak checks (see build.gradle config)")
//      environment("TEAMCITY_VERSION" to "X")
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
