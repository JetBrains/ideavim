/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformRepositoriesExtension

// Set repository for snapshot versions of gradle plugin
pluginManagement {
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// Automatically download JDKs from Foojay API when required toolchain is not installed locally
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_SETTINGS
  repositories {
    maven {
      url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
    }
    maven {
      url = uri("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    val intellijPlatformExt = (this as org.gradle.api.plugins.ExtensionAware)
      .extensions.getByName("intellijPlatform") as IntelliJPlatformRepositoriesExtension
    intellijPlatformExt.defaultRepositories()
  }
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
