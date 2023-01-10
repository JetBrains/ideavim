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
)

suspend fun main() {
  val response = client.get("https://plugins.jetbrains.com/api/plugins/") {
    parameter("dependency", "IdeaVIM")
    parameter("includeOptional", true)
  }
  val output = response.body<List<String>>()
  println(output)
  if (knownPlugins != output) error("Unknown plugins list: $output")
}
