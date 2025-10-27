/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File

data class Change(val id: String, val text: String)

fun main(args: Array<String>) {
  val projectDir = if (args.isNotEmpty()) File(args[0]) else File(".")
  
  println("Start updating youtrack")
  println("Project directory: $projectDir")

  val newFixes = changes(projectDir)
  val newTickets = newFixes.map { it.id }
  println("Set new status for $newTickets")
  
  runBlocking {
    setYoutrackStatus(newTickets, "Ready To Release")
  }
}

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
