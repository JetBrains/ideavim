/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  kotlin("jvm")
}

val kotlinVersion: String by project

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.13.4"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.0.2-1")
  compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")
}

tasks.test {
  useJUnitPlatform()
}