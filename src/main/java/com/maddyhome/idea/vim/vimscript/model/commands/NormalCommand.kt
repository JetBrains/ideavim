/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.exitInsertMode
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

data class NormalCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE, Flag.SAVE_VISUAL)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    var useMappings = true
    var argument = argument
    if (argument.startsWith("!")) {
      useMappings = false
      argument = argument.substring(1)
    }

    val commandState = CommandState.getInstance(editor)
    val rangeUsed = ranges.size() != 0
    when (editor.mode) {
      CommandState.Mode.VISUAL -> {
        editor.getTopLevelEditor().exitVisualMode()
        if (!rangeUsed) {
          val selectionStart = VimPlugin.getMark().getMark(editor, '<')!!
          editor.caretModel.moveToLogicalPosition(LogicalPosition(selectionStart.logicalLine, selectionStart.col))
        }
      }
      CommandState.Mode.CMD_LINE -> VimPlugin.getProcess().cancelExEntry(editor, false)
      CommandState.Mode.INSERT, CommandState.Mode.REPLACE -> editor.exitInsertMode(context, OperatorArguments(false, 1, commandState.mode, commandState.subMode))
      CommandState.Mode.SELECT -> editor.exitSelectMode(false)
      CommandState.Mode.OP_PENDING, CommandState.Mode.COMMAND -> Unit
    }
    val range = getLineRange(editor, editor.caretModel.primaryCaret)

    for (line in range.startLine..range.endLine) {
      if (rangeUsed) {
        // Move caret to the first position on line
        if (editor.document.lineCount < line) {
          break
        }
        val startOffset = EditorHelper.getLineStartOffset(editor, line)
        editor.caretModel.moveToOffset(startOffset)
      }

      // Perform operations
      val keys = stringToKeys(argument)
      val keyHandler = KeyHandler.getInstance()
      keyHandler.reset(editor)
      for (key in keys) {
        keyHandler.handleKey(editor, key, context, useMappings, true)
      }

      // Exit if state leaves as insert or cmd_line
      val mode = commandState.mode
      if (mode == CommandState.Mode.CMD_LINE) {
        VimPlugin.getProcess().cancelExEntry(editor, false)
      }
      if (mode == CommandState.Mode.INSERT || mode == CommandState.Mode.REPLACE) {
        editor.exitInsertMode(context, OperatorArguments(false, 1, commandState.mode, commandState.subMode))
      }
    }

    return ExecutionResult.Success
  }
}
