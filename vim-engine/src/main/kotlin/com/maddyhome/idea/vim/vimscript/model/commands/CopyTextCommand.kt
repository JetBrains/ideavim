/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :copy"
 */
data class CopyTextCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val carets = editor.sortedCarets()
    for (caret in carets) {
      val range = getTextRange(editor, caret, false)
      val text = editor.getText(range)

      val goToLineCommand = injector.vimscriptParser.parseCommand(argument) ?: throw ExException("E16: Invalid range")
      val line = goToLineCommand.commandRanges.getFirstLine(editor, caret)

      val transferableData = injector.clipboardManager.getTransferableData(editor, range, text)
      val textData = PutData.TextData(text, SelectionType.LINE_WISE, transferableData)
      val putData = PutData(
        textData,
        null,
        1,
        insertTextBeforeCaret = false,
        rawIndent = true,
        caretAfterInsertedText = false,
        putToLine = line
      )
      injector.put.putTextForCaret(editor, caret, context, putData)
    }
    return ExecutionResult.Success
  }
}
