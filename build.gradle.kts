import dev.feedforward.markdownto.DownParser
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.detekt
import java.net.HttpURLConnection
import java.net.URL

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71")
        classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
    }
}

plugins {
    java
    kotlin("jvm") version "1.3.71"

    id("org.jetbrains.intellij") version "0.7.2"
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
    id("org.jetbrains.changelog") version "1.1.2"
    // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

// Import variables from gradle.properties file
val javaVersion: String by project
val kotlinVersion: String by project
val ideaVersion: String by project
val downloadIdeaSources: String by project
val instrumentPluginCode: String by project

val publishChannels: String by project
val publishUsername: String by project
val publishToken: String by project

val slackUrl: String by project

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://jetbrains.bintray.com/intellij-third-party-dependencies") }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compileOnly("org.jetbrains:annotations:20.1.0")

    // https://mvnrepository.com/artifact/com.ensarsarajcic.neovim.java/neovim-api
    testImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
    testImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")

    testImplementation("com.intellij.remoterobot:remote-robot:0.10.3")
    testImplementation("com.intellij.remoterobot:remote-fixtures:1.1.18")
}

// --- Compilation

tasks {
    compileJava {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        options.encoding = "UTF-8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion
//            allWarningsAsErrors = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = javaVersion
//            allWarningsAsErrors = true
        }
    }
}

gradle.projectsEvaluated {
    tasks.compileJava {
        options.compilerArgs.add("-Werror")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("test")
    }
}

// --- Intellij plugin

intellij {
    version = ideaVersion
    pluginName = "IdeaVim"
    updateSinceUntilBuild = false
    downloadSources = downloadIdeaSources.toBoolean()
    instrumentCode = instrumentPluginCode.toBoolean()
    intellijRepo = "https://www.jetbrains.com/intellij-repository"
    setPlugins("java")
}

tasks {
    downloadRobotServerPlugin {
        version = "0.10.0"
    }

    publishPlugin {
        channels(publishChannels.split(","))
        username(publishUsername)
        token(publishToken)
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
    }

    runPluginVerifier {
        ideVersions(listOf("IC-2020.2.3", "IC-2020.3.2"))
        downloadDirectory("${project.buildDir}/pluginVerifier/ides")
        teamCityOutputFormat = true
    }
}

// --- Linting

detekt {
    config = files("${rootProject.projectDir}/.detekt/config.yaml")
    baseline = file("${rootProject.projectDir}/.detekt/baseline.xml")
    input = files("src")

    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}

tasks {
    withType<Detekt> {
        this.jvmTarget = javaVersion
    }
}

ktlint {
    disabledRules.add("no-wildcard-imports")
}

// --- Tests

tasks {
    test {
        exclude("**/propertybased/**")
        exclude("/ui/**")
    }
}

tasks.register<Test>("testWithNeovim") {
    group = "verification"
    systemProperty("ideavim.nvim.test", "true")
    exclude("/ui/**")
}

tasks.register<Test>("testPropertyBased") {
    group = "verification"
    include("**/propertybased/**")
}

tasks.register<Test>("testUi") {
    group = "verification"
    include("/ui/**")
}

// --- Changelog

changelog {
    groups = listOf("Features:", "Changes:", "Deprecations:", "Fixes:", "Merged PRs:")
    itemPrefix = "*"
    path = "${project.projectDir}/CHANGES.md"
    unreleasedTerm = "To Be Released"
    headerParserRegex = "0\\.\\d{2}(.\\d+)?".toRegex()
//    header = { "${project.version}" }
//    version = "0.60"
}

tasks.register("getUnreleasedChangelog") {
    group = "changelog"
    doLast {
        val log = changelog.getUnreleased().toHTML()
        println(log)
    }
}

// --- Slack notification

tasks.register("slackNotification") {
    doLast {
        if (slackUrl.isBlank()) {
            println("Slack Url is not defined")
            return@doLast
        }
        val changeLog = changelog.getLatest().toText()
        val slackDown = DownParser(changeLog, true).toSlack().toString()

        //language=JSON
        val message = """
            {
                "text": "New version of IdeaVim",
                "blocks": [
                    {
                        "type": "selection",
                        "text": {
                            "type": "mrkdwn",
                            "text": "IdeaVim $version has been released\\n$slackDown"
                        }
                    }
                ]
            }
        """.trimIndent()

        val post = URL(slackUrl)
        with(post.openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")

            outputStream.write(message.toByteArray())

            val postRc = responseCode
            println("Response code: $postRc")
            if (postRc == 200) {
                println(inputStream.bufferedReader().use { it.readText() })
            }
        }
    }
}
