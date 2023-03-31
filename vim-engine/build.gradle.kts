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
}

// group 'org.jetbrains.ideavim'
// version 'SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")

    compileOnly("org.jetbrains:annotations:23.0.0")
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
