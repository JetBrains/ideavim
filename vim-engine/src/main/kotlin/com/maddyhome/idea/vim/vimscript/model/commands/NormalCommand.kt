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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

data class NormalCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.WRITABLE,
    Flag.SAVE_VISUAL
  )

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.ideadelaymacroName)) {
      return ExecutionResult.Success
    }

    var useMappings = true
    var argument = argument
    if (argument.startsWith("!")) {
      useMappings = false
      argument = argument.substring(1)
    }

    val commandState = editor.vimStateMachine
    val rangeUsed = ranges.size() != 0
    when (editor.mode) {
      VimStateMachine.Mode.VISUAL -> {
        editor.exitVisualModeNative()
        if (!rangeUsed) {
          val selectionStart = injector.markGroup.getMark(editor, '<')!!
          editor.currentCaret().moveToLogicalPosition(VimLogicalPosition(selectionStart.logicalLine, selectionStart.col))
        }
      }
      VimStateMachine.Mode.CMD_LINE -> injector.processGroup.cancelExEntry(editor, false)
      VimStateMachine.Mode.INSERT, VimStateMachine.Mode.REPLACE -> editor.exitInsertMode(context, OperatorArguments(false, 1, commandState.mode, commandState.subMode))
      VimStateMachine.Mode.SELECT -> editor.exitSelectModeNative(false)
      VimStateMachine.Mode.OP_PENDING, VimStateMachine.Mode.COMMAND -> Unit
      VimStateMachine.Mode.INSERT_NORMAL -> Unit
      VimStateMachine.Mode.INSERT_VISUAL -> Unit
      VimStateMachine.Mode.INSERT_SELECT -> Unit
    }
    val range = getLineRange(editor, editor.primaryCaret())

    for (line in range.startLine..range.endLine) {
      if (rangeUsed) {
        // Move caret to the first position on line
        if (editor.lineCount() < line) {
          break
        }
        val startOffset = editor.getLineStartOffset(line)
        editor.currentCaret().moveToOffset(startOffset)
      }

      // Perform operations
      val keys = injector.parser.stringToKeys(argument)
      val keyHandler = KeyHandler.getInstance()
      keyHandler.reset(editor)
      for (key in keys) {
        keyHandler.handleKey(editor, key, context, useMappings, true)
      }

      // Exit if state leaves as insert or cmd_line
      val mode = commandState.mode
      if (mode == VimStateMachine.Mode.CMD_LINE) {
        injector.processGroup.cancelExEntry(editor, false)
      }
      if (mode == VimStateMachine.Mode.INSERT || mode == VimStateMachine.Mode.REPLACE) {
        editor.exitInsertMode(context, OperatorArguments(false, 1, commandState.mode, commandState.subMode))
      }
    }

    return ExecutionResult.Success
  }
}
