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
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray


val vimProjectId = "22-43"

// It was a bit complicated to get this field. Usually I used a request for custom fields on ticket, but this field
//  was not presented there. So, instead I've just loaded the page with the ticket and investigated the loaded resources
//  using chrome toolkit
val areaFieldId = "123-5386"

suspend fun getAreaValues(): Map<String, String> {
  val client = httpClient()
  return client.get("admin/projects/$vimProjectId/customFields/$areaFieldId/bundle/values?fields=id,name")
    .body<JsonArray>()
    .map { it.jsonObject }
    .associate { element ->
      val elementId = element["id"]
      val elementName = element["name"]
      if (elementId == null || elementName == null) error("Can't parse $element")
      elementId.jsonPrimitive.content to elementName.jsonPrimitive.content
    }
}

suspend fun issuesQuery(query: String = "", fields: String = ""): HttpResponse {
  val client = httpClient()
  val res = client.get("issues") {
    parameter("query", query)
    parameter("fields", fields)
  }
  return res
}

suspend fun updateCustomField(issueId: String, setField: JsonObjectBuilder.() -> Unit): HttpResponse {
  val client = httpClient()
  return client.post("issues/$issueId") {
    contentType(ContentType.Application.Json)
    accept(ContentType.Application.Json)
    val request = buildJsonObject {
      putJsonArray("customFields") {
        addJsonObject {
          setField()
        }
      }
    }
    setBody(request)
  }
}


suspend fun main() {
  println(getAreaValues())
}
