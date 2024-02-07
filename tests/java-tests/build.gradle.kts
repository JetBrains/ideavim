/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  id("java")
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

group = "org.example"
version = "SNAPSHOT"

repositories {
  mavenCentral()
}

val kotlinVersion: String by project
val ideaVersion: String by project

repositories {
  mavenCentral()
  maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testImplementation(testFixtures(project(":"))) // The root project
}

tasks.test {
  useJUnitPlatform()
}

intellij {
  version.set(ideaVersion)
  // Yaml is only used for testing. It's part of the IdeaIC distribution, but needs to be included as a reference
  plugins.set(listOf("java", "yaml"))
}
