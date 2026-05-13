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
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:6.0.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.1.0")
  compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
}

tasks.test {
  useJUnitPlatform()
}