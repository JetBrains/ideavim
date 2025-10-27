/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

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
import org.intellij.markdown.ast.getTextInNode
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
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
    classpath("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

    // This is needed for jgit to connect to ssh
    classpath("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:7.3.0.202506031305-r")
    classpath("org.kohsuke:github-api:1.305")

    classpath("io.ktor:ktor-client-core:3.3.0")
    classpath("io.ktor:ktor-client-cio:3.3.0")
    classpath("io.ktor:ktor-client-auth:3.3.0")
    classpath("io.ktor:ktor-client-content-negotiation:3.3.0")
    classpath("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

    // This comes from the changelog plugin
//        classpath("org.jetbrains:markdown:0.3.1")
  }
}

plugins {
  java
  kotlin("jvm") version "2.2.0"
  application
  id("java-test-fixtures")

  // NOTE: Unignore "test block comment falls back to line comment when not available" test
  //   After changing this version. It supposed to work on the next version of the gradle plugin
  //   Or go report to the devs that this test still fails.
  id("org.jetbrains.intellij.platform") version "2.9.0"

  id("org.jetbrains.changelog") version "2.4.0"
  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("com.dorongold.task-tree") version "4.0.1"
  id("com.google.devtools.ksp") version "2.2.0-2.0.2"
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

    // VERSION UPDATE: This module is required since 2025.2
    if (ideaVersion == "LATEST-EAP-SNAPSHOT") {
      bundledModule("intellij.spellchecker")
    }
    if (ideaVersion.startsWith("2025.2")) {
      bundledModule("intellij.spellchecker")
    }
    if (ideaVersion.startsWith("2025.3")) {
      bundledModule("intellij.spellchecker")
    }
  }

  moduleSources(project(":vim-engine", "sourcesJarArtifacts"))

  // --------- Test dependencies ----------

  testApi("com.squareup.okhttp3:okhttp:5.0.0")

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
  testImplementation("org.junit.vintage:junit-vintage-engine:6.0.0")
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
        We’ve launched a program to reward quality contributions with a one-year All Products Pack subscription. Learn more at: <a href="https://github.com/JetBrains/ideavim/blob/master/CONTRIBUTING.md">CONTRIBUTING.md</a> .
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

// --- Kover

koverMerged {
  enable()
}

// Uncomment to enable FUS testing mode
// tasks {
//    withType<org.jetbrains.intellij.tasks.RunIdeTask> {
//        jvmArgs("-Didea.is.internal=true")
//        jvmArgs("-Dfus.internal.test.mode=true")
//    }
// }

val vimProjectId = "22-43"
val fixVersionsFieldId = "123-285"
val fixVersionsFieldType = "VersionProjectCustomField"
val fixVersionsElementType = "VersionBundleElement"

tasks.register<Task>("releaseActions") {
  group = "other"
  doLast {
    if (releaseType == "patch") return@doLast

    val tickets =
      getYoutrackTicketsByQuery("%23%7BReady+To+Release%7D%20and%20tag:%20%7BIdeaVim%20Released%20In%20EAP%7D%20")
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

tasks.register<Task>("integrationsTest") {
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

    // TODO: Update to call the script version
    // updateMergedPr(525)
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

tasks.register<Task>("testUpdateChangelog") {
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







// Shared utility functions (also used in scripts project)
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
