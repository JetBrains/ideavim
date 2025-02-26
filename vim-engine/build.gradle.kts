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
    id("com.google.devtools.ksp") version "2.0.0-1.0.23"
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
    antlr
}

val sourcesJarArtifacts by configurations.registering {
  attributes {
    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
  }
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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.5")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.5")

    // Temp workaround suggested in https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    // Can be removed when IJPL-159134 is fixed
//    testRuntimeOnly("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.12.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    compileOnly("org.jetbrains:annotations:26.0.2")

    runtimeOnly("org.antlr:antlr4-runtime:4.13.2")
    antlr("org.antlr:antlr4:4.13.2")

    ksp(project(":annotation-processors"))
    compileOnly(project(":annotation-processors"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")

    compileOnly(kotlin("reflect"))

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

tasks {
    test {
      useJUnitPlatform()
    }

    generateGrammarSource {
        maxHeapSize = "128m"
        arguments.addAll(listOf("-package", "com.maddyhome.idea.vim.parser.generated", "-visitor"))
        outputDirectory = file("src/main/java/com/maddyhome/idea/vim/parser/generated")
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

java {
  withSourcesJar()
  withJavadocJar()
}

artifacts.add(sourcesJarArtifacts.name, tasks.named("sourcesJar"))

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
