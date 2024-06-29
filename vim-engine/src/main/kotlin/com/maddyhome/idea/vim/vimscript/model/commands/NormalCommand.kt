/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

// todo make it for each caret
@ExCommand(command = "norm[al]")
data class NormalCommand(val range: Range, val argument: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(
    RangeFlag.RANGE_OPTIONAL,
    ArgumentFlag.ARGUMENT_OPTIONAL,
    Access.WRITABLE,
    Flag.SAVE_VISUAL,
  )

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    var useMappings = true
    var argument = argument
    if (argument.startsWith("!")) {
      useMappings = false
      argument = argument.substring(1)
    }

    val rangeUsed = range.size() != 0
    when (editor.mode) {
      is Mode.VISUAL -> {
        editor.exitVisualMode()
        if (!rangeUsed) {
          val selectionStart = injector.markService.getMark(editor.primaryCaret(), VimMarkService.SELECTION_START_MARK)!!
          editor.currentCaret().moveToBufferPosition(BufferPosition(selectionStart.line, selectionStart.col))
        }
      }
      is Mode.CMD_LINE -> injector.processGroup.cancelExEntry(editor, refocusOwningEditor = true, resetCaret = false)
      Mode.INSERT, Mode.REPLACE -> editor.exitInsertMode(context, OperatorArguments(false, 1, editor.mode))
      is Mode.SELECT -> editor.exitSelectModeNative(false)
      is Mode.OP_PENDING, is Mode.NORMAL -> Unit
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
        keyHandler.handleKey(editor, key, context, useMappings, true, keyHandler.keyHandlerState)
      }

      // Exit if state leaves as insert or cmd_line
      val mode = editor.mode
      if (mode is Mode.CMD_LINE) {
        injector.processGroup.cancelExEntry(editor, refocusOwningEditor = true, resetCaret = false)
      }
      if (mode is Mode.INSERT || mode is Mode.REPLACE) {
        editor.exitInsertMode(context, OperatorArguments(false, 1, mode))
      }
    }

    return ExecutionResult.Success
  }
}
