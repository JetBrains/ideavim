/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "1.8.21"
}

group = "com.intellij"
version = "SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.15")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.1")
}
