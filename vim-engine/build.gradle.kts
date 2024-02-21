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
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    `maven-publish`
    antlr
}

val kotlinVersion: String by project
val kotlinxSerializationVersion: String by project

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
  tasks.named("kspKotlin").configure { dependsOn("generateGrammarSource") }
  tasks.named("kspTestKotlin").configure { enabled = false }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    compileOnly("org.jetbrains:annotations:24.1.0")

    runtimeOnly("org.antlr:antlr4-runtime:4.13.1")
    antlr("org.antlr:antlr4:4.13.1")

    ksp(project(":annotation-processors"))
    implementation(project(":annotation-processors"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
}

tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
    }

    generateGrammarSource {
        maxHeapSize = "128m"
        arguments.addAll(listOf("-package", "com.maddyhome.idea.vim.regexp.parser.generated", "-visitor"))
        outputDirectory = file("src/main/java/com/maddyhome/idea/vim/regexp/parser/generated")
    }

    named("compileKotlin") {
      dependsOn("generateGrammarSource")
    }
    named("compileTestKotlin") {
      dependsOn("generateTestGrammarSource")
    }

    compileKotlin {
        kotlinOptions {
            apiVersion = "1.9"
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

java {
  withSourcesJar()
  withJavadocJar()
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