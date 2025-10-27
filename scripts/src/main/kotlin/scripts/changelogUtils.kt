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
import java.io.File

data class Change(val id: String, val text: String)

fun changes(projectDir: File): List<Change> {
  val repository = RepositoryBuilder().setGitDir(File(projectDir, ".git")).build()
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

val sections = listOf(
  "### Features:",
  "### Changes:",
  "### Fixes:",
  "### Merged PRs:",
)

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
