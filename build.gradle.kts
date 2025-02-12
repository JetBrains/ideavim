/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

import dev.feedforward.markdownto.DownParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.intellij.markdown.ast.getTextInNode
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware
import org.kohsuke.github.GHUser
import java.net.HttpURLConnection
import java.net.URL

buildscript {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
    classpath("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

    // This is needed for jgit to connect to ssh
    classpath("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.1.0.202411261347-r")
    classpath("org.kohsuke:github-api:1.305")

    classpath("io.ktor:ktor-client-core:3.0.3")
    classpath("io.ktor:ktor-client-cio:3.0.3")
    classpath("io.ktor:ktor-client-auth:3.1.0")
    classpath("io.ktor:ktor-client-content-negotiation:3.0.3")
    classpath("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

    // This comes from the changelog plugin
//        classpath("org.jetbrains:markdown:0.3.1")
  }
}

plugins {
  java
  kotlin("jvm") version "2.0.0"
  application
  id("java-test-fixtures")
  id("org.jetbrains.intellij.platform") version "2.2.1"
  id("org.jetbrains.changelog") version "2.2.1"
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("com.dorongold.task-tree") version "4.0.0"
  id("com.google.devtools.ksp") version "2.0.0-1.0.23"
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
  ksp(project(":annotation-processors"))
  compileOnly(project(":annotation-processors"))

  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:26.0.2")

  intellijPlatform {
    // Snapshots don't use installers
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions-installers
    val useInstaller = "EAP-SNAPSHOT" !in ideaVersion

    // Note that it is also possible to use local("...") to compile against a locally installed IDE
    // E.g. local("/Users/{user}/Applications/IntelliJ IDEA Ultimate.app")
    // Or something like: intellijIdeaUltimate(ideaVersion)
    create(ideaType, ideaVersion, useInstaller)

    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)

    // AceJump is an optional dependency. We use their SessionManager class to check if it's active
    plugin("AceJump", "3.8.19")

    // [VERSION UPDATE] 2024.3+
    // Dirty hack: in 2024.3+ the json plugin is extracted. So, the tests that use JSON don't work.
    // If the IdeaVim minimal version is bumped to 2024.3, leave only `bundledPlugins...` line
    // If we start to support a new version like 2024.2.2 or 2025.1.1, please add it to the correct branch of this when
    // This check intentionally has a pattern where we explicitly specify if to use the JSON plugin for each
    //   supported version or not to prevent developers from figuring out what happened with JSON.
    when (ideaVersion) {
      "2024.2.1" -> { /* Nothing */ }
      "LATEST-EAP-SNAPSHOT", "2024.3", "2024.3.2.2" -> bundledPlugins("com.intellij.modules.json")
      else -> error("Unsupported version: $ideaVersion")
    }

    bundledPlugins("org.jetbrains.plugins.terminal")
  }

  moduleSources(project(":vim-engine", "sourcesJarArtifacts"))

  // --------- Test dependencies ----------

  testApi("com.squareup.okhttp3:okhttp:4.12.0")

  // https://mvnrepository.com/artifact/com.ensarsarajcic.neovim.java/neovim-api
  testImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
  testImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")
  testFixturesImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
  testFixturesImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")

  // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test
  testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
  testFixturesImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

  // https://mvnrepository.com/artifact/org.mockito.kotlin/mockito-kotlin
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.5")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.5")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.5")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.5")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.5")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:5.10.5")

  // Temp workaround suggested in https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  // Can be removed when IJPL-159134 is fixed
//  testRuntimeOnly("junit:junit:4.13.2")
  testImplementation("org.junit.vintage:junit-vintage-engine:5.10.5")
//  testFixturesImplementation("org.junit.vintage:junit-vintage-engine:5.10.3")
}

configurations {
  runtimeClasspath {
    exclude(group = "org.antlr", module = "antlr4")
  }
}

tasks {
  test {
    useJUnitPlatform()

    // Set teamcity env variable locally to run additional tests for leaks.
    // By default, this test runs on TC only, but this test doesn't take a lot of time,
    //   so we can turn it on for local development
    if (environment["TEAMCITY_VERSION"] == null) {
      println("Set env TEAMCITY_VERSION to X to enable project leak checks from the platform")
      environment("TEAMCITY_VERSION" to "X")
    }

    systemProperty("ideavim.nvim.test", System.getProperty("nvim") ?: false)
  }

  compileJava {
    // CodeQL can't resolve the 'by project' property, so we need to give it a hint. This is the minimum version we need
    // so doesn't have to match exactly
    // Hint for the CodeQL autobuilder: sourceCompatibility = 17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    options.encoding = "UTF-8"
  }

  compileKotlin {
    kotlinOptions {
      jvmTarget = javaVersion
      // See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
      // For the list of bundled versions
      apiVersion = "1.9"
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

  compileTestKotlin {
    kotlinOptions {
      jvmTarget = javaVersion
      apiVersion = "1.9"

      // Needed to compile the AceJump which uses kotlin beta
      //  Without these two option compilation fails
      freeCompilerArgs += listOf("-Xskip-prerelease-check", "-Xallow-unstable-dependencies")
//            allWarningsAsErrors = true
    }
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
        Undo in IdeaVim now works like in Vim<br/>
        Caret movement is no longer a separate undo step, and full insert is undoable in one step.<br/>
        <a href="https://youtrack.jetbrains.com/issue/VIM-547/Undo-splits-Insert-mode-edits-into-separate-undo-chunks">Share Feedback</a>
        <br/>
        <br/>
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

  verifyPlugin {
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

// --- Kover

koverMerged {
  enable()
}

// --- Slack notification

tasks.register("slackNotification") {
  doLast {
    if (version.toString().last() != '0') return@doLast
    if (slackUrl.isBlank()) {
      println("Slack Url is not defined")
      return@doLast
    }

    val changeLog = changelog.renderItem(changelog.getLatest(), Changelog.OutputType.PLAIN_TEXT)
    val slackDown = DownParser(changeLog, true).toSlack().toString()

    //language=JSON
    val message = """
            {
                "text": "New version of IdeaVim",
                "blocks": [
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "IdeaVim $version has been released\n$slackDown"
                        }
                    }
                ]
            }
        """.trimIndent()

    println("Parsed data: $slackDown")
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
      } else {
        println(errorStream.bufferedReader().use { it.readText() })
      }
    }
  }
}

// Uncomment to enable FUS testing mode
// tasks {
//    withType<org.jetbrains.intellij.tasks.RunIdeTask> {
//        jvmArgs("-Didea.is.internal=true")
//        jvmArgs("-Dfus.internal.test.mode=true")
//    }
// }

// --- Update authors
tasks.register("updateAuthors") {
  doLast {
    val uncheckedEmails = setOf(
      "aleksei.plate@jetbrains.com",
      "aleksei.plate@teamcity",
      "aleksei.plate@TeamCity",
      "alex.plate@192.168.0.109",
      "nikita.koshcheev@TeamCity",
      "TeamCity@TeamCity",
    )
    updateAuthors(uncheckedEmails)
  }
}

val prId: String by project

tasks.register("updateMergedPr") {
  doLast {
    val x = changelog.getUnreleased()
    println("x")
//    if (project.hasProperty("prId")) {
//      println("Got pr id: $prId")
//      updateMergedPr(prId.toInt())
//    } else {
//      error("Cannot get prId")
//    }
  }
}

tasks.register("updateChangelog") {
  doLast {
    updateChangelog()
  }
}

tasks.register("updateYoutrackOnCommit") {
  doLast {
    updateYoutrackOnCommit()
  }
}

val vimProjectId = "22-43"
val fixVersionsFieldId = "123-285"
val fixVersionsFieldType = "VersionProjectCustomField"
val fixVersionsElementType = "VersionBundleElement"

tasks.register("releaseActions") {
  group = "other"
  doLast {
    if (releaseType == "patch") return@doLast

    val tickets = getYoutrackTicketsByQuery("%23%7BReady+To+Release%7D%20and%20tag:%20%7BIdeaVim%20Released%20In%20EAP%7D%20")
    if (tickets.isNotEmpty()) {
      println("Updating statuses for tickets: $tickets")
      setYoutrackStatus(tickets, "Fixed")
      println("Checking if version $version exists...")
      val versionId = getVersionIdByName(version.toString())
      if (versionId == null) {
        addReleaseToYoutrack(version.toString())
      } else {
        println("Version $version already exists in YouTrack. Version id: $versionId")
      }
      setYoutrackFixVersion(tickets, version.toString())
    } else {
      println("No tickets to update statuses")
    }
  }
}

tasks.register("integrationsTest") {
  group = "other"
  doLast {
    val testTicketId = "VIM-2784"

    // YouTrack set to Ready To Release on Fix commit
    setYoutrackStatus(listOf(testTicketId), "Ready To Release")
    if ("Ready To Release" != getYoutrackStatus(testTicketId)) {
      error("Ticket status was not updated")
    }
    setYoutrackStatus(listOf(testTicketId), "Open")

    // Check YouTrack requests
    val prevStatus = getYoutrackStatus(testTicketId)
    setYoutrackStatus(listOf(testTicketId), "Ready To Release")
    val tickets = getYoutrackTicketsByQuery("%23%7BReady+To+Release%7D")
    if (testTicketId !in tickets) {
      error("Test ticket is not found in request")
    }
    setYoutrackStatus(listOf(testTicketId), prevStatus)

    // Check adding and removing release
    val existingVersionId = getVersionIdByName("TEST_VERSION")
    if (existingVersionId != null) {
      deleteVersionById(existingVersionId)
    }
    val versionId = addReleaseToYoutrack("TEST_VERSION")
    guard(getVersionIdByName("TEST_VERSION") != null) { "Test version isn't created" }
    setYoutrackStatus(listOf(testTicketId), "Fixed")
    setYoutrackFixVersion(listOf(testTicketId), "TEST_VERSION")
    deleteVersionById(versionId)
    setYoutrackStatus(listOf(testTicketId), "Open")
    guard(getVersionIdByName("TEST_VERSION") == null) { "Test version isn't deleted" }

    updateMergedPr(525)
    // TODO: test Ticket parsing
    // TODO: test Update CHANGES
    // TODO: test Update AUTHORS
    // TODO: test Slack notification
    // TODO: Add a comment on EAP release
  }
}

fun guard(check: Boolean, ifWrong: () -> String) {
  if (!check) {
    error(ifWrong())
  }
}

tasks.register("testUpdateChangelog") {
  group = "verification"
  description = "This is a task to manually assert the correctness of the update tasks"
  doLast {
    val changesFile = File("$projectDir/CHANGES.md")
    val changes = changesFile.readText()

    val changesBuilder = StringBuilder(changes)
    val insertOffset = setupSection(changes, changesBuilder, "### Changes:")

    changesBuilder.insert(insertOffset, "--Hello--\n")

    changesFile.writeText(changesBuilder.toString())
  }
}

fun addReleaseToYoutrack(name: String): String {
  val client = httpClient()
  println("Creating new release version in YouTrack: $name")

  return runBlocking {
    val response =
      client.post("https://youtrack.jetbrains.com/api/admin/projects/$vimProjectId/customFields/$fixVersionsFieldId/bundle/values?fields=id,name") {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        val request = buildJsonObject {
          put("name", name)
          put("\$type", fixVersionsElementType)
        }
        setBody(request)
      }
    response.body<JsonObject>().getValue("id").jsonPrimitive.content
  }
}

fun getVersionIdByName(name: String): String? {
  val client = httpClient()

  return runBlocking {
    val response =
      client.get("https://youtrack.jetbrains.com/api/admin/projects/$vimProjectId/customFields/$fixVersionsFieldId/bundle/values?fields=id,name&query=$name")
    response.body<JsonArray>().singleOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
  }
}

fun deleteVersionById(id: String) {
  val client = httpClient()

  runBlocking {
    client.delete("https://youtrack.jetbrains.com/api/admin/projects/$vimProjectId/customFields/$fixVersionsFieldId/bundle/values/$id")
  }
}

fun updateYoutrackOnCommit() {
  println("Start updating youtrack")
  println(projectDir)

  val newFixes = changes()
  val newTickets = newFixes.map { it.id }
  println("Set new status for $newTickets")
  setYoutrackStatus(newTickets, "Ready To Release")
}

fun getYoutrackTicketsByQuery(query: String): Set<String> {
  val client = httpClient()

  return runBlocking {
    val response = client.get("https://youtrack.jetbrains.com/api/issues/?fields=idReadable&query=project:VIM+$query")
    response.body<JsonArray>().mapTo(HashSet()) { it.jsonObject.getValue("idReadable").jsonPrimitive.content }
  }
}

fun setYoutrackStatus(tickets: Collection<String>, status: String) {
  val client = httpClient()

  runBlocking {
    for (ticket in tickets) {
      println("Try to set $ticket to $status")
      val response =
        client.post("https://youtrack.jetbrains.com/api/issues/$ticket?fields=customFields(id,name,value(id,name))") {
          contentType(ContentType.Application.Json)
          accept(ContentType.Application.Json)
          val request = buildJsonObject {
            putJsonArray("customFields") {
              addJsonObject {
                put("name", "State")
                put("\$type", "SingleEnumIssueCustomField")
                putJsonObject("value") {
                  put("name", status)
                }
              }
            }
          }
          setBody(request)
        }
      println(response)
      println(response.body<String>())
      if (!response.status.isSuccess()) {
        error("Request failed. $ticket, ${response.body<String>()}")
      }
      val finalState = response.body<JsonObject>()["customFields"]!!.jsonArray
        .single { it.jsonObject["name"]!!.jsonPrimitive.content == "State" }
        .jsonObject["value"]!!
        .jsonObject["name"]!!
        .jsonPrimitive.content
      if (finalState != status) {
        error("Ticket $ticket is not updated! Expected status $status, but actually $finalState")
      }
    }
  }
}

fun setYoutrackFixVersion(tickets: Collection<String>, version: String) {
  val client = httpClient()

  runBlocking {
    for (ticket in tickets) {
      println("Try to set fix version $version for $ticket")
      val response =
        client.post("https://youtrack.jetbrains.com/api/issues/$ticket?fields=customFields(id,name,value(id,name))") {
          contentType(ContentType.Application.Json)
          accept(ContentType.Application.Json)
          val request = buildJsonObject {
            putJsonArray("customFields") {
              addJsonObject {
                put("name", "Fix versions")
                put("\$type", "MultiVersionIssueCustomField")
                putJsonArray("value") {
                  addJsonObject { put("name", version) }
                }
              }
            }
          }
          setBody(request)
        }
      println(response)
      println(response.body<String>())
      if (!response.status.isSuccess()) {
        error("Request failed. $ticket, ${response.body<String>()}")
      }
      val finalState = response.body<JsonObject>()["customFields"]!!.jsonArray
        .single { it.jsonObject["name"]!!.jsonPrimitive.content == "Fix versions" }
        .jsonObject["value"]!!
        .jsonArray[0]
        .jsonObject["name"]!!
        .jsonPrimitive.content
      if (finalState != version) {
        error("Ticket $ticket is not updated! Expected fix version $version, but actually $finalState")
      }
    }
  }
}

fun getYoutrackStatus(ticket: String): String {
  val client = httpClient()

  return runBlocking {
    val response =
      client.get("https://youtrack.jetbrains.com/api/issues/$ticket/customFields/123-129?fields=value(name)")
    response.body<JsonObject>()["value"]!!.jsonObject.getValue("name").jsonPrimitive.content
  }
}

fun updateChangelog() {
  println("Start update authors")
  println(projectDir)
  val newFixes = changes()

  // Update changes file
  val changesFile = File("$projectDir/CHANGES.md")
  val changes = changesFile.readText()

  val changesBuilder = StringBuilder(changes)
  val insertOffset = setupSection(changes, changesBuilder, "### Fixes:")

  if (insertOffset < 50) error("Incorrect offset: $insertOffset")

  val firstPartOfChanges = changes.take(insertOffset)
  val actualFixes = newFixes
    .filterNot { it.id in firstPartOfChanges }
  val newUpdates = actualFixes
    .joinToString("") { "* [${it.id}](https://youtrack.jetbrains.com/issue/${it.id}) ${it.text}\n" }

  changesBuilder.insert(insertOffset, newUpdates)
  if (actualFixes.isNotEmpty()) {
    changesFile.writeText(changesBuilder.toString())
  }
}

fun updateAuthors(uncheckedEmails: Set<String>) {
  println("Start update authors")
  println(projectDir)
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)
  val lastSuccessfulCommit = System.getenv("SUCCESS_COMMIT")!!
  val hashesAndEmailes = git.log().call()
    .takeWhile {
      !it.id.name.equals(lastSuccessfulCommit, ignoreCase = true)
    }
    .associate { it.authorIdent.emailAddress to it.name }

  println("Last successful commit: $lastSuccessfulCommit")
  println("Amount of commits: ${hashesAndEmailes.size}")
  println("Emails: ${hashesAndEmailes.keys}")
  val gitHub = org.kohsuke.github.GitHub.connect()
  val ghRepository = gitHub.getRepository("JetBrains/ideavim")
  val users = mutableSetOf<Author>()
  println("Start emails processing")
  for ((email, hash) in hashesAndEmailes) {
    println("Processing '$email'...")
    if (email in uncheckedEmails) {
      println("Email '$email' is in unchecked emails. Skip it")
      continue
    }
    if ("dependabot[bot]@users.noreply.github.com" in email) {
      println("Email '$email' is from dependabot. Skip it")
      continue
    }
    if ("tcuser" in email) {
      println("Email '$email' is from teamcity. Skip it")
      continue
    }
    val user: GHUser? = ghRepository.getCommit(hash).author
    if (user == null) {
      println("Cant get the commit author. Email: $email. Commit: $hash")
      continue
    }
    val htmlUrl = user.htmlUrl.toString()
    val name = user.name ?: user.login
    users.add(Author(name, htmlUrl, email))
  }

  println("Emails processed")
  val authorsFile = File("$projectDir/AUTHORS.md")
  val authors = authorsFile.readText()
  val parser =
    org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
  val tree = parser.buildMarkdownTreeFromString(authors)

  val contributorsSection = tree.children[24]
  val existingEmails = mutableSetOf<String>()
  for (child in contributorsSection.children) {
    if (child.children.size > 1) {
      existingEmails.add(
        child.children[1].children[0].children[2].children[2].getTextInNode(authors).toString(),
      )
    }
  }

  val newAuthors = users.filterNot { it.mail in existingEmails }
  if (newAuthors.isEmpty()) return

  val authorNames = newAuthors.joinToString(", ") { it.name }
  println("::set-output name=authors::$authorNames")

  val insertionString = newAuthors.toMdString()
  val resultingString = StringBuffer(authors).insert(contributorsSection.endOffset, insertionString).toString()

  authorsFile.writeText(resultingString)
}

fun List<Author>.toMdString(): String {
  return this.joinToString(separator = "") {
    """
          |
          |* [![icon][mail]](mailto:${it.mail})
          |  [![icon][github]](${it.url})
          |  &nbsp;
          |  ${it.name}
        """.trimMargin()
  }
}

data class Author(val name: String, val url: String, val mail: String)
data class Change(val id: String, val text: String)

fun updateMergedPr(number: Int) {
  val token = System.getenv("GITHUB_OAUTH")
  println("Token size: ${token.length}")
  val gitHub = org.kohsuke.github.GitHubBuilder().withOAuthToken(token).build()
  println("Connecting to the repo...")
  val repository = gitHub.getRepository("JetBrains/ideavim")
  println("Getting pull requests...")
  val pullRequest = repository.getPullRequest(number)
  if (pullRequest.user.login == "dependabot[bot]") return

  val changesFile = File("$projectDir/CHANGES.md")
  val changes = changesFile.readText()

  val changesBuilder = StringBuilder(changes)
  val insertOffset = setupSection(changes, changesBuilder, "### Merged PRs:")

  if (insertOffset < 50) error("Incorrect offset: $insertOffset")
  if (pullRequest.user.login == "dependabot[bot]") return

  val prNumber = pullRequest.number
  val userName = pullRequest.user.name ?: pullRequest.user.login
  val login = pullRequest.user.login
  val title = pullRequest.title
  val section =
    "* [$prNumber](https://github.com/JetBrains/ideavim/pull/$prNumber) by [$userName](https://github.com/$login): $title\n"
  changesBuilder.insert(insertOffset, section)

  changesFile.writeText(changesBuilder.toString())
}

fun setupSection(
  changes: String,
  authorsBuilder: StringBuilder,
  sectionName: String,
): Int {
  val parser =
    org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
  val tree = parser.buildMarkdownTreeFromString(changes)

  var idx = -1
  for (index in tree.children.indices) {
    if (tree.children[index].getTextInNode(changes).startsWith("## ")) {
      idx = index
      break
    }
  }

  val hasToBeReleased = tree.children[idx].getTextInNode(changes).contains("To Be Released")
  return if (hasToBeReleased) {
    var mrgIdx = -1
    for (index in (idx + 1) until tree.children.lastIndex) {
      val textInNode = tree.children[index].getTextInNode(changes)
      val foundIndex = textInNode.startsWith(sectionName)
      if (foundIndex) {
        var filledPr = index + 2
        while (tree.children[filledPr].getTextInNode(changes).startsWith("*")) {
          filledPr++
        }
        mrgIdx = tree.children[filledPr].startOffset + 1
        break
      } else {
        val currentSectionIndex = sections.indexOf(sectionName)
        val insertHere = textInNode.startsWith("## ") ||
                textInNode.startsWith("### ") &&
                sections.indexOfFirst { textInNode.startsWith(it) }
                  .let { if (it < 0) false else it > currentSectionIndex }
        if (insertHere) {
          val section = """
                        $sectionName
                        
                        
                    """.trimIndent()
          authorsBuilder.insert(tree.children[index].startOffset, section)
          mrgIdx = tree.children[index].startOffset + (section.length - 1)
          break
        }
      }
    }
    mrgIdx
  } else {
    val section = """
            ## To Be Released
            
            $sectionName
            
            
        """.trimIndent()
    authorsBuilder.insert(tree.children[idx].startOffset, section)
    tree.children[idx].startOffset + (section.length - 1)
  }
}

val sections = listOf(
  "### Features:",
  "### Changes:",
  "### Fixes:",
  "### Merged PRs:",
)

fun changes(): List<Change> {
  val repository = RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
  val git = Git(repository)
  val lastSuccessfulCommit = System.getenv("SUCCESS_COMMIT")!!
  val messages = git.log().call()
    .takeWhile {
      !it.id.name.equals(lastSuccessfulCommit, ignoreCase = true)
    }
    .map { it.shortMessage }

  // Collect fixes
  val newFixes = mutableListOf<Change>()
  println("Last successful commit: $lastSuccessfulCommit")
  println("Amount of commits: ${messages.size}")
  println("Start changes processing")
  for (message in messages) {
    println("Processing '$message'...")
    val lowercaseMessage = message.lowercase()
    val regex = "^fix\\((vim-\\d+)\\):".toRegex()
    val findResult = regex.find(lowercaseMessage)
    if (findResult != null) {
      println("Message matches")
      val value = findResult.groups[1]!!.value.uppercase()
      val shortMessage = message.drop(findResult.range.last + 1).trim()
      newFixes += Change(value, shortMessage)
    } else {
      println("Message doesn't match")
    }
  }
  return newFixes
}

fun httpClient(): HttpClient {
  return HttpClient(CIO) {
    expectSuccess = true
    install(Auth) {
      bearer {
        loadTokens {
          val accessToken = youtrackToken.ifBlank { System.getenv("YOUTRACK_TOKEN")!! }
          BearerTokens(accessToken, "")
        }
      }
    }
    install(ContentNegotiation) {
      json(
        Json {
          prettyPrint = true
          isLenient = true
        },
      )
    }
  }
}
