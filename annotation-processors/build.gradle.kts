/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.3.20"
}

val kotlinxSerializationVersion: String by project

group = "com.intellij"
version = "SNAPSHOT"

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
}

dependencies {
  compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.10")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion") {
    // kotlin stdlib is provided by IJ, so there is no need to include it into the distribution
    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
  }
  api(project(":api"))
}
