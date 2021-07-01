
import dev.feedforward.markdownto.DownParser
import org.intellij.markdown.ast.getTextInNode
import java.net.HttpURLConnection
import java.net.URL

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.0")
        classpath("com.github.AlexPl292:mark-down-to-slack:1.1.2")
        classpath("org.eclipse.jgit:org.eclipse.jgit:5.12.0.202106070339-r")
        classpath("org.kohsuke:github-api:1.129")
        classpath("org.jetbrains:markdown:0.2.4")
    }
}

plugins {
    java
    kotlin("jvm") version "1.5.0"

    id("org.jetbrains.intellij") version "1.0"
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
val publishToken: String by project

val slackUrl: String by project

repositories {
    mavenCentral()
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compileOnly("org.jetbrains:annotations:21.0.1")

    // https://mvnrepository.com/artifact/com.ensarsarajcic.neovim.java/neovim-api
    testImplementation("com.ensarsarajcic.neovim.java:neovim-api:0.2.3")
    testImplementation("com.ensarsarajcic.neovim.java:core-rpc:0.2.3")

    testImplementation("com.intellij.remoterobot:remote-robot:0.11.5")
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
            apiVersion = "1.3"
//            allWarningsAsErrors = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = javaVersion
            apiVersion = "1.3"
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
    version.set(ideaVersion)
    pluginName.set("IdeaVim")
    updateSinceUntilBuild.set(false)
    downloadSources.set(downloadIdeaSources.toBoolean())
    instrumentCode.set(instrumentPluginCode.toBoolean())
    intellijRepository.set("https://www.jetbrains.com/intellij-repository")
    plugins.set(listOf("java"))
}

tasks {
    downloadRobotServerPlugin {
        version.set("0.10.0")
    }

    publishPlugin {
        channels.set(publishChannels.split(","))
        token.set(publishToken)
    }

    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
    }

    runPluginVerifier {
        ideVersions.set(listOf("IC-2020.2.3", "IC-2020.3.2", "IC-2021.1"))
        downloadDir.set("${project.buildDir}/pluginVerifier/ides")
        teamCityOutputFormat.set(true)
    }
}

// --- Linting

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
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "IdeaVim $version has been released\n$slackDown"
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

// --- Update authors

tasks.register("updateAuthors") {
    doLast {
        val uncheckedEmails = setOf(
            "aleksei.plate@jetbrains.com",
            "aleksei.plate@teamcity",
            "aleksei.plate@TeamCity",
            "alex.plate@192.168.0.109"
        )
        updateAuthors(uncheckedEmails)
    }
}

val prId: String by project

tasks.register("updateMergedPr") {
    doLast {
        if (project.hasProperty("prId")) {
            updateMergedPr(prId.toInt())
        } else {
            error("Cannot get prId")
        }
    }
}

tasks.register("updateChangelog") {
    doLast {
        updateChangelog()
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

fun updateChangelog() {
    println("Start update authors")
    println(projectDir)
    val repository = org.eclipse.jgit.lib.RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
    val git = org.eclipse.jgit.api.Git(repository)
    val messages = git.log().call().take(40).map { it.shortMessage }

    // Collect fixes
    val newFixes = mutableListOf<Change>()
    println("Start emails processing")
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

    // Update changes file
    val changesFile = File("$projectDir/CHANGES.md")
    val changes = changesFile.readText()

    val changesBuilder = StringBuilder(changes)
    val insertOffset = setupSection(changes, changesBuilder, "### Fixes:")

    if (insertOffset < 50) error("Incorrect offset: $insertOffset")

    val firstPartOfChanges = changes.take(insertOffset)
    val newUpdates = newFixes
        .filterNot { it.id in firstPartOfChanges }
        // Temporally disable this example
        .filterNot { it.id == "VIM-123" }
        .joinToString { "* [${it.id}](https://youtrack.jetbrains.com/issue/${it.id}) ${it.text}\n" }

    changesBuilder.insert(insertOffset, newUpdates)
    changesFile.writeText(changesBuilder.toString())
}

fun updateAuthors(uncheckedEmails: Set<String>) {
    println("Start update authors")
    println(projectDir)
    val repository = org.eclipse.jgit.lib.RepositoryBuilder().setGitDir(File("$projectDir/.git")).build()
    val git = org.eclipse.jgit.api.Git(repository)
    val hashesAndEmailes = git.log().call().take(40).mapTo(HashSet()) { it.name to it.authorIdent.emailAddress }

    println("Emails: ${hashesAndEmailes.map { it.second }}")
    val gitHub = org.kohsuke.github.GitHub.connect()
    val ghRepository = gitHub.getRepository("JetBrains/ideavim")
    val users = mutableSetOf<Author>()
    println("Start emails processing")
    for ((hash, email) in hashesAndEmailes) {
        println("Processing '$email'...")
        if (email in uncheckedEmails) {
            println("Email '$email' is in unchecked emails. Skip it")
            continue
        }
        if ("dependabot[bot]@users.noreply.github.com" in email) {
            println("Email '$email' is from dependabot. Skip it")
            continue
        }
        val user = ghRepository.getCommit(hash).author
        val htmlUrl = user.htmlUrl.toString()
        val name = user.name
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
                child.children[1].children[0].children[2].children[2].getTextInNode(authors).toString()
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
    val gitHub = org.kohsuke.github.GitHub.connect()
    val repository = gitHub.getRepository("JetBrains/ideavim")
    val pullRequest = repository.getPullRequest(number)
    if (pullRequest.user.login == "dependabot[bot]") return

    val changesFile = File("$projectDir/CHANGES.md")
    val changes = changesFile.readText()

    val changesBuilder = StringBuilder(changes)
    val insertOffset = setupSection(changes, changesBuilder, "### Merged PRs:")

    if (insertOffset < 50) error("Incorrect offset: $insertOffset")
    if (pullRequest.user.login == "dependabot[bot]") return

    val prNumber = pullRequest.number
    val userName = pullRequest.user.name
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
