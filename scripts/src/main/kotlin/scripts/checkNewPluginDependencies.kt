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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Marketplace has an API to get all plugins that depend on our plugin.
 * Here we have a list of dependent plugins at some moment, and we check if something changed in that.
 * If so, we need to update our list of plugins.
 *
 * This script makes no actions and aimed to notify the devs in case they need to update the list of IdeaVim plugins.
 */

@Suppress("SpellCheckingInspection")
val knownPlugins = listOf(
  "IdeaVimExtension",
  "github.zgqq.intellij-enhance",
  "org.jetbrains.IdeaVim-EasyMotion",
  "io.github.mishkun.ideavimsneak",
  "eu.theblob42.idea.whichkey",
  "com.github.copilot",
  "com.github.dankinsoid.multicursor",
  "com.joshestein.ideavim-quickscope",
  "ca.alexgirard.HarpoonIJ",

//   "cc.implicated.intellij.plugins.bunny", // I don't want to include this plugin in the list of IdeaVim plugins as I don't understand what this is for
)

suspend fun main() {
  val response = client.get("https://plugins.jetbrains.com/api/plugins/") {
    parameter("dependency", "IdeaVIM")
    parameter("includeOptional", true)
  }
  val output = response.body<List<String>>()
  println(output)
  if (knownPlugins != output) {
    val newPlugins = (output - knownPlugins).map { it to (getPluginLinkByXmlId(it) ?: "Can't find plugin link") }
    val removedPlugins = (knownPlugins - output.toSet()).map {
      it to (getPluginLinkByXmlId(it) ?: "Can't find plugin link")
    }
    error(
      """
        
      Unregistered plugins:
      ${if (newPlugins.isNotEmpty()) newPlugins.joinToString(separator = "\n") { it.first + "(" + it.second + ")" } else "No unregistered plugins"}
      
      Removed plugins:
      ${if (removedPlugins.isNotEmpty()) removedPlugins.joinToString(separator = "\n") { it.first + "(" + it.second + ")" } else "No removed plugins"}
    """.trimIndent()
    )
  }
}

private suspend fun getPluginLinkByXmlId(it: String): String? {
  val newPluginLink = client.get("https://plugins.jetbrains.com/api/plugins/intellij/$it")
    .body<JsonObject>()["link"]?.jsonPrimitive?.content
  return newPluginLink?.let { "https://plugins.jetbrains.com$it" }
}
