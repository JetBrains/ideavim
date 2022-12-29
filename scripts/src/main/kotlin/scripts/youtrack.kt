/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

suspend fun setYoutrackStatus(tickets: Collection<String>, status: String) {
  val client = httpClient()

  for (ticket in tickets) {
    println("Try to set $ticket to $status")
    val response =
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
