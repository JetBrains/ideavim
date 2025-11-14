/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import dev.feedforward.markdownto.DownParser
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

fun main(args: Array<String>) {
  val version = args.getOrNull(0) ?: error("Version not provided")
  val slackUrl = args.getOrNull(1) ?: ""
  val changesFile = args.getOrNull(2) ?: error("Changes file path not provided")
  
  if (version.last() != '0') {
    println("Skipping Slack notification for non-release version: $version")
    return
  }
  
  if (slackUrl.isBlank()) {
    println("Slack URL is not defined")
    return
  }
  
  val changeLog = extractLatestChangelog(File(changesFile))
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
  
  println("Parsed data: $slackDown")
  
  runBlocking {
    val client = HttpClient(CIO)
    try {
      val response = client.post(slackUrl) {
        contentType(ContentType.Application.Json)
        setBody(message)
      }
      
      val responseCode = response.status.value
      println("Response code: $responseCode")
      
      val responseBody = response.body<String>()
      println(responseBody)
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
