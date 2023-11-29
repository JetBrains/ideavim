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
  `maven-publish`
}

val kotlinVersion: String by project

// group 'org.jetbrains.ideavim'
// version 'SNAPSHOT'

repositories {
    mavenCentral()
}

ksp {
  arg("generated_directory", "$projectDir/src/main/resources/ksp-generated")
  arg("vimscript_functions_file", "engine_vimscript_functions.json")
  arg("ex_commands_file", "engine_ex_commands.json")
  arg("commands_file", "engine_commands.json")
}

afterEvaluate {
    tasks.named("kspTestKotlin").configure { enabled = false }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    compileOnly("org.jetbrains:annotations:24.1.0")

    ksp(project(":annotation-processors"))
    implementation(project(":annotation-processors"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.1")
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

val spaceUsername: String by project
val spacePassword: String by project
val engineVersion: String by project
val uploadUrl: String by project

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.maddyhome.idea.vim"
      artifactId = "vim-engine"
      version = engineVersion
      from(components["java"])
    }
  }
  repositories {
    maven {
      url = uri(uploadUrl)
      credentials {
        username = spaceUsername
        password = spacePassword
      }
    }
  }
}