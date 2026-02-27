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
  id("com.google.devtools.ksp")
}

val fleetRpcVersion: String by project

val kotlinVersion: String by project
val ideaType: String by project
val ideaVersion: String by project
val javaVersion: String by project

repositories {
  mavenCentral()
  maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  compileOnly(project(":"))
  compileOnly(project(":modules:ideavim-common"))
  api(project(":vim-engine"))
  api(project(":api"))
  ksp(project(":annotation-processors"))
  compileOnly(project(":annotation-processors"))
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.0.2-1")
  kotlinCompilerPluginClasspath("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin:$kotlinVersion")
  kotlinCompilerPluginClasspath("com.jetbrains.fleet:rpc-compiler-plugin:$fleetRpcVersion")

  intellijPlatform {
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      useInstaller = false
    }

    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    bundledModule("intellij.spellchecker")
    bundledModule("intellij.platform.kernel.impl")
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
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
}

ksp {
  arg("generated_directory", "$projectDir/src/main/resources/ksp-generated")
  arg("commands_file", "frontend_commands.json")
  arg("ex_commands_file", "frontend_ex_commands.json")
  arg("vimscript_functions_file", "frontend_vimscript_functions.json")
  arg("extensions_file", "ideavim_extensions.json")
}

afterEvaluate {
  tasks.named("kspTestKotlin").configure { enabled = false }
}
