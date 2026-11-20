/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

val ideaBranchNumber = ideaVersion.substringBefore('-').split('.').let { parts ->
  val head = parts[0].toIntOrNull() ?: Int.MAX_VALUE // LATEST-EAP-SNAPSHOT
  val isProductVersion = head > 1000                // 2026.2 rather than 262.8665.258
  if (isProductVersion) (head % 100) * 10 + (parts.getOrNull(1)?.toIntOrNull() ?: 1) else head
}

// intellij.rd.client is a product module since 2026.2; before, its jar only ships for the split-mode frontend
val rdClientModuleExists = ideaBranchNumber >= 262

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  compileOnly(project(":"))
  compileOnly(project(":ideavim-common"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    if (rdClientModuleExists) bundledModule("intellij.rd.client")
  }
  if (!rdClientModuleExists) {
    compileOnly(files({
      project.the<IntelliJPlatformExtension>().platformPath.resolve("plugins/cwm-plugin/lib/frontend-split/rd-client.jar")
    }))
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
