/*
 * Copyright 2003-2026 The IdeaVim authors
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
    val (lookupCommands, normalCommands) = commands
      .groupBy { it.`class` }
      .map { (className, beans) ->
        val keys = beans.map { bean -> injector.parser.parseKeys(bean.keys) }.toSet()
        val modes = beans.first().modes.map { mode -> MappingMode.parseModeChar(mode) }.toSet()
        LazyVimCommand(keys, modes, className, classLoader, beans.first().lookup)
      }
      .partition { it.isLookupAction }
    return pairUpLookupVariants(normalCommands, lookupCommands)
  }

  /**
   * Hangs each `@CommandOrMotion(lookup = true)` handler off the normal command it shares its keys with, and returns
   * only the normal ones.
   *
   * One key sequence resolves to one trie node, so the pair has to travel as a single entry - see
   * [LazyVimCommand.lookupVariant]. A lookup handler with no counterpart is registered on its own, which is what
   * happens for a key that does nothing at all without a popup.
   */
  private fun pairUpLookupVariants(
    normalCommands: List<LazyVimCommand>,
    lookupCommands: List<LazyVimCommand>,
  ): Collection<LazyVimCommand> {
    if (lookupCommands.isEmpty()) return normalCommands
    val unpaired = lookupCommands.toMutableList()
    for (command in normalCommands) {
      val variant = unpaired.firstOrNull { it.keys == command.keys && it.modes == command.modes } ?: continue
      command.lookupVariant = variant
      unpaired.remove(variant)
    }
    return normalCommands + unpaired
  }

  private fun getFile(): InputStream {
    return this.javaClass.classLoader.getResourceAsStream("ksp-generated/$commandListFileName")
      ?: throw RuntimeException("Failed to fetch ex commands from ${javaClass.name}")
  }
}

@Serializable
data class CommandBean(val keys: String, val `class`: String, val modes: String, val lookup: Boolean = false)
