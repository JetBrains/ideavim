/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

/** One entry of the generated ex-command table, written by `ExCommandProcessor` */
@Serializable
private data class ExCommandEntry(
  val className: String,
  val barSeparates: Boolean = true,
  val delimitedSections: Int = 0,
)

interface ExCommandProvider {
  val exCommandsFileName: String

  @OptIn(ExperimentalSerializationApi::class)
  fun getCommands(): Map<String, LazyExCommandInstance> {
    val classLoader = this.javaClass.classLoader
    val commandToEntry: Map<String, ExCommandEntry> = Json.decodeFromStream(getFile())
    return commandToEntry.entries.associate { (command, entry) ->
      command to LazyExCommandInstance(entry.className, classLoader, entry.barSeparates, entry.delimitedSections)
    }
  }

  private fun getFile(): InputStream {
    return this.javaClass.classLoader.getResourceAsStream("ksp-generated/$exCommandsFileName")
      ?: throw RuntimeException("Failed to fetch ex-commands for ${javaClass.name}")
  }
}
