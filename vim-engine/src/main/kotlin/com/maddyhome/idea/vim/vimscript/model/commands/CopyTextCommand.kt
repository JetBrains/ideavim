/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeColumn
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :copy"
 */
@ExCommand(command = "t,co[py]")
data class CopyTextCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val carets = editor.sortedCarets()
    for (caret in carets) {
      val sourceLineRange = getLineRange(editor, caret)
      val range = sourceLineRange.toTextRange(editor)
      val copiedText = injector.clipboardManager.collectCopiedText(editor, context, range)

      // Copy is defined as:
      // :[range]co[py] {address}
      // Copy the given [range] to below the line given by {address}. Address can be a range, but only the first address
      // is used. The rest is ignored with no errors. Note that address is one-based, and 0 means copy the text to below
      // the line _before_ the first line (i.e., copy to above the first line).
      val address1 = getAddressFromArgument(editor)

      // Remember the current column to respect the 'startofline' option
      val caretColumn = caret.getBufferPosition().column

      val textData = PutData.TextData(null, copiedText, SelectionType.LINE_WISE)
      var mutableCaret = caret
      val putData = if (address1 == 0) {
        mutableCaret = mutableCaret.moveToOffset(0)
        PutData(
          textData,
          null,
          1,
          insertTextBeforeCaret = true,
          rawIndent = true,
          caretAfterInsertedText = false,
        )
      } else {
        PutData(
          textData,
          null,
          1,
          insertTextBeforeCaret = false,
          rawIndent = true,
          caretAfterInsertedText = false,
          putToLine = address1 - 1
        )
      }
      injector.put.putTextForCaret(editor, mutableCaret, context, putData)

      // Move the caret to the last line of the copied text, obeying 'startofline'
      // The copied text is placed after the target line (address1 for line after that address, or 0 for start of file)
      // The caret should be on the last line of the copied range
      val targetLine = if (address1 == 0) {
        sourceLineRange.size - 1
      } else {
        address1 + sourceLineRange.size - 1
      }

      val caretOffset = if (!injector.options(editor).startofline) {
        // Maintain the original column position if 'nostartofline' is set
        val column = editor.normalizeColumn(targetLine, caretColumn, allowEnd = false)
        editor.bufferPositionToOffset(BufferPosition(targetLine, column))
      } else {
        // Move to the first non-whitespace character on the line
        injector.motion.moveCaretToLineStartSkipLeading(editor, targetLine)
      }
      mutableCaret.moveToOffset(caretOffset)
    }
    return ExecutionResult.Success
  }
}
