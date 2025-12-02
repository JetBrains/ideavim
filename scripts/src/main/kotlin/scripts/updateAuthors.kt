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
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.impl.ListCompositeNode
import org.kohsuke.github.GHUser
import java.io.File

data class Author(val name: String, val url: String, val mail: String)

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
  val authors = authorsFile.readText()
  val parser =
    org.intellij.markdown.parser.MarkdownParser(org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor())
  val tree = parser.buildMarkdownTreeFromString(authors)

  val contributorsSection = tree.children
    .filter { it is ListCompositeNode }
    .single { it.getTextInNode(authors).contains("yole") }
  val existingEmails = mutableSetOf<String>()
  for (child in contributorsSection.children) {
    if (child.children.size > 1) {
      existingEmails.add(
        child.children[1].children[0].children[2].children[2].getTextInNode(authors).toString(),
      )
    }
  }

  // Also extract existing GitHub URLs to prevent duplicates from different emails
  val existingGitHubUrls = Regex("""\[!\[icon]\[github]]\((https://github\.com/[^)]+)\)""")
    .findAll(authors)
    .map { it.groupValues[1] }
    .toSet()

  val newAuthors = users.filterNot { it.mail in existingEmails || it.url in existingGitHubUrls }
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
