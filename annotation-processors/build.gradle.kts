/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  kotlin("jvm")
}

group = "com.intellij"
version = "SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly("com.google.devtools.ksp:symbol-processing-api:1.9.10-1.0.13")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.0")
}