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
  id("com.google.devtools.ksp")
    kotlin("plugin.serialization") version "2.3.20"
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
    maven { url = uri("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2") }
}

ksp {
  arg("generated_directory", "$projectDir/src/main/resources/ksp-generated")
  arg("vimscript_functions_file", "engine_vimscript_functions.json")
  arg("ex_commands_file", "engine_ex_commands.json")
  arg("commands_file", "engine_commands.json")
  arg("extensions_file", "ideavim_extensions.json")
  arg("help_directory", layout.buildDirectory.dir("help").get().asFile.path)
  arg("help_file", "engine_help.json")
}

afterEvaluate {
  tasks.named("kspKotlin").configure {
    dependsOn("generateGrammarSource")
    // Written by HelpProcessor, see the generateHelp task
    outputs.dir(layout.buildDirectory.dir("help"))
  }
  tasks.named("kspTestKotlin").configure { enabled = false }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")

    // Temp workaround suggested in https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    // Can be removed when IJPL-159134 is fixed
//    testRuntimeOnly("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:6.1.2")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    compileOnly("org.jetbrains:annotations:26.1.0")

    runtimeOnly("org.antlr:antlr4-runtime:4.13.2")
    antlr("org.antlr:antlr4:4.13.2")

    ksp(project(":annotation-processors"))
    compileOnly(project(":annotation-processors"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")

    compileOnly(kotlin("reflect"))

    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    implementation(project(":api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
}

tasks {
    test {
      useJUnitPlatform()
    }

    generateGrammarSource {
        maxHeapSize = "128m"
        arguments.addAll(listOf("-visitor"))

        // Use `packageName` rather than passing `-package` in `arguments`. ANTLR's `-package` argument only
        // sets the package declaration in the generated code, it does not nest the output in a matching
        // directory. Gradle's javac invocation doesn't care, but IntelliJ resolves Java classes via the
        // package/directory relationship under a source root, so the generated parser, lexer, listener and
        // visitor classes appear unresolvable in the IDE. Setting `packageName` makes Gradle nest the output
        // to match, while leaving the registered source root at `build/generated-src/antlr/main`.
        // Passing `-package` directly is also deprecated, and becomes an error in Gradle 10.
        packageName = "com.maddyhome.idea.vim.parser.generated"
    }

    named("compileKotlin") {
      dependsOn("generateGrammarSource")
    }
    named("compileTestKotlin") {
      dependsOn("generateTestGrammarSource")
    }
}

kotlin {
  compilerOptions {
    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
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
      if (uploadUrl.isNotEmpty()) {
        url = uri(uploadUrl)
        credentials {
          username = spaceUsername
          password = spacePassword
        }
      }
    }
  }
}
