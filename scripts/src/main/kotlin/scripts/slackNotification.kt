/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Calls Claude Code CLI with the given prompt and returns the output.
 */
fun callClaudeCode(prompt: String): String {
  val process = ProcessBuilder("claude", "-p", prompt)
    .redirectErrorStream(true)
    .start()

  val output = process.inputStream.bufferedReader().readText()
  val exitCode = process.waitFor()

  if (exitCode != 0) {
    error("Claude Code failed with exit code $exitCode: $output")
  }

  return output.trim()
}

/**
 * Builds the prompt for generating a Slack message from changelog.
 */
fun buildSlackMessagePrompt(version: String, changelog: String): String = """
Generate a Slack message JSON for IdeaVim $version release.

RULES:
1. Output ONLY valid JSON - no markdown code blocks, no explanations, no extra text
2. Use Slack mrkdwn format: *bold*, _italic_, <url|text> for links
3. Structure: { "text": "...", "blocks": [...] }
4. Keep it concise - summarize if changelog is long
5. Include version announcement

Changelog:
$changelog
""".trimIndent()

/**
 * Builds the prompt for fixing a Slack message that caused an error.
 */
fun buildFixPrompt(originalMessage: String, slackError: String): String = """
The following Slack message JSON caused an error when sent to Slack.

ERROR: $slackError

MESSAGE:
$originalMessage

Fix the JSON. Output ONLY the corrected JSON, nothing else.
""".trimIndent()

/**
 * Sends a message to Slack with retry logic.
 * On error, asks Claude Code to fix the message and retries.
 */
suspend fun sendToSlackWithRetry(
  client: HttpClient,
  slackUrl: String,
  initialMessage: String,
  maxAttempts: Int = 3
): Boolean {
  var currentMessage = initialMessage
  var lastError: String? = null

  repeat(maxAttempts) { attempt ->
    println("Attempt ${attempt + 1} of $maxAttempts")

    if (lastError != null) {
      println("Asking Claude Code to fix the message...")
      currentMessage = callClaudeCode(buildFixPrompt(currentMessage, lastError!!))
      println("Claude provided fixed message")
    }

    val response = client.post(slackUrl) {
      contentType(ContentType.Application.Json)
      setBody(currentMessage)
    }

    val responseBody = response.body<String>()

    if (response.status.isSuccess() && responseBody == "ok") {
      println("Message sent successfully!")
      return true
    }

    lastError = "HTTP ${response.status.value}: $responseBody"
    println("Slack error: $lastError")
  }

  println("Failed to send message after $maxAttempts attempts")
  return false
}

fun main(args: Array<String>) {
  val version = args.getOrNull(0) ?: error("Version not provided")
  val slackUrl = args.getOrNull(1) ?: ""
  val changesFile = args.getOrNull(2) ?: error("Changes file path not provided")
  val dryRun = args.getOrNull(3)?.toBoolean() ?: false

  if (!dryRun && version.last() != '0') {
    println("Skipping Slack notification for non-release version: $version")
    return
  }

  if (!dryRun && slackUrl.isBlank()) {
    println("Slack URL is not defined")
    return
  }

  val changeLog = extractLatestChangelog(File(changesFile))
  println("Extracted changelog:\n$changeLog\n")

  // Generate message with Claude Code
  val prompt = buildSlackMessagePrompt(version, changeLog)
  println("Generating Slack message with Claude Code...")
  val slackMessage = callClaudeCode(prompt)
  println("Generated message:\n$slackMessage\n")

  if (dryRun) {
    println("Dry run mode - not sending to Slack")
    return
  }

  runBlocking {
    val client = HttpClient(CIO)
    try {
      val success = sendToSlackWithRetry(client, slackUrl, slackMessage)
      if (!success) {
        error("Failed to send Slack notification after all retry attempts")
      }
    } catch (e: Exception) {
      println("Error sending Slack notification: ${e.message}")
      throw e
    } finally {
      client.close()
    }
  }
}

fun extractLatestChangelog(changesFile: File): String {
  val content = changesFile.readText()
  val lines = content.lines()
  
  // Find the first "## " header (version header)
  val startIdx = lines.indexOfFirst { it.startsWith("## ") && !it.contains("Unreleased") }
  if (startIdx == -1) return ""
  
  // Find the next "## " header (next version)
  val endIdx = lines.drop(startIdx + 1).indexOfFirst { it.startsWith("## ") }
  
  return if (endIdx == -1) {
    lines.drop(startIdx).joinToString("\n")
  } else {
    lines.subList(startIdx, startIdx + endIdx + 1).joinToString("\n")
  }
}
