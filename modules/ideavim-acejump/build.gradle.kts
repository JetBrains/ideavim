/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  java
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
  compileOnly(project(":"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    plugin("AceJump", "3.8.19")
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

  compilerOptions {
    freeCompilerArgs = listOf(
      // AceJump is compiled with a pre-release Kotlin version
      "-Xskip-prerelease-check",
      "-Xallow-unstable-dependencies",
    )
  }
}
