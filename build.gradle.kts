/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
    classpath("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

    // This is needed for jgit to connect to ssh
    classpath("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.4.0.202509020913-r")
    classpath("org.kohsuke:github-api:1.305")

    classpath("io.ktor:ktor-client-core:3.3.3")
    classpath("io.ktor:ktor-client-cio:3.3.3")
    classpath("io.ktor:ktor-client-auth:3.3.3")
    classpath("io.ktor:ktor-client-content-negotiation:3.3.3")
    classpath("io.ktor:ktor-serialization-kotlinx-json:3.3.3")

    // This comes from the changelog plugin
//        classpath("org.jetbrains:markdown:0.3.1")
  }
}

plugins {
  java
  kotlin("jvm") version "2.2.21"
  application
  id("java-test-fixtures")

  // NOTE: Unignore "test block comment falls back to line comment when not available" test
  //   After changing this version. It supposed to work on the next version of the gradle plugin
  //   Or go report to the devs that this test still fails.
  id("org.jetbrains.intellij.platform") version "2.10.5"

  id("org.jetbrains.changelog") version "2.5.0"
  id("com.dorongold.task-tree") version "4.0.1"
  id("com.google.devtools.ksp") version "2.2.21-2.0.4"
}

val moduleSources by configurations.registering

// Import variables from gradle.properties file
val javaVersion: String by project
val kotlinVersion: String by project
val ideaVersion: String by project
val ideaType: String by project
val instrumentPluginCode: String by project
val remoteRobotVersion: String by project

val publishChannels: String by project
val publishToken: String by project

val slackUrl: String by project
val youtrackToken: String by project

val releaseType: String? by project

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  api(project(":vim-engine"))
  api(project(":api"))
  ksp(project(":annotation-processors"))
  compileOnly(project(":annotation-processors"))

  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.0.2-1")

  intellijPlatform {
    // Snapshots don't use installers
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions-installers
    var useInstaller = "EAP-SNAPSHOT" !in ideaVersion
    if (ideaType == "RD") {
      // Using Rider as a target IntelliJ Platform with `useInstaller = true` is currently not supported, please set `useInstaller = false` instead. See: https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1852
      useInstaller = false
    }

    // Note that it is also possible to use local("...") to compile against a locally installed IDE
    // E.g. local("/Users/{user}/Applications/IntelliJ IDEA Ultimate.app")
    // Or something like: intellijIdeaUltimate(ideaVersion)
    create(ideaType, ideaVersion) { this.useInstaller = useInstaller }

    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)

    // AceJump is an optional dependency. We use their SessionManager class to check if it's active
    plugin("AceJump", "3.8.19")

    bundledPlugins("org.jetbrains.plugins.terminal")

    bundledModule("intellij.spellchecker")
  }

  moduleSources(project(":vim-engine", "sourcesJarArtifacts"))

  // --------- Test dependencies ----------

  testApi("com.squareup.okhttp3:okhttp:5.3.0")

  // https://mvnrepository.com/artifact/com.ensarsarajcic.neovim.java/neovim-api
  testImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
  testImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")
  testFixturesImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
  testFixturesImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")

  // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testFixturesImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

  // https://mvnrepository.com/artifact/org.mockito.kotlin/mockito-kotlin
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:6.0.0")

  // Temp workaround suggested in https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  // Can be removed when IJPL-159134 is fixed
//  testRuntimeOnly("junit:junit:4.13.2")
  testImplementation("org.junit.vintage:junit-vintage-engine:6.0.1")
//  testFixturesImplementation("org.junit.vintage:junit-vintage-engine:5.10.3")
}

configurations {
  runtimeClasspath {
    exclude(group = "org.antlr", module = "antlr4")
  }
}

val currentJavaVersion = javaToolchains.launcherFor {}.get().metadata.languageVersion.toString()
if (currentJavaVersion != javaVersion) {
  // NOTE: I made this exception because the default Gradle error message is horrible, noone can understand it.
  throw RuntimeException(
    """
    Incorrect java version used for building.
    IdeaVim uses java version $javaVersion, but the current java version is $currentJavaVersion.
    If IntelliJ IDEA is used, change the setting in "Settings | Build, Execution, Deployment | Build Tools | Gradle"
    If build is run from the terminal, set JAVA_HOME environment variable to the correct java version.
  """.trimIndent()
  )
}

tasks {
  test {
    useJUnitPlatform()

    // Set teamcity env variable locally to run additional tests for leaks.
    println("Project leak checks: If you experience project leaks on TeamCity that doesn't reproduce locally")
    println("Uncomment the following line in build.gradle to enable leak checks (see build.gradle config)")
//      environment("TEAMCITY_VERSION" to "X")

    systemProperty("ideavim.nvim.test", System.getProperty("nvim") ?: false)

    // This removes all localization plugins from the test version of IJ.
    // There is a bug that IJ for tests may be loaded with a different locale and some keys may be missing there,
    //   what breaks the tests. This usually happens in EAP versions of IJ.
    classpath -= classpath.filter { it.name.startsWith("localization-") && it.name.endsWith(".jar") }
  }

  compileJava {
    // CodeQL can't resolve the 'by project' property, so we need to give it a hint. This is the minimum version we need
    // so doesn't have to match exactly
    // Hint for the CodeQL autobuilder: sourceCompatibility = 17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    options.encoding = "UTF-8"
  }

  // Note that this will run the plugin installed in the IDE specified in dependencies. To run in a different IDE, use
  // a custom task (see below)
  runIde {
    systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
  }

  // Uncomment to run the plugin in a custom IDE, rather than the IDE specified as a compile target in dependencies
  // Note that the version must be greater than the plugin's target version, for obvious reasons
  // You can also set splitMode and splitModeTarget here to test split mode in a custom IDE
//  val runIdeCustom by intellijPlatformTesting.runIde.registering {
//    type = IntelliJPlatformType.Rider
//    version = "2024.1.2"
//  }

  // Uncomment to run the plugin in a locally installed IDE
//  val runIdeLocal by intellijPlatformTesting.runIde.registering {
//    localPath = file("/Users/{user}/Applications/WebStorm.app")
//  }

  val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
      jvmArgumentProviders += CommandLineArgumentProvider {
        listOf(
          "-Drobot-server.port=8082",
          "-Dide.mac.message.dialogs.as.sheets=false",
          "-Djb.privacy.policy.text=<!--999.999-->",
          "-Djb.consents.confirmation.enabled=false",
          "-Dide.show.tips.on.startup.default.value=false",
          "-Doctopus.handler=" + (System.getProperty("octopus.handler") ?: true),
        )
      }
    }

    plugins {
      robotServerPlugin(remoteRobotVersion)
    }
  }

  val runIdeSplitMode by intellijPlatformTesting.runIde.registering {
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.FRONTEND
  }

  // Add plugin open API sources to the plugin ZIP
  val sourcesJar by registering(Jar::class) {
    dependsOn(moduleSources)
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set(DocsType.SOURCES)
    from(sourceSets.main.map { it.kotlin })
    from(provider {
      moduleSources.map {
        it.map { jarFile -> zipTree(jarFile) }
      }
    })
  }

  buildPlugin {
    dependsOn(sourcesJar)
    from(sourcesJar) { into("lib/src") }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }

  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(javaVersion))

    // See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
    // For the list of bundled versions
    apiVersion.set(KotlinVersion.KOTLIN_2_0)
    freeCompilerArgs = listOf(
      "-Xjvm-default=all-compatibility",

      // Needed to compile the AceJump which uses kotlin beta
      //  Without these two option compilation fails
      "-Xskip-prerelease-check",
      "-Xallow-unstable-dependencies",
    )
//            allWarningsAsErrors = true
  }
}

gradle.projectsEvaluated {
  tasks.compileJava {
//        options.compilerArgs.add("-Werror")
    options.compilerArgs.add("-Xlint:deprecation")
  }
}

// --- Intellij plugin

intellijPlatform {
  pluginConfiguration {
    name = "IdeaVim"
    changeNotes.set(
      """
        <b>Fixes:</b><br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-2431">VIM-2431</a> Fixed <code>&lt;Action&gt;</code> mappings to accept count prefix (e.g., <code>5gj</code> with <code>map gj &lt;Action&gt;(EditorCloneCaretBelow)</code> now executes 5 times)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4105">VIM-4105</a> Fixed <code>a"</code> <code>a'</code> <code>a`</code> text objects to include surrounding whitespace per Vim spec<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4097">VIM-4097</a> Fixed <code>&lt;A-n&gt;</code> (NextOccurrence) with text containing backslashes - e.g., selecting <code>\IntegerField</code> now works correctly<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4094">VIM-4094</a> Fixed UninitializedPropertyAccessException when loading history<br>
        <br>
        <a href="https://youtrack.jetbrains.com/issues/VIM?q=State:%20Fixed%20Fix%20versions:%20${version.get()}">Changelog</a>
        """.trimIndent()
    )

    ideaVersion {
      // Let the Gradle plugin set the since-build version. It defaults to the version of the IDE we're building against
      // specified as two components, `{branch}.{build}` (e.g., "241.15989"). There is no third component specified.
      // The until-build version defaults to `{branch}.*`, but we want to support _all_ future versions, so we set it
      // with a null provider (the provider is important).
      // By letting the Gradle plugin handle this, the Plugin DevKit IntelliJ plugin cannot help us with the "Usage of
      // IntelliJ API not available in older IDEs" inspection. However, since our since-build is the version we compile
      // against, we can never get an API that's newer - it would be an unresolved symbol.
      untilBuild.set(provider { null })
    }
  }

  publishing {
    channels.set(publishChannels.split(","))
    token.set(publishToken)
  }

  signing {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
  }

  pluginVerification {
    teamCityOutputFormat = true
    ides {
      recommended()
    }
  }

  instrumentCode.set(instrumentPluginCode.toBoolean())
}

ksp {
  arg("generated_directory", "$projectDir/src/main/resources/ksp-generated")
  arg("vimscript_functions_file", "intellij_vimscript_functions.json")
  arg("ex_commands_file", "intellij_ex_commands.json")
  arg("commands_file", "intellij_commands.json")
  arg("extensions_file", "ideavim_extensions.json")
}

afterEvaluate {
//  tasks.named("kspKotlin").configure { dependsOn("clean") }
  tasks.named("kspTestFixturesKotlin").configure { enabled = false }
  tasks.named("kspTestFixturesKotlin").configure { enabled = false }
  tasks.named("kspTestKotlin").configure { enabled = false }
}


// --- Changelog

changelog {
  groups.set(listOf("Features:", "Changes:", "Deprecations:", "Fixes:", "Merged PRs:"))
  itemPrefix.set("*")
  path.set("${project.projectDir}/CHANGES.md")
  unreleasedTerm.set("To Be Released")
  headerParserRegex.set("(\\d\\.\\d+(.\\d+)?)".toRegex())
//    header = { "${project.version}" }
//    version = "0.60"
}

// Uncomment to enable FUS testing mode
// tasks {
//    withType<org.jetbrains.intellij.tasks.RunIdeTask> {
//        jvmArgs("-Didea.is.internal=true")
//        jvmArgs("-Dfus.internal.test.mode=true")
//    }
// }


