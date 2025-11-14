/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject


// YouTrack tag "IdeaVim Released In EAP"
const val releasedInEapTagId = "68-385032"

const val VIM_PROJECT_ID = "22-43"
const val FIX_VERSIONS_FIELD_ID = "123-285"
const val FIX_VERSIONS_ELEMENT_TYPE = "VersionBundleElement"

suspend fun setYoutrackStatus(tickets: Collection<String>, status: String) {
  val client = httpClient()

  for (ticket in tickets) {
    println("Try to set $ticket to $status")
    val response =
      // I've updated default url in client, so this may be broken now
      client.post("https://youtrack.jetbrains.com/api/issues/$ticket?fields=customFields(id,name,value(id,name))") {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        val request = buildJsonObject {
          putJsonArray("customFields") {
            addJsonObject {
              put("name", "State")
              put("\$type", "SingleEnumIssueCustomField")
              putJsonObject("value") {
                put("name", status)
              }
            }
          }
        }
        setBody(request)
      }
    println(response)
    println(response.body<String>())
    if (!response.status.isSuccess()) {
      error("Request failed. $ticket, ${response.body<String>()}")
    }
    val finalState = response.body<JsonObject>()["customFields"]!!.jsonArray
      .single { it.jsonObject["name"]!!.jsonPrimitive.content == "State" }
      .jsonObject["value"]!!
      .jsonObject["name"]!!
      .jsonPrimitive.content
    if (finalState != status) {
      error("Ticket $ticket is not updated! Expected status $status, but actually $finalState")
    }
  }
}

fun getYoutrackTicketsByQuery(query: String): Set<String> {
  val client = httpClient()

  return runBlocking {
    val response = client.get("https://youtrack.jetbrains.com/api/issues/?fields=idReadable&query=project:VIM+$query")
    response.body<JsonArray>().mapTo(HashSet()) { it.jsonObject.getValue("idReadable").jsonPrimitive.content }
  }
}

/**
 * 68-385032
 * [issueHumanId] is like VIM-123
 * [tagId] is like "145-23"
 */
suspend fun setTag(issueHumanId: String, tagId: String) {
  val client = httpClient()

  println("Try to add tag $tagId to $issueHumanId")
  val response =
    // I've updated default url in client, so this may be broken now
    client.post("https://youtrack.jetbrains.com/api/issues/$issueHumanId/tags?fields=customFields(id,name,value(id,name))") {
      contentType(ContentType.Application.Json)
      accept(ContentType.Application.Json)
      val request = buildJsonObject {
        put("id", tagId)
      }
      setBody(request)
    }
  println(response)
  println(response.body<String>())
  if (!response.status.isSuccess()) {
    error("Request failed. $issueHumanId, ${response.body<String>()}")
  }
}

suspend fun addComment(issueHumanId: String, text: String) {
  val client = httpClient()

  println("Try to add comment to $issueHumanId")
  val response =
    // I've updated default url in client, so this may be broken now
    client.post("https://youtrack.jetbrains.com/api/issues/$issueHumanId/comments?fields=customFields(id,name,value(id,name))") {
      contentType(ContentType.Application.Json)
      accept(ContentType.Application.Json)
      val request = buildJsonObject {
        put("text", text)
      }
      setBody(request)
    }
  println(response)
  println(response.body<String>())
  if (!response.status.isSuccess()) {
    error("Request failed. $issueHumanId, ${response.body<String>()}")
  }
}

suspend fun addReleaseToYoutrack(name: String): String {
  val client = httpClient()
  println("Creating new release version in YouTrack: $name")

  val response =
    client.post("https://youtrack.jetbrains.com/api/admin/projects/$VIM_PROJECT_ID/customFields/$FIX_VERSIONS_FIELD_ID/bundle/values?fields=id,name") {
      contentType(ContentType.Application.Json)
      accept(ContentType.Application.Json)
      val request = buildJsonObject {
        put("name", name)
        put("\$type", FIX_VERSIONS_ELEMENT_TYPE)
      }
      setBody(request)
    }
  return response.body<JsonObject>().getValue("id").jsonPrimitive.content
}

suspend fun getVersionIdByName(name: String): String? {
  val client = httpClient()

  val response =
    client.get("https://youtrack.jetbrains.com/api/admin/projects/$VIM_PROJECT_ID/customFields/$FIX_VERSIONS_FIELD_ID/bundle/values?fields=id,name&query=$name")
  return response.body<JsonArray>().singleOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
}

suspend fun deleteVersionById(id: String) {
  val client = httpClient()
  client.delete("https://youtrack.jetbrains.com/api/admin/projects/$VIM_PROJECT_ID/customFields/$FIX_VERSIONS_FIELD_ID/bundle/values/$id")
}

suspend fun setYoutrackFixVersion(tickets: Collection<String>, version: String) {
  val client = httpClient()

  for (ticket in tickets) {
    println("Try to set fix version $version for $ticket")
    val response =
      client.post("https://youtrack.jetbrains.com/api/issues/$ticket?fields=customFields(id,name,value(id,name))") {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        val request = buildJsonObject {
          putJsonArray("customFields") {
            addJsonObject {
              put("name", "Fix versions")
              put("\$type", "MultiVersionIssueCustomField")
              putJsonArray("value") {
                addJsonObject { put("name", version) }
              }
            }
          }
        }
        setBody(request)
      }
    println(response)
    println(response.body<String>())
    if (!response.status.isSuccess()) {
      error("Request failed. $ticket, ${response.body<String>()}")
    }
    val finalState = response.body<JsonObject>()["customFields"]!!.jsonArray
      .single { it.jsonObject["name"]!!.jsonPrimitive.content == "Fix versions" }
      .jsonObject["value"]!!
      .jsonArray[0]
      .jsonObject["name"]!!
      .jsonPrimitive.content
    if (finalState != version) {
      error("Ticket $ticket is not updated! Expected fix version $version, but actually $finalState")
    }
  }
}

suspend fun getYoutrackStatus(ticket: String): String {
  val client = httpClient()
  val response =
    client.get("https://youtrack.jetbrains.com/api/issues/$ticket/customFields/123-129?fields=value(name)")
  return response.body<JsonObject>()["value"]!!.jsonObject.getValue("name").jsonPrimitive.content
}
