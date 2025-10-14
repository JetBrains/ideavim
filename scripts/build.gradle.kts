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
  mavenCentral()
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")

  implementation("io.ktor:ktor-client-core:3.3.1")
  implementation("io.ktor:ktor-client-cio:3.3.1")
  implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
  implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
  implementation("io.ktor:ktor-client-auth:3.3.1")
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

  // This is needed for jgit to connect to ssh
  implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.3.0.202506031305-r")
  implementation("com.vdurmont:semver4j:3.1.0")
}

val releaseType: String? by project

kotlin {
  compilerOptions {
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
  }
}

tasks.register("generateIdeaVimConfigurations", JavaExec::class) {
  group = "verification"
  description = "This job tracks if there are any new plugins in marketplace we don't know about"
  mainClass.set("scripts.MainKt")
  classpath = sourceSets["main"].runtimeClasspath
}

tasks.register("checkNewPluginDependencies", JavaExec::class) {
  group = "verification"
  description = "This job tracks if there are any new plugins in marketplace we don't know about"
  mainClass.set("scripts.CheckNewPluginDependenciesKt")
  classpath = sourceSets["main"].runtimeClasspath
}

tasks.register("calculateNewVersion", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.CalculateNewVersionKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf("${rootProject.rootDir}", releaseType ?: "")
}

tasks.register("changelogUpdateUnreleased", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.ChangelogUpdateUnreleasedKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}

tasks.register("commitChanges", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.CommitChangesKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
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

tasks.register("eapReleaseActions", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.releaseEap.EapReleaseActionsKt")
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

tasks.register("setTeamCityBuildNumber", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.SetTeamCityBuildNumberKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}
