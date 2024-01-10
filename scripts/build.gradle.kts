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
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

  implementation("io.ktor:ktor-client-core:2.3.7")
  implementation("io.ktor:ktor-client-cio:2.3.7")
  implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
  implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
  implementation("io.ktor:ktor-client-auth:2.3.7")
  implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

  // This is needed for jgit to connect to ssh
  implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.8.0.202311291450-r")
  implementation("com.vdurmont:semver4j:3.1.0")
}

val releaseType: String? by project

tasks {
  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }
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

tasks.register("updateAffectedRates", JavaExec::class) {
  group = "verification"
  description = "This job updates Affected Rate field on YouTrack"
  mainClass.set("scripts.YouTrackUsersAffectedKt")
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

tasks.register("resetReleaseBranch", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.ResetReleaseBranchKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}

tasks.register("pushChanges", JavaExec::class) {
  mainClass.set("scripts.PushChangesKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(rootProject.rootDir.toString())
}

tasks.register("pushChangesWithReleaseBranch", JavaExec::class) {
  mainClass.set("scripts.PushChangesWithReleaseBranchKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(rootProject.rootDir.toString(), releaseType ?: "")
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

tasks.register("setTeamCityBuildNumber", JavaExec::class) {
  group = "release"
  mainClass.set("scripts.release.SetTeamCityBuildNumberKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), rootProject.rootDir.toString(), releaseType ?: "")
}
