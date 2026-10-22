/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// Git plugin integration, split out of :ideavim-backend into its own content module so the
// optional Git dependency does not gate the rest of the backend. When the Git plugin is
// missing or disabled, this module is skipped and GitCliClient falls back to `git` on PATH.
// This variant depends on the intellij.vcs.git.backend module (newer IDEs); its sources are
// shared with :ideavim-backend-git-legacy, which depends on the Git4Idea plugin (2026.1).

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
