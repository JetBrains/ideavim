/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import org.intellij.markdown.ast.getTextInNode
import java.io.File

fun main(args: Array<String>) {
  val prNumber = args.getOrNull(0)?.toIntOrNull() ?: error("PR number not provided")
  val projectDir = if (args.size > 1) File(args[1]) else File(".")
  
  updateMergedPr(prNumber, projectDir)
}

fun updateMergedPr(number: Int, projectDir: File) {
  val token = System.getenv("GITHUB_OAUTH")
  println("Token size: ${token.length}")
  val gitHub = org.kohsuke.github.GitHubBuilder().withOAuthToken(token).build()
  println("Connecting to the repo...")
  val repository = gitHub.getRepository("JetBrains/ideavim")
  println("Getting pull requests...")
  val pullRequest = repository.getPullRequest(number)
  if (pullRequest.user.login == "dependabot[bot]") return

  val changesFile = File(projectDir, "CHANGES.md")
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
