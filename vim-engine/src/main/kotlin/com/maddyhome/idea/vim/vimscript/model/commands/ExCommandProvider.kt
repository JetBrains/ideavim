/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

interface ExCommandProvider {
  val exCommandsFileName: String

  @OptIn(ExperimentalSerializationApi::class)
  fun getCommands(): Map<String, LazyExCommandInstance> {
    val classLoader = this.javaClass.classLoader
    val commandToClass: Map<String, String> = Json.decodeFromStream(getFile())
    return commandToClass.entries.associate { it.key to LazyExCommandInstance(it.value, classLoader) }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream("ksp-generated/$exCommandsFileName")
      ?: throw RuntimeException("Failed to fetch ex-commands for ${javaClass.name}")
  }
}
