/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.vim

/**
 * COMPATIBILITY-LAYER: Additional class
 */
class CommandState(private val machine: VimStateMachine) {

  val isOperatorPending: Boolean
    get() = machine.isOperatorPending

  val mode: CommandState.Mode
    get() = machine.mode.ij

  enum class Mode {
    // Basic modes
    COMMAND, VISUAL, SELECT, INSERT, CMD_LINE, /*EX*/

    // Additional modes
    OP_PENDING, REPLACE /*, VISUAL_REPLACE*/, INSERT_NORMAL, INSERT_VISUAL, INSERT_SELECT
  }

  enum class SubMode {
    NONE, VISUAL_CHARACTER, VISUAL_LINE, VISUAL_BLOCK
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): CommandState {
      return CommandState(editor.vim.vimStateMachine)
    }
  }
}

val CommandState.SubMode.engine: VimStateMachine.SubMode
  get() = when (this) {
    CommandState.SubMode.NONE -> VimStateMachine.SubMode.NONE
    CommandState.SubMode.VISUAL_CHARACTER -> VimStateMachine.SubMode.VISUAL_CHARACTER
    CommandState.SubMode.VISUAL_LINE -> VimStateMachine.SubMode.VISUAL_LINE
    CommandState.SubMode.VISUAL_BLOCK -> VimStateMachine.SubMode.VISUAL_BLOCK
  }

val CommandState.Mode.engine: VimStateMachine.Mode
  get() = when (this) {
    CommandState.Mode.COMMAND -> VimStateMachine.Mode.COMMAND
    CommandState.Mode.VISUAL -> VimStateMachine.Mode.VISUAL
    CommandState.Mode.SELECT -> VimStateMachine.Mode.SELECT
    CommandState.Mode.INSERT -> VimStateMachine.Mode.INSERT
    CommandState.Mode.CMD_LINE -> VimStateMachine.Mode.CMD_LINE
    CommandState.Mode.OP_PENDING -> VimStateMachine.Mode.OP_PENDING
    CommandState.Mode.REPLACE -> VimStateMachine.Mode.REPLACE
    CommandState.Mode.INSERT_NORMAL -> VimStateMachine.Mode.INSERT_NORMAL
    CommandState.Mode.INSERT_VISUAL -> VimStateMachine.Mode.INSERT_VISUAL
    CommandState.Mode.INSERT_SELECT -> VimStateMachine.Mode.INSERT_SELECT
  }

val VimStateMachine.Mode.ij: CommandState.Mode
  get() = when (this) {
    VimStateMachine.Mode.COMMAND -> CommandState.Mode.COMMAND
    VimStateMachine.Mode.VISUAL -> CommandState.Mode.VISUAL
    VimStateMachine.Mode.SELECT -> CommandState.Mode.SELECT
    VimStateMachine.Mode.INSERT -> CommandState.Mode.INSERT
    VimStateMachine.Mode.CMD_LINE -> CommandState.Mode.CMD_LINE
    VimStateMachine.Mode.OP_PENDING -> CommandState.Mode.OP_PENDING
    VimStateMachine.Mode.REPLACE -> CommandState.Mode.REPLACE
    VimStateMachine.Mode.INSERT_NORMAL -> CommandState.Mode.INSERT_NORMAL
    VimStateMachine.Mode.INSERT_VISUAL -> CommandState.Mode.INSERT_VISUAL
    VimStateMachine.Mode.INSERT_SELECT -> CommandState.Mode.INSERT_SELECT
  }
