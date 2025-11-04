/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

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
