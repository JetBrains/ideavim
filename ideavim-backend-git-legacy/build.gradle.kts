/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// Variant of :ideavim-backend-git for IDEs where the Git plugin keeps GitExecutableManager in its
// main module (2026.1) rather than in the intellij.vcs.git.backend content module. It shares its
// Kotlin sources with :ideavim-backend-git; the only difference is the module descriptor.
// GitPluginExecutableLocator answers only when the Git classes resolve from its own classloader.

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

// Compile the shared sources from :ideavim-backend-git into this module too, so each content
// module's jar carries its own copy of the classes (each is loaded by its own classloader).
sourceSets {
  main {
    java.srcDir("${rootProject.projectDir}/ideavim-backend-git/src/main/java")
  }
}

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  compileOnly(project(":ideavim-common"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    bundledPlugins("Git4Idea")
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
