/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

// Core-platform variant of the bookmark backend RPC handler, for IDEs where the bookmark classes
// are part of the monolith core (2026.1 and earlier) rather than the separate
// intellij.platform.bookmarks plugin module (2026.2+). It shares its Kotlin sources with
// :ideavim-backend-bookmarks; the only differences are the module descriptor (no bookmarks
// dependency here) and, at runtime, which classloader resolves com.intellij.ide.bookmark.*.
// BookmarkRemoteApiProvider guards registration so exactly one of the two variants activates.

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

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")

  intellijPlatform {
    defaultRepositories()
  }
}

// Compile the shared sources from :ideavim-backend-bookmarks into this module too, so each content
// module's jar carries its own copy of the classes (each is loaded by its own classloader).
sourceSets {
  main {
    java.srcDir("${rootProject.projectDir}/ideavim-backend-bookmarks/src/main/java")
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
    // This module shares its sources with :ideavim-backend-bookmarks, so it references
    // com.intellij.ide.bookmark.providers.* too and needs those classes on the compile classpath.
    // On 2026.2+ (EAP) they live in the separate intellij.platform.bookmarks module; on 2026.1 and
    // earlier they are in platform core (and the separate module id does not resolve). The runtime
    // descriptor here still declares no bookmarks dependency, so on 2026.2+ this variant simply does
    // not register - BookmarkRemoteApiProvider guards on class resolvability from its own classloader.
    if ("EAP-SNAPSHOT" in ideaVersion) bundledModule("intellij.platform.bookmarks")
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
