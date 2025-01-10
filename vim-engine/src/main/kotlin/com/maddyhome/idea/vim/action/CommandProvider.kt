/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

/**
 * An interface defining the contract for providers responsible for reading and parsing JSON files.
 * These files contain a list of command beans that are intended to be lazily loaded during runtime.
 * The primary functionality of this interface is to transform the JSON data into a collection of
 * {@code LazyVimCommand} instances.
 */
interface CommandProvider {
  val commandListFileName: String

  @OptIn(ExperimentalSerializationApi::class)
  fun getCommands(): Collection<LazyVimCommand> {
    val classLoader = this.javaClass.classLoader
    val commands: List<CommandBean> = Json.decodeFromStream(getFile())
    return commands
      .groupBy { it.`class` }
      .map {
        val keys = it.value.map { bean -> injector.parser.parseKeys(bean.keys) }.toSet()
        val modes = it.value.first().modes.map { mode -> MappingMode.parseModeChar(mode) }.toSet()
        LazyVimCommand(keys, modes, it.key, classLoader)
      }
  }

  private fun getFile(): InputStream {
    return object {}.javaClass.classLoader.getResourceAsStream("ksp-generated/$commandListFileName")
      ?: throw RuntimeException("Failed to fetch ex commands from ${javaClass.name}")
  }
}

@Serializable
data class CommandBean(val keys: String, val `class`: String, val modes: String)
