/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// Bookmark backend RPC handler, split out of :ideavim-backend into its own content module so the
// optional intellij.platform.bookmarks dependency does not gate the rest of the backend RPC
// providers. Since 2026.x bookmarks is a separate plugin that not every IDE/version bundles.
// The bookmark code lives here (not in :ideavim-backend) so this module's classloader - which
// depends on intellij.platform.bookmarks - can resolve com.intellij.ide.bookmark.* at runtime.

plugins {
  java
  kotlin("jvm")
  id("org.jetbrains.intellij.platform.module")
}

val fleetRpcVersion: String by project
val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

val ideaBranchNumber = ideaVersion.substringBefore('-').split('.').let { parts ->
  val head = parts[0].toIntOrNull() ?: Int.MAX_VALUE // LATEST-EAP-SNAPSHOT
  val isProductVersion = head > 1000                // 2026.2 rather than 262.8665.258
  if (isProductVersion) (head % 100) * 10 + (parts.getOrNull(1)?.toIntOrNull() ?: 1) else head
}

// com.intellij.ide.bookmark.* left lib/app.jar for a bundled plugin in 2026.2; older
// product-info.json files list neither the plugin id nor the module, which fails resolution.
val bookmarksIsSeparatePlugin = ideaBranchNumber >= 262

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  compileOnly(project(":"))
  compileOnly(project(":ideavim-common"))
  compileOnly(project(":vim-engine"))
  compileOnly(project(":api"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  kotlinCompilerPluginClasspath("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin:$kotlinVersion")
  kotlinCompilerPluginClasspath("com.jetbrains.fleet:rpc-compiler-plugin:$fleetRpcVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    bundledModule("intellij.platform.kernel.backend")
    bundledModule("intellij.platform.rpc.backend")
    if (bookmarksIsSeparatePlugin) bundledModule("intellij.platform.bookmarks")
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
    // Must follow javaVersion: this module inlines platform bytecode (Fleet RPC RemoteApi),
    // which on newer platforms (EAP) is built for JVM target 25. A lower target here fails with
    // "Cannot inline bytecode built with JVM target 25 into bytecode ... target 21".
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion))
  }
}
