/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.kohsuke.github.GHUser
import java.io.File

data class Author(val name: String, val url: String, val mail: String) {
  val isJetBrainsEmployee: Boolean
    get() = mail.endsWith("@jetbrains.com")
}

private const val CONTRIBUTORS_HEADER = "Contributors:"
private const val JETBRAINS_IP_HEADER = "Contributors with JetBrains IP:"
private const val PREVIOUS_CONTRIBUTORS_HEADER = "Previous contributors:"

fun main(args: Array<String>) {
  val projectDir = if (args.isNotEmpty()) File(args[0]) else File(".")

  val uncheckedEmails = setOf(
    "aleksei.plate@jetbrains.com",
    "aleksei.plate@teamcity",
    "aleksei.plate@TeamCity",
    "alex.plate@192.168.0.109",
    "nikita.koshcheev@TeamCity",
    "TeamCity@TeamCity",
  )

  updateAuthors(projectDir, uncheckedEmails)
}

fun updateAuthors(projectDir: File, uncheckedEmails: Set<String>) {
  println("Start update authors")
  println(projectDir)
  val repository = RepositoryBuilder().setGitDir(File(projectDir, ".git")).build()
  val git = Git(repository)
  val lastSuccessfulCommit = System.getenv("SUCCESS_COMMIT")!!
  val hashesAndEmails = git.log().call()
    .takeWhile {
      !it.id.name.equals(lastSuccessfulCommit, ignoreCase = true)
    }
    .associate { it.authorIdent.emailAddress to it.name }

  println("Last successful commit: $lastSuccessfulCommit")
  println("Amount of commits: ${hashesAndEmails.size}")
  println("Emails: ${hashesAndEmails.keys}")
  val gitHub = org.kohsuke.github.GitHub.connect()
  val ghRepository = gitHub.getRepository("JetBrains/ideavim")
  val users = mutableSetOf<Author>()
  println("Start emails processing")
  for ((email, hash) in hashesAndEmails) {
    println("Processing '$email'...")
    if (email in uncheckedEmails) {
      println("Email '$email' is in unchecked emails. Skip it")
      continue
    }
    if ("[bot]@users.noreply.github.com" in email) {
      println("Email '$email' is from a bot. Skip it")
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
  val authorsFile = File(projectDir, "AUTHORS.md")
  val authorsContent = authorsFile.readText()

  val result = addAuthorsToContent(authorsContent, users)
  if (result.newAuthors.isNotEmpty()) {
    val authorNames = result.newAuthors.joinToString(", ") { it.name }
    println("::set-output name=authors::$authorNames")
    authorsFile.writeText(result.content)
  }
}

data class AddAuthorsResult(
  val content: String,
  val newAuthors: List<Author>,
)

/**
 * Adds authors to the AUTHORS.md content.
 * - JetBrains employees (emails ending with @jetbrains.com) are added to "Contributors with JetBrains IP:" section
 * - Other contributors are added to "Contributors:" section
 * - Authors already present in either section (by email or GitHub URL) are skipped
 */
fun addAuthorsToContent(authorsContent: String, authors: Collection<Author>): AddAuthorsResult {
  val existingEmails = extractExistingEmails(authorsContent)
  val existingGitHubUrls = extractExistingGitHubUrls(authorsContent)

  val newAuthors = authors.filterNot { it.mail in existingEmails || it.url in existingGitHubUrls }
  if (newAuthors.isEmpty()) {
    return AddAuthorsResult(authorsContent, emptyList())
  }

  val jetBrainsAuthors = newAuthors.filter { it.isJetBrainsEmployee }
  val regularAuthors = newAuthors.filterNot { it.isJetBrainsEmployee }

  var result = authorsContent

  // Add JetBrains employees to JetBrains IP section
  if (jetBrainsAuthors.isNotEmpty()) {
    val insertionPoint = findSectionEndOffset(result, JETBRAINS_IP_HEADER, PREVIOUS_CONTRIBUTORS_HEADER)
    if (insertionPoint != -1) {
      val insertionString = jetBrainsAuthors.toMdString(isJetBrainsSection = true)
      result = StringBuilder(result).insert(insertionPoint, insertionString).toString()
    }
  }

  // Add regular contributors to Contributors section
  if (regularAuthors.isNotEmpty()) {
    val insertionPoint = findSectionEndOffset(result, CONTRIBUTORS_HEADER, JETBRAINS_IP_HEADER)
    if (insertionPoint != -1) {
      val insertionString = regularAuthors.toMdString(isJetBrainsSection = false)
      result = StringBuilder(result).insert(insertionPoint, insertionString).toString()
    }
  }

  return AddAuthorsResult(result, newAuthors)
}

/**
 * Extracts all email addresses from the AUTHORS.md content.
 * Emails are found in mailto: links.
 */
fun extractExistingEmails(content: String): Set<String> {
  return Regex("""mailto:([^)]+)\)""")
    .findAll(content)
    .map { it.groupValues[1] }
    .toSet()
}

/**
 * Extracts all GitHub profile URLs from the AUTHORS.md content.
 */
fun extractExistingGitHubUrls(content: String): Set<String> {
  return Regex("""\[!\[icon]\[github]]\((https://github\.com/[^)]+)\)""")
    .findAll(content)
    .map { it.groupValues[1] }
    .toSet()
}

/**
 * Finds the end offset of a section (where new entries should be inserted).
 * The section starts with [sectionHeader] and ends before [nextSectionHeader].
 * Returns the position just before the blank line preceding the next section header.
 */
fun findSectionEndOffset(content: String, sectionHeader: String, nextSectionHeader: String): Int {
  val sectionStart = content.indexOf(sectionHeader)
  if (sectionStart == -1) return -1

  val nextSectionStart = content.indexOf(nextSectionHeader, sectionStart)
  if (nextSectionStart == -1) return -1

  // Find the last non-blank line before the next section
  // We want to insert before the blank lines that precede the next section header
  var insertionPoint = nextSectionStart
  while (insertionPoint > sectionStart && content[insertionPoint - 1] == '\n') {
    insertionPoint--
  }

  return insertionPoint
}

fun List<Author>.toMdString(isJetBrainsSection: Boolean): String {
  return this.joinToString(separator = "") { author ->
    val nameWithNote = if (isJetBrainsSection) "${author.name} (JetBrains employee)" else author.name
    """
          |
          |* [![icon][mail]](mailto:${author.mail})
          |  [![icon][github]](${author.url})
          |  &nbsp;
          |  $nameWithNote
        """.trimMargin()
  }
}
