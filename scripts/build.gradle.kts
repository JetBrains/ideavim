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
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")

    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation("io.ktor:ktor-client-auth:2.2.4")
    // https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    // https://mvnrepository.com/artifact/com.vdurmont/semver4j
    implementation("com.vdurmont:semver4j:3.1.0")
}

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
  mainClass.set("scripts.CalculateNewVersionKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf("${rootProject.rootDir}")
}

tasks.register("changelogUpdateUnreleased", JavaExec::class) {
  mainClass.set("scripts.ChangelogUpdateUnreleasedKt")
  classpath = sourceSets["main"].runtimeClasspath
  args = listOf(project.version.toString(), "${rootProject.rootDir}")
}
