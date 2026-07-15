/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

plugins {
  java
  kotlin("jvm")
  application
}

// group 'org.jetbrains.ideavim'
// version 'SNAPSHOT'

repositories {
  maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
  maven { url = uri("https://jitpack.io") }
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")

  testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  implementation("io.ktor:ktor-client-core:3.5.1")
  implementation("io.ktor:ktor-client-cio:3.5.1")
  implementation("io.ktor:ktor-client-content-negotiation:3.5.1")
  implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.1")
  implementation("io.ktor:ktor-client-auth:3.5.1")
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

  // This is needed for jgit to connect to ssh
  implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.7.0.202606012155-r")
  implementation("com.vdurmont:semver4j:3.1.0")
}

val releaseType: String? by project
val youtrackToken: String by project

kotlin {
  compilerOptions {
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
}

tasks.register("calculateNewVersion", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.CalculateNewVersionKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf("${rootProject.rootDir}", releaseType ?: "")
}

tasks.register("addReleaseTag", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.AddReleaseTagKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}

tasks.register("selectBranch", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.SelectBranchKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}

tasks.register("calculateNewEapVersion", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.CalculateNewEapVersionKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf("${rootProject.rootDir}")
}

tasks.register("calculateNewDevVersion", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.CalculateNewDevVersionKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf("${rootProject.rootDir}")
}

tasks.register("releaseActions", JavaExec::class) {
  group = "other"
  mainClass.set("scripts.ReleaseActionsKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), releaseType ?: "")
  environment("YOUTRACK_TOKEN", youtrackToken)
}

tasks.test {
  useJUnitPlatform()
}
