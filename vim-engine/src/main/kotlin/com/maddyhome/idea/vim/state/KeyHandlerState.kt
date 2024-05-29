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
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.state.mode.Mode

public data class KeyHandlerState(
  public val mappingState: MappingState,
  public val digraphSequence: DigraphSequence,
  public val editorCommandBuilder: CommandBuilder,
  public var commandLineCommandBuilder: CommandBuilder?,
): Cloneable {
  public constructor() : this(MappingState(), DigraphSequence(), CommandBuilder(injector.keyGroup.getKeyRoot(MappingMode.NORMAL)), null)

  public val commandBuilder: CommandBuilder
    get() = commandLineCommandBuilder ?: editorCommandBuilder

  public fun enterCommandLine() {
    commandLineCommandBuilder = CommandBuilder(injector.keyGroup.getKeyRoot(MappingMode.CMD_LINE))
  }

  public fun leaveCommandLine() {
    commandLineCommandBuilder = null
  }

  public fun partialReset(mode: Mode) {
    mappingState.resetMappingSequence()
    commandBuilder.resetInProgressCommandPart(injector.keyGroup.getKeyRoot(mode.toMappingMode()))
  }

  public fun reset(mode: Mode) {
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
}