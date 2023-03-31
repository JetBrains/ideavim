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
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")

    implementation("io.ktor:ktor-client-core:2.1.3")
    implementation("io.ktor:ktor-client-cio:2.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.3")
    implementation("io.ktor:ktor-client-auth:2.2.4")
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
