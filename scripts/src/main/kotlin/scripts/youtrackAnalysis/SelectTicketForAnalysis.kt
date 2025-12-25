/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.youtrackAnalysis

import kotlinx.coroutines.runBlocking
import scripts.claudeAnalyzedTagId
import scripts.getTicketDetails
import scripts.getYoutrackTicketsByQuery
import java.io.File

/**
 * Selects a random open YouTrack ticket for Claude analysis.
 *
 * Query: State is Open AND ticket is NOT tagged with "claude-analyzed"
 *
 * Outputs:
 * - ticket_details.md: Markdown file with ticket info for Claude to read
 * - GitHub Actions outputs: ticket_id, ticket_summary (via GITHUB_OUTPUT)
 */
fun main(args: Array<String>) = runBlocking {
  val projectDir = if (args.isNotEmpty()) File(args[0]) else File(".")

  println("Searching for open YouTrack tickets not yet analyzed by Claude...")

  // Query: Open state, excluding tickets with "claude-analyzed" tag
  // Note: YouTrack query syntax uses "tag: -{tagName}" to exclude a tag
  val query = "State: Open tag: -claude-analyzed"
  val tickets = getYoutrackTicketsByQuery(query)

  println("Found ${tickets.size} unanalyzed open tickets")

  if (tickets.isEmpty()) {
    println("No unanalyzed tickets found")
    writeGitHubOutput("ticket_id", "")
    writeGitHubOutput("ticket_summary", "")
    return@runBlocking
  }

  // Pick a random ticket
  val randomTicketId = tickets.random()
  println("Selected random ticket: $randomTicketId")

  // Fetch ticket details
  val details = getTicketDetails(randomTicketId)
  println("Ticket summary: ${details.summary}")
  println("Ticket state: ${details.state}")

  // Write ticket details to file for Claude to read
  val ticketDetailsFile = File(projectDir, "ticket_details.md")
  ticketDetailsFile.writeText(
    """
    |# YouTrack Ticket: ${details.id}
    |
    |## Summary
    |${details.summary}
    |
    |## Description
    |${details.description ?: "No description provided"}
    |
    |## Current State
    |${details.state}
    |
    |## URL
    |https://youtrack.jetbrains.com/issue/${details.id}
    """.trimMargin()
  )
  println("Wrote ticket details to ${ticketDetailsFile.absolutePath}")

  // Write GitHub Actions outputs
  writeGitHubOutput("ticket_id", details.id)
  writeGitHubOutput("ticket_summary", details.summary)
}

/**
 * Writes output to GitHub Actions environment file (GITHUB_OUTPUT).
 * Falls back to println for local testing.
 */
private fun writeGitHubOutput(name: String, value: String) {
  val githubOutput = System.getenv("GITHUB_OUTPUT")
  if (githubOutput != null) {
    File(githubOutput).appendText("$name=$value\n")
  } else {
    // Fallback for local testing
    println("OUTPUT: $name=$value")
  }
}
