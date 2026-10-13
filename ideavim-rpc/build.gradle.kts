/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// Optional module carrying the dependency on intellij.platform.rpc, which IdeaVIM.ideavim-common can't declare
// (see VimRemoteApiResolver). The platform skips it on 2025.3, where the module doesn't exist.

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

// intellij.platform.rpc has a module descriptor since 2026.1; in 2025.3 its classes are in the core and
// product-info.json doesn't list it, which fails resolution.
val rpcModuleExists = ideaBranchNumber >= 261

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

    if (rpcModuleExists) bundledModule("intellij.platform.rpc")
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
