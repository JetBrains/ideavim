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
) : Cloneable {
  constructor() : this(
    MappingState(),
    DigraphSequence(),
    CommandBuilder(injector.keyGroup.getBuiltinCommandsTrie(MappingMode.NORMAL)),
    null
  )

  companion object {
    private val logger = vimLogger<KeyHandlerState>()
  }

  val commandBuilder: CommandBuilder
    get() = commandLineCommandBuilder ?: editorCommandBuilder

  fun enterCommandLine() {
    // Create a new command builder for commands entered inside the command line, which allows for nested commands, such
    // as inserting a digraph while entering a search pattern as a delete motion - `d/foo<C-K>OK`.
    // When matching an action that opens a command line, the new builder is created before the action is added to a
    // builder, which means it gets added to the new command line builder. Since there are no arguments required by the
    // action, the command is completed and executed (and the command line builder reset), and the command line UI is
    // opened.
    // When matching an action that accepts or cancels a command line, the command line builder is removed before the
    // action is added, so it is added to the editor's command line builder. The key handler recognises the action to
    // accept the command line, and will add the command line contents as an argument, and then execute the command.
    // The user might have already entered some state in the editor command builder, specifically uncommitted count.
    // E.g., the user might have typed `3:` expecting the command line to be displayed with `:.,.+2` as initial text, or
    // started something like `2d3/foo` to delete up to the 6th occurrence of `foo`. (In both examples, the uncommitted
    // count is `3`.)
    // We pass the current, in progress count for the editor command builder to the new command line builder, where it
    // becomes the count for the new action. The `:` handler will transform it into a range, while the search handler
    // will ignore it.
    // In other words, the `:` handler uses the count eagerly, where it becomes part of the command line that is
    // executed (by the editor command builder) when the command line UI is closed. But the search handler doesn't use
    // it - it's needed by the search motion action executed by the editor command builder once the command line UI is
    // accepted. For this reason, we do NOT clear the uncommitted count from the editor command builder. A command such
    // as `2d3/foo` becomes a delete operator action with a search action motion argument, which itself has an Ex string
    // argument with the search string. The command has a count of `6`. And a command such as `3:p` becomes an action to
    // process Ex entry with an argument of `.,.+2p` and a count of 3. The count is ignored by this action.
    // Note that we use the calculated count. In Vim, `2"a3"b:` transforms to `:.,.+5`, which is the same behaviour
    commandLineCommandBuilder = CommandBuilder(
      injector.keyGroup.getBuiltinCommandsTrie(MappingMode.CMD_LINE),
      editorCommandBuilder.calculateCount0Snapshot()
    )
  }

  fun leaveCommandLine() {
    commandLineCommandBuilder = null
  }

  fun partialReset(mode: Mode) {
    logger.trace("entered partialReset. mode: $mode")
    mappingState.resetMappingSequence()
    commandBuilder.resetCommandTrie(injector.keyGroup.getBuiltinCommandsTrie(mode.toMappingMode()))
  }

  fun reset(mode: Mode) {
    logger.trace("entered reset. mode: $mode")
    digraphSequence.reset()
    mappingState.resetMappingSequence()

    commandLineCommandBuilder = null
    editorCommandBuilder.resetAll(injector.keyGroup.getBuiltinCommandsTrie(mode.toMappingMode()))
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
