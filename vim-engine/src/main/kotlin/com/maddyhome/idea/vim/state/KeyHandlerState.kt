/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingState
import com.maddyhome.idea.vim.common.DigraphSequence
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.state.mode.Mode

data class KeyHandlerState(
  val mappingState: MappingState,
  val digraphSequence: DigraphSequence,
  val editorCommandBuilder: CommandBuilder,
  var commandLineCommandBuilder: CommandBuilder?,
): Cloneable {
  constructor() : this(MappingState(), DigraphSequence(), CommandBuilder(injector.keyGroup.getKeyRoot(MappingMode.NORMAL)), null)

  companion object {
    private val logger = vimLogger<KeyHandlerState>()
  }

  val commandBuilder: CommandBuilder
    get() = commandLineCommandBuilder ?: editorCommandBuilder

  fun enterCommandLine() {
    // Create a new command builder for the command line, so we can handle nested commands inside the command line.
    // The command that starts the command line is added to the new command builder and immediately executed, opening
    // the command line UI.
    // When we match the command that accepts or cancels the command line, we remove this nested command builder, and
    // that command is added to the editor command builder, immediately completed and executed.
    // The user might already have entered some state in the editor command builder, specifically uncommitted count.
    // E.g., the user might have typed `3:` expecting the command line to be displayed with `:.,.+2` as initial text.
    // We do not reset the uncommitted count in the editor command builder. The Ex actions ignore it, preferring the
    // range in the text command. The search actions use it, and it will be combined with an operator count as expected.
    // E.g., `2d3/foo` will delete up to the 6th occurrence of `foo`
    commandLineCommandBuilder = CommandBuilder(injector.keyGroup.getKeyRoot(MappingMode.CMD_LINE), editorCommandBuilder.count)
  }

  fun leaveCommandLine() {
    commandLineCommandBuilder = null
  }

  fun partialReset(mode: Mode) {
    logger.trace("entered partialReset. mode: $mode")
    mappingState.resetMappingSequence()
    commandBuilder.resetInProgressCommandPart(injector.keyGroup.getKeyRoot(mode.toMappingMode()))
  }

  fun reset(mode: Mode) {
    logger.trace("entered reset. mode: $mode")
    digraphSequence.reset()
    mappingState.resetMappingSequence()

    commandLineCommandBuilder = null
    editorCommandBuilder.resetAll(injector.keyGroup.getKeyRoot(mode.toMappingMode()))
  }

  public override fun clone(): KeyHandlerState {
    return KeyHandlerState(
      mappingState.clone(),
      digraphSequence.clone(),
      editorCommandBuilder.clone(),
      commandLineCommandBuilder?.clone(),
    )
  }

  override fun toString(): String {
    return """
      KeyHandlerState:
      Mapping state:
      $mappingState
      Digraph sequence:
      $digraphSequence
      Editor command builder:
      $editorCommandBuilder
      Command line command builder:
      $commandLineCommandBuilder
      """.trimMargin()
  }
}
