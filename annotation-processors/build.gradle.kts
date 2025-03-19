/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.0.21"
}

val kotlinxSerializationVersion: String by project

group = "com.intellij"
version = "SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly("com.google.devtools.ksp:symbol-processing-api:2.1.10-1.0.31")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion") {
    // kotlin stdlib is provided by IJ, so there is no need to include it into the distribution
    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
  }
}
