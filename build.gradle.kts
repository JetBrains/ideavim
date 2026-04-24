/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
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
    classpath("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.6.0.202603022253-r")
    classpath("org.kohsuke:github-api:1.305")

    classpath("io.ktor:ktor-client-core:3.4.3")
    classpath("io.ktor:ktor-client-cio:3.4.3")
    classpath("io.ktor:ktor-client-auth:3.4.3")
    classpath("io.ktor:ktor-client-content-negotiation:3.4.3")
    classpath("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

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
  id("org.jetbrains.intellij.platform") version "2.11.0"

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

val fleetRpcVersion: String by project
val publishChannels: String by project
val publishToken: String by project

val slackUrl: String by project
val youtrackToken: String by project

val releaseType: String? by project

repositories {
  mavenCentral()
  maven("https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  api(project(":vim-engine"))
  api(project(":api"))
  api(project(":modules:ideavim-common"))

  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.1.0")
  ksp(project(":annotation-processors"))
  compileOnly(project(":annotation-processors"))
  kotlinCompilerPluginClasspath("org.jetbrains.kotlin:kotlin-serialization-compiler-plugin:$kotlinVersion")
  kotlinCompilerPluginClasspath("com.jetbrains.fleet:rpc-compiler-plugin:$fleetRpcVersion")

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

    pluginModule(runtimeOnly(project(":modules:ideavim-common")))
    pluginModule(runtimeOnly(project(":modules:ideavim-frontend")))
    pluginModule(runtimeOnly(project(":modules:ideavim-backend")))
    pluginModule(runtimeOnly(project(":modules:ideavim-acejump")))
    pluginModule(runtimeOnly(project(":modules:ideavim-rider")))
    pluginModule(runtimeOnly(project(":modules:ideavim-clion-nova")))
    pluginModule(runtimeOnly(project(":modules:ideavim-terminal")))

    bundledModule("intellij.spellchecker")
    bundledModule("intellij.platform.kernel.impl")
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
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:6.0.0")

  // Temp workaround suggested in https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  // Can be removed when IJPL-159134 is fixed
//  testRuntimeOnly("junit:junit:4.13.2")
  testImplementation("org.junit.vintage:junit-vintage-engine:6.0.3")
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

  val runPycharm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PyCharmProfessional
    version = "2026.1"
    task {
      systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
    }
  }

  val runWebstorm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.WebStorm
    version = "2025.3.2"
    task {
      systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
    }
  }

  val runClion by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.CLion
    version = "2026.1"
    task {
      systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
    }
  }

  val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    version = "2026.1"
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
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }
  }
  val runWebstormSplitMode by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.WebStorm
    version = "2025.3.2"
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }
  }
  val runRider by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.Rider
    version = "2026.1"
    task {
      systemProperty("idea.log.debug.categories", "com.maddyhome.idea.vim.handler.EditorHandlersChainLogger")
    }
    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }
  }
  val runCLionSplitMode by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.CLion
    version = "2025.3.2"
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }
  }
  val runPycharmSplitMode by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PyCharmProfessional
    version = "2026.1"
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }
  }

  // Run split mode with a JDWP debug agent on the frontend (JetBrains Client) process.
  // After the frontend window appears, run the "Split Frontend Debugger" run configuration to attach.
  val runIdeSplitModeDebugFrontend by intellijPlatformTesting.runIde.registering {
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }

    prepareSandboxTask {
      val sandboxDir = project.layout.buildDirectory.dir("idea-sandbox").map { it.asFile }
      doLast {
        val debugLine = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006"
        val vmoptions = sandboxDir.get().walkTopDown()
          .filter { it.name == "jetbrains_client64.vmoptions" && it.path.contains("runIdeSplitModeDebugFrontend") }
          .firstOrNull()
          ?: sandboxDir.get().walkTopDown()
            .filter { it.name == "jetbrains_client64.vmoptions" }
            .firstOrNull()

        if (vmoptions != null) {
          val content = vmoptions.readText()
          if (debugLine !in content) {
            vmoptions.appendText("\n$debugLine\n")
            logger.lifecycle("Patched frontend vmoptions with JDWP debug agent: ${vmoptions.absolutePath}")
          }
          logger.lifecycle("Connect a Remote JVM Debug configuration to localhost:5006")
        } else {
          logger.warn(
            "Could not find jetbrains_client64.vmoptions in sandbox. " +
                    "Run `./gradlew runIdeSplitMode` once first to populate the sandbox, then use this task."
          )
        }
      }
    }
  }

  val runPycharmSplitModeDebugFrontend by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PyCharmProfessional
    version = "2026.1"
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }

    prepareSandboxTask {
      val sandboxDir = project.layout.buildDirectory.dir("idea-sandbox").map { it.asFile }
      doLast {
        val debugLine = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006"
        val vmoptions = sandboxDir.get().walkTopDown()
          .filter { it.name == "jetbrains_client64.vmoptions" && it.path.contains("runPycharmSplitModeDebugFrontend") }
          .firstOrNull()
          ?: sandboxDir.get().walkTopDown()
            .filter { it.name == "jetbrains_client64.vmoptions" }
            .firstOrNull()

        if (vmoptions != null) {
          val content = vmoptions.readText()
          if (debugLine !in content) {
            vmoptions.appendText("\n$debugLine\n")
            logger.lifecycle("Patched frontend vmoptions with JDWP debug agent: ${vmoptions.absolutePath}")
          }
          logger.lifecycle("Connect a Remote JVM Debug configuration to localhost:5006")
        } else {
          logger.warn(
            "Could not find jetbrains_client64.vmoptions in sandbox. " +
                    "Run `./gradlew runPycharmSplitMode` once first to populate the sandbox, then use this task."
          )
        }
      }
    }
  }

  val testIdeSplitMode by intellijPlatformTesting.testIde.registering {
    splitMode = true
    splitModeTarget = SplitModeAware.SplitModeTarget.BOTH

    plugins {
      plugin("AceJump", "3.8.22")
      plugin("org.jetbrains.IdeaVim-EasyMotion", "1.16")
    }

    task {
      useJUnitPlatform()
    }
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
        <b>Features:</b><br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-1693">VIM-1693</a> Added <code>:autocmd</code> command - run Ex commands on editor events such as <code>BufRead</code>, <code>BufWrite</code>, <code>BufEnter</code>, <code>BufLeave</code>, <code>InsertEnter</code>, <code>InsertLeave</code>, <code>WinEnter</code>, <code>WinLeave</code>, <code>FocusGained</code>, <code>FocusLost</code>, and <code>FileType</code>; supports <code>augroup</code> and file pattern matching (e.g., <code>autocmd BufWritePre *.py echo "saving python"</code>)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-268">VIM-268</a> Added file name completion in ex commands - press <code>&lt;Tab&gt;</code>/<code>&lt;S-Tab&gt;</code> to cycle through file matches in <code>:edit</code>, <code>:split</code>, <code>:vsplit</code>, <code>:write</code>, <code>:read</code>, <code>:source</code>, and <code>:find</code> commands; use arrow keys to navigate the completion panel<br>
        * New VimScript functions: <code>add()</code>, <code>call()</code>, <code>extend()</code>, <code>extendnew()</code>, <code>filter()</code>, <code>flatten()</code>, <code>flattennew()</code>, <code>foreach()</code>, <code>has_key()</code>, <code>indexof()</code>, <code>insert()</code>, <code>items()</code>, <code>keys()</code>, <code>map()</code>, <code>mapnew()</code>, <code>reduce()</code>, <code>remove()</code>, <code>slice()</code>, <code>sort()</code>, <code>uniq()</code>, <code>values()</code><br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-1595">VIM-1595</a> Added support for <code>:read</code> command - insert file content below current line (e.g., <code>:read file.txt</code>, <code>0read file.txt</code>)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-1595">VIM-1595</a> Added support for <code>:read!</code> command - insert shell command output below current line (e.g., <code>:read! echo "hello"</code>)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-566">VIM-566</a> Added support for <code>zA</code> command - toggle folds recursively<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-566">VIM-566</a> Added support for <code>zr</code> command - increase fold level to show more folds<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-566">VIM-566</a> Added support for <code>zm</code> command - decrease fold level to hide more folds<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-566">VIM-566</a> Added support for <code>zf</code> command - create fold from selection or motion<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-566">VIM-566</a> Added support for <code>:set foldlevel</code> option - control fold visibility level<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-1158">VIM-1158</a> Added <code>gw</code> command - reformat code like <code>gq</code> but preserving the cursor position<br>
        <br>
        <b>Fixes:</b><br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4197">VIM-4197</a> Fixed Vim features (e.g., <code>f</code>, <code>w</code>, text objects) not working in Java files decompiled from Kotlin class files<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4112">VIM-4112</a> Fixed undo after block-visual edit (<code>&lt;C-V&gt;...x</code>, <code>&lt;C-V&gt;...c</code>, <code>&lt;C-V&gt;...I</code>) leaving stray carets in normal mode<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4176">VIM-4176</a> Fixed race condition in single-line output panel that could cause <code>*</code> search wrapping to behave unreliably<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4175">VIM-4175</a> Fixed search "not found" showing previous "Hit ENTER" text alongside the error - panel is now cleared before displaying errors like "E486: Pattern not found"<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4135">VIM-4135</a> Fixed IdeaVim not loading in Rider<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4134">VIM-4134</a> Fixed undo in commentary - <code>gcc</code>/<code>gc{motion}</code> changes are now properly grouped as a single undo step<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4134">VIM-4134</a> Fixed <code>=</code> (format/auto-indent) action in split mode<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4134">VIM-4134</a> Fixed global marks causing errors when used inside write actions (e.g., during document modifications)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4105">VIM-4105</a> Fixed <code>a"</code> <code>a'</code> <code>a`</code> text objects to include surrounding whitespace per Vim spec<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4097">VIM-4097</a> Fixed <code>&lt;A-n&gt;</code> (NextOccurrence) with text containing backslashes - e.g., selecting <code>\IntegerField</code> now works correctly<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4094">VIM-4094</a> Fixed UninitializedPropertyAccessException when loading history<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4016">VIM-4016</a> Fixed <code>:edit</code> command when project has no source roots<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-3948">VIM-3948</a> Improved hint generation visibility checks for better UI component detection<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4195">VIM-4195</a> Fixed settings not being saved in remote development (split) mode<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-3473">VIM-3473</a> Fixed "Reload .ideavimrc" action in remote development (split) mode - no longer causes File Cache Conflict dialogs<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-2821">VIM-2821</a> Fixed undo grouping when repeating text insertion with <code>.</code> in remote development (split mode)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-1705">VIM-1705</a> Fixed window-switching commands (e.g., <code>&lt;C-w&gt;h</code>) during macro playback<br>
        * Fixed <code>pumvisible()</code> function returning incorrect result (was inverted)<br>
        * Fixed <code>&lt;Esc&gt;</code> not properly exiting insert mode in Rider/CLion when canceling a completion lookup<br>
        * Fixed <code>&lt;Esc&gt;</code> not exiting insert mode after <code>&lt;C-Space&gt;</code> completion in Rider<br>
        * Fixed <code>&lt;Esc&gt;</code> in search bar no longer inserts <code>^[</code> literal text when search is not found - panel is now properly closed<br>
        * Fixed IdeaVim entering broken state when a VimScript extension plugin fails to initialize<br>
        * Fixed compatibility issues with external plugins (e.g., IdeaVim-EasyMotion, multicursor)<br>
        * Fixed recursive key mappings (e.g., <code>map b wbb</code>) causing an apparent infinite loop - <code>maxmapdepth</code> limit now properly terminates the entire mapping chain<br>
        * Fixed NERDTree <code>gs</code>/<code>gi</code> preview split commands to keep focus on the tree<br>
        * Fixed visual marks (<code>&lt;</code> and <code>&gt;</code>) position tracking after text deletion - <code>gv</code> now re-selects correctly<br>
        * Fixed <code>IndexOutOfBoundsException</code> when using text objects like <code>a)</code> at end of file<br>
        * Fixed high CPU usage while showing command line<br>
        * Fixed comparison of String and Number in VimScript expressions<br>
        * Fixed <code>\/</code>, <code>\?</code>, and <code>\&</code> in Ex command ranges now correctly report E35/E33 errors when no previous search or substitute pattern exists, instead of crashing<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4172">VIM-4172</a> IdeaVim is now disabled in Python Console to prevent key interference<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4113">VIM-4113</a> Fixed Visual mode commands (e.g., <code>:'&lt;,'&gt;sort</code>) failing when run off the Event Dispatch Thread<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-3727">VIM-3727</a> Fixed Enter and arrow keys not working in Python Console in split mode<br>
        * Fixed NERDTree navigation (<code>j</code>/<code>k</code>/<code>G</code>/<code>gg</code>/<code>p</code>/<code>&lt;C-J&gt;</code>/<code>&lt;C-K&gt;</code>) poor performance in split mode - navigation now uses Swing actions directly instead of going through backend RPC<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4180">VIM-4180</a> Fixed ReplaceWithRegister plugin's default <code>gr</code>/<code>grr</code> mappings overriding user-defined key mappings<br>
        * Fixed <code>IndexOutOfBoundsException</code> when using <code>:command</code> with <code>-nargs</code> option but without a command name<br>
        * Fixed spurious beep when pressing <code>&lt;Esc&gt;</code> to cancel register selection in normal mode (after pressing <code>"</code>)<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4202">VIM-4202</a> Fixed <code>&lt;S-Tab&gt;</code> being intercepted by IdeaVim - users can now remap <code>&lt;S-Tab&gt;</code> to other IntelliJ actions<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4202">VIM-4202</a> Fixed <code>gcc</code>/<code>gc{motion}</code> commentary leaving editor in incorrect mode in Rider/CLion split mode<br>
        * <a href="https://youtrack.jetbrains.com/issue/VIM-4115">VIM-4115</a> Fixed NullPointerException in <code>CommandKeyConsumer</code> when pressing Esc after disabling and re-enabling IdeaVim with an open command line<br>
        <br>
        <b>Merged PRs:</b><br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1704">1704</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4202 Don't intercept all &lt;S-Tab&gt;<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1703">1703</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4202 Fix state after commentary action<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1700">1700</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4139 Compute nesting depth for fold regions<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1699">1699</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4112 collapse restored carets after undo of block-visual edit<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1696">1696</a> by <a href="https://github.com/citizenmatt">citizenmatt</a>: VIM-4197 Fix missing Vim features in Java files decompiled from Kotlin class files<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1695">1695</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-1693 Implement autocmd<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1690">1690</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: Make nerdtree work without calling backend actions<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1688">1688</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4172 Disable ideavim in Python Console<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1687">1687</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: Restore old VimPLugin method signatures<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1685">1685</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-3727 Fix Python console Enter and arrow keys in split mode<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1548">1548</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-1158 Add <code>gw</code> to reformat code with preserving the cursor position<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1682">1682</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-268 Complete file names in edit command<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1632">1632</a> by <a href="https://github.com/chylex">chylex</a>: Fix pumvisible returning opposite result<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1615">1615</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: Fix IndexOutOfBoundsException in findBlock when caret is at end of file<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1613">1613</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-3473 Sync ideavim in remdev<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1608">1608</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: VIM-4134 format using = action in split mode<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1585">1585</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: Break in case of maximum recursion depth<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1414">1414</a> by <a href="https://github.com/citizenmatt">Matt Ellis</a>: Refactor/functions<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1442">1442</a> by <a href="https://github.com/citizenmatt">Matt Ellis</a>: Fix high CPU usage while showing command line<br>
        * <a href="https://github.com/JetBrains/ideavim/pull/1665">1665</a> by <a href="https://github.com/1grzyb1">1grzyb1</a>: Fix visual selection commands failing off-EDT due to nested write-in-read action<br>
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
  arg("commands_file", "frontend_commands.json")
  arg("ex_commands_file", "frontend_ex_commands.json")
  arg("vimscript_functions_file", "frontend_vimscript_functions.json")
  arg("extensions_file", "ideavim_extensions.json")
}

afterEvaluate {
  tasks.named("kspTestFixturesKotlin").configure { enabled = false }
  tasks.named("kspTestKotlin").configure { enabled = false }
}

// Allow test and testFixtures sources to access `internal` members from :modules:ideavim-common.
// This is needed because plugin source code was split into the common module during the
// plugin split, but tests remain in the root project. Kotlin's -Xfriend-paths compiler flag grants
// internal visibility across module boundaries for testing purposes.
// We add both the class directory and the JAR because the IntelliJ Platform Gradle plugin may resolve
// classes from the composed/instrumented JAR rather than raw class files.
val commonProject = project(":modules:ideavim-common")
val commonClassesDir = commonProject.layout.buildDirectory.dir("classes/kotlin/main").get().asFile
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
  friendPaths.from(commonClassesDir)
  friendPaths.from(commonProject.layout.buildDirectory.dir("libs"))
}
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestFixturesKotlin") {
  friendPaths.from(commonClassesDir)
  friendPaths.from(commonProject.layout.buildDirectory.dir("libs"))
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


