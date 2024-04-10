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
import org.kohsuke.github.GHUser
import java.net.HttpURLConnection
import java.net.URL

buildscript {
  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
    classpath("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")

    // This is needed for jgit to connect to ssh
    classpath("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.9.0.202403050737-r")
    classpath("org.kohsuke:github-api:1.305")

    classpath("io.ktor:ktor-client-core:2.3.9")
    classpath("io.ktor:ktor-client-cio:2.3.9")
    classpath("io.ktor:ktor-client-auth:2.3.9")
    classpath("io.ktor:ktor-client-content-negotiation:2.3.9")
    classpath("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

    // This comes from the changelog plugin
//        classpath("org.jetbrains:markdown:0.3.1")
  }
}

plugins {
  antlr
  java
  kotlin("jvm") version "1.9.22"
  application
  id("java-test-fixtures")

  id("org.jetbrains.intellij") version "1.17.3"
  id("org.jetbrains.changelog") version "2.2.0"

  id("org.jetbrains.kotlinx.kover") version "0.6.1"
  id("com.dorongold.task-tree") version "3.0.0"

  id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

ksp {
  arg("generated_directory", "$projectDir/src/main/resources/ksp-generated")
  arg("vimscript_functions_file", "intellij_vimscript_functions.json")
  arg("ex_commands_file", "intellij_ex_commands.json")
  arg("commands_file", "intellij_commands.json")
}

afterEvaluate {
//  tasks.named("kspKotlin").configure { dependsOn("clean") }
  tasks.named("kspKotlin").configure { dependsOn("generateGrammarSource") }
  tasks.named("kspTestFixturesKotlin").configure { enabled = false }
  tasks.named("kspTestFixturesKotlin").configure { enabled = false }
  tasks.named("kspTestKotlin").configure { enabled = false }
}

// Import variables from gradle.properties file
val javaVersion: String by project
val kotlinVersion: String by project
val ideaVersion: String by project
val ideaType: String by project
val downloadIdeaSources: String by project
val instrumentPluginCode: String by project
val antlrVersion: String by project
val remoteRobotVersion: String by project

val publishChannels: String by project
val publishToken: String by project

val slackUrl: String by project
val youtrackToken: String by project

repositories {
  mavenCentral()
  maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

dependencies {
  api(project(":vim-engine"))
  ksp(project(":annotation-processors"))
  implementation(project(":annotation-processors"))

  compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
  compileOnly("org.jetbrains:annotations:24.1.0")
  runtimeOnly("org.antlr:antlr4-runtime:$antlrVersion")
  antlr("org.antlr:antlr4:$antlrVersion")

  // --------- Test dependencies ----------

  testImplementation(testFixtures(project(":")))

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
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
  testFixturesImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
}

configurations {
  runtimeClasspath {
    exclude(group = "org.antlr", module = "antlr4")
  }
}

tasks {
  test {
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
      freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
//            allWarningsAsErrors = true
    }
  }

  compileTestKotlin {
    kotlinOptions {
      jvmTarget = javaVersion
      apiVersion = "1.9"
//            allWarningsAsErrors = true
    }
  }

  downloadRobotServerPlugin {
    version.set(remoteRobotVersion)
  }

  runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")
    systemProperty("ide.show.tips.on.startup.default.value", "false")
    systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
  }

  runIde {
    systemProperty("octopus.handler", System.getProperty("octopus.handler") ?: true)
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(javaVersion))
  }
}

kotlin {
  explicitApi()
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

intellij {
  version.set(ideaVersion)
  type.set(ideaType)
  pluginName.set("IdeaVim")

  updateSinceUntilBuild.set(false)

  downloadSources.set(downloadIdeaSources.toBoolean())
  instrumentCode.set(instrumentPluginCode.toBoolean())
  intellijRepository.set("https://www.jetbrains.com/intellij-repository")
  plugins.set(listOf("AceJump:3.8.11"))
}

tasks {
  publishPlugin {
    channels.set(publishChannels.split(","))
    token.set(publishToken)
  }

  signPlugin {
    certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
    privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
    password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
  }

  runPluginVerifier {
    downloadDir.set("${project.buildDir}/pluginVerifier/ides")
    teamCityOutputFormat.set(true)
  }

  generateGrammarSource {
    maxHeapSize = "128m"
    arguments.addAll(listOf("-package", "com.maddyhome.idea.vim.vimscript.parser.generated", "-visitor"))
    outputDirectory = file("src/main/java/com/maddyhome/idea/vim/vimscript/parser/generated")
  }

  named("compileKotlin") {
    dependsOn("generateGrammarSource")
  }
  named("compileTestKotlin") {
    dependsOn("generateTestGrammarSource")
  }
  named("compileTestFixturesKotlin") {
    dependsOn("generateTestFixturesGrammarSource")
  }

  // Add plugin open API sources to the plugin ZIP
  val createOpenApiSourceJar by registering(Jar::class) {
    dependsOn("generateGrammarSource")
    // Java sources
    from(sourceSets.main.get().java) {
      include("**/com/maddyhome/idea/vim/**/*.java")
    }
    from(project(":vim-engine").sourceSets.main.get().java) {
      include("**/com/maddyhome/idea/vim/**/*.java")
    }
    // Kotlin sources
    from(kotlin.sourceSets.main.get().kotlin) {
      include("**/com/maddyhome/idea/vim/**/*.kt")
    }
    from(project(":vim-engine").kotlin.sourceSets.main.get().kotlin) {
      include("**/com/maddyhome/idea/vim/**/*.kt")
    }
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveClassifier.set("src")
  }

  buildPlugin {
    dependsOn(createOpenApiSourceJar)
    from(createOpenApiSourceJar) { into("lib/src") }
  }

  patchPluginXml {
    // Don't forget to update plugin.xml
    sinceBuild.set("233.11799.67")

    changeNotes.set(
      """<a href="https://youtrack.jetbrains.com/issues/VIM?q=State:%20Fixed%20Fix%20versions:%20${version.get()}">Changelog</a>"""
    )
  }
}

// --- Tests

tasks {
  test {
    useJUnitPlatform()
  }
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
  return this.joinToString {
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
    val lowercaseMessage = message.toLowerCase()
    val regex = "^fix\\((vim-\\d+)\\):".toRegex()
    val findResult = regex.find(lowercaseMessage)
    if (findResult != null) {
      println("Message matches")
      val value = findResult.groups[1]!!.value.toUpperCase()
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
