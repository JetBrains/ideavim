/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import io.ktor.client.call.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

val areaWeights = setOf(
  Triple("118-53212", "Plugins", 50),
  Triple("118-53220", "Vim Script", 30),
  Triple("118-54084", "Esc", 100),
)

suspend fun updateRates() {
  println("Updating rates of the issues")
  areaWeights.forEach { (id, name, weight) ->
    val unmappedIssues = unmappedIssues(name)
    println("Got ${unmappedIssues.size} for $name area")

    unmappedIssues.forEach { issueId ->
      print("Trying to update issue $issueId: ")
      val response = updateCustomField(issueId) {
        put("name", "Affected Rate")
        put("\$type", "SimpleIssueCustomField")
        put("value", weight)
      }

      println(response)
    }
  }
}

private suspend fun unmappedIssues(area: String): List<String> {
  val areaProcessed = if (" " in area) "{$area}" else area
  val res = issuesQuery(
    query = "project: VIM Affected Rate: {No affected rate} Area: $areaProcessed #Unresolved",
    fields = "id,idReadable"
  )
  return res.body<JsonArray>().map { it.jsonObject }.map { it["idReadable"]!!.jsonPrimitive.content }
}

suspend fun getAreasWithoutWeight(): Set<Pair<String, String>> {
  val allAreas = getAreaValues()
  return allAreas
    .filterNot { it.key in areaWeights.map { it.first }.toSet() }
    .entries
    .map { it.key to it.value }
    .toSet()
}

suspend fun main() {
  updateRates()
}
