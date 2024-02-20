/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * COMPATIBILITY-LAYER: Additional class
 * Please see: https://jb.gg/zo8n0r
 */
public class CommandState(private val machine: VimStateMachine) {

  public val isOperatorPending: Boolean
    get() = machine.isOperatorPending

  public val mode: Mode
    get() {
      val myMode = machine.mode
      return when (myMode) {
        is com.maddyhome.idea.vim.state.mode.Mode.CMD_LINE -> Mode.CMD_LINE
        com.maddyhome.idea.vim.state.mode.Mode.INSERT -> Mode.INSERT
        is com.maddyhome.idea.vim.state.mode.Mode.NORMAL -> Mode.COMMAND
        is com.maddyhome.idea.vim.state.mode.Mode.OP_PENDING -> Mode.OP_PENDING
        com.maddyhome.idea.vim.state.mode.Mode.REPLACE -> Mode.REPLACE
        is com.maddyhome.idea.vim.state.mode.Mode.SELECT -> Mode.SELECT
        is com.maddyhome.idea.vim.state.mode.Mode.VISUAL -> Mode.VISUAL
      }
    }

  public val commandBuilder: CommandBuilder
    get() = machine.commandBuilder

  public val mappingState: MappingState
    get() = machine.mappingState

  public enum class Mode {
    // Basic modes
    COMMAND, VISUAL, SELECT, INSERT, CMD_LINE, /*EX*/

    // Additional modes
    OP_PENDING, REPLACE /*, VISUAL_REPLACE*/, INSERT_NORMAL, INSERT_VISUAL, INSERT_SELECT
  }

  public enum class SubMode {
    NONE, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  public companion object {
    @JvmStatic
    public fun getInstance(editor: Editor): CommandState {
      return CommandState(editor.vim.vimStateMachine)
    }
  }
}

internal val CommandState.SubMode.engine: SelectionType
  get() = when (this) {
    CommandState.SubMode.NONE -> error("Unexpected value")
    CommandState.SubMode.VISUAL_CHARACTER -> SelectionType.CHARACTER_WISE
    CommandState.SubMode.VISUAL_LINE -> SelectionType.LINE_WISE
    CommandState.SubMode.VISUAL_BLOCK -> SelectionType.BLOCK_WISE
  }
