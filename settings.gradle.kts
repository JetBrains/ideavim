/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

pluginManagement {
  repositories {
    maven {
      url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
    mavenCentral()
    gradlePluginPortal()
    // Snapshot versions of gradle plugins. The legacy OSSRH endpoint below is deprecated and no longer
    // accessible (Sonatype shut it down in July 2025), so it is kept last only as a harmless fallback.
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}

// Automatically download JDKs from Foojay API when required toolchain is not installed locally
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "IdeaVIM"

include("vim-engine")
include("scripts")
include("annotation-processors")
include("tests:java-tests")
include("tests:property-tests")
include("tests:long-running-tests")
include("tests:ui-ij-tests")
include("tests:ui-py-tests")
include("tests:ui-fixtures")
include("api")
include("ideavim-common")
include("ideavim-frontend")
include("ideavim-backend")
include("ideavim-acejump")
include("ideavim-rider")
include("ideavim-clion-nova")
include("ideavim-terminal")
include("tests:ui-rd-tests")
include("tests:split-mode-tests")
