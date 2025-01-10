/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
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

  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val carets = editor.sortedCarets()
    for (caret in carets) {
      val range = getLineRange(editor, caret).toTextRange(editor)
      val copiedText = injector.clipboardManager.collectCopiedText(editor, context, range)

      // Copy is defined as:
      // :[range]co[py] {address}
      // Copy the given [range] to below the line given by {address}. Address can be a range, but only the first address
      // is used. The rest is ignored with no errors. Note that address is one-based, and 0 means copy the text to below
      // the line _before_ the first line (i.e., copy to above the first line).
      val address1 = getAddressFromArgument(editor)

      val textData = PutData.TextData(null, copiedText, SelectionType.LINE_WISE)
      var mutableCaret = caret
      val putData = if (address1 == 0) {
        // TODO: This should maintain current column location
        mutableCaret = mutableCaret.moveToOffset(0)
        PutData(
          textData,
          null,
          1,
          insertTextBeforeCaret = true,
          rawIndent = true,
          caretAfterInsertedText = false,
        )
      }
      else {
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
    }
    return ExecutionResult.Success
  }
}
