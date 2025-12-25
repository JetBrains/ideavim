/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.youtrackAnalysis

import kotlinx.coroutines.runBlocking
import scripts.addComment
import scripts.claudeAnalyzedTagId
import scripts.setTag
import java.io.File

/**
 * Completes the YouTrack ticket analysis by tagging and optionally commenting.
 *
 * Arguments:
 * - args[0]: ticket ID (e.g., "VIM-1234")
 * - args[1]: analysis result ("suitable", "unsuitable", or "error")
 * - args[2]: PR URL (optional, only present if a PR was created)
 *
 * Actions:
 * - Always tags the ticket with "claude-analyzed"
 * - Adds a comment only if result is "suitable" or "error"
 */
fun main(args: Array<String>) = runBlocking {
  val ticketId = args.getOrNull(0)
  val analysisResult = args.getOrNull(1) ?: "unknown"
  val prUrl = args.getOrNull(2)?.takeIf { it.isNotBlank() }

  if (ticketId.isNullOrBlank()) {
    println("No ticket ID provided, skipping completion")
    return@runBlocking
  }

  println("Completing analysis for ticket: $ticketId")
  println("Analysis result: $analysisResult")
  if (prUrl != null) {
    println("PR URL: $prUrl")
  }

  // Always tag the ticket to exclude from future analysis runs
  println("Tagging ticket with 'claude-analyzed'...")
  setTag(ticketId, claudeAnalyzedTagId)
  println("Ticket tagged successfully")

  // Only add comment if suitable (PR created) or error occurred
  // For "unsuitable" tickets, we just tag without commenting
  if (analysisResult != "unsuitable") {
    val comment = buildComment(analysisResult, prUrl)
    println("Adding comment to ticket...")
    addComment(ticketId, comment)
    println("Comment added successfully")
  } else {
    println("Ticket marked as unsuitable, skipping comment")
  }

  println("Analysis completion finished for $ticketId")
}

private fun buildComment(analysisResult: String, prUrl: String?): String {
  return buildString {
    appendLine("**Claude Code Automated Analysis**")
    appendLine()

    when (analysisResult) {
      "suitable" -> {
        appendLine("This ticket was analyzed and determined to be suitable for automated fixing.")
        if (prUrl != null) {
          appendLine()
          appendLine("A pull request has been created: $prUrl")
        }
      }
      "error" -> {
        appendLine("An error occurred during automated analysis of this ticket.")
        appendLine("Manual review may be required.")
      }
      else -> {
        appendLine("Analysis completed with result: $analysisResult")
      }
    }

    appendLine()
    appendLine("---")
    appendLine("*This comment was generated automatically by the YouTrack Auto-Analysis workflow.*")
  }
}
