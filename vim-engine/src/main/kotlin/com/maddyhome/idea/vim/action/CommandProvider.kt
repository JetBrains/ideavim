/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import com.intellij.vim.processors.CommandOrMotionProcessor
import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

public interface CommandProvider {
  public val exCommandListFileName: String

  @OptIn(ExperimentalSerializationApi::class)
  public fun getCommands(): Collection<LazyVimCommand> {
    val classLoader = this.javaClass.classLoader
    val commands: List<CommandOrMotionProcessor.CommandBean> = Json.decodeFromStream(getFile())
    return commands
      .groupBy { it.`class` }
      .map {
        val keys = it.value.map { bean -> injector.parser.parseKeys(bean.keys) }.toSet()
        val modes = it.value.first().modes.map { mode -> MappingMode.parseModeChar(mode) }.toSet()
        LazyVimCommand(
          keys,
          modes,
          it.key,
          classLoader
        )
    }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream(exCommandListFileName)
      ?: throw RuntimeException("Failed to fetch ex commands from ${javaClass.name}")
  }

}