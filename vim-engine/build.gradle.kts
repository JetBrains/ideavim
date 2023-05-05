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
//    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.devtools.ksp") version "1.8.21-1.0.11"
}

// group 'org.jetbrains.ideavim'
// version 'SNAPSHOT'

repositories {
    mavenCentral()
}

ksp {
  arg("resourcesDir", "$projectDir/src/main/resources")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")

    compileOnly("org.jetbrains:annotations:24.0.1")

    ksp(project(":annotation-processors"))
    implementation(project(":annotation-processors"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0")
}

tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            apiVersion = "1.5"
            freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
        }
    }
}

// --- Linting

//ktlint {
//    version.set("0.48.2")
//}

kotlin {
    explicitApi()
}
