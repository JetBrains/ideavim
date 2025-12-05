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
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

@ExCommand(command = "flip")
data class FlipCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val caret = editor.carets().get(0)!!
    val range = injector.searchHelper.findWordObject(editor, caret, 1, isOuter = false, isBig = false)
    flipText(editor, range)
    return ExecutionResult.Success
  }

  fun flipText(
    editor: VimEditor,
    range: TextRange,
  ): Boolean {
    flip(editor, range.startOffset, range.endOffset)
    return true
  }

  private fun flip(
    editor: VimEditor,
    start: Int,
    end: Int,
  ) {
    val selectedText = editor.text().subSequence(start, end)
    replaceText(editor, start, end, StringBuilder(selectedText).reverse().toString())
  }

  private fun replaceText(editor: VimEditor, start: Int, end: Int, str: String) {
    injector.application.runWriteAction {
      (editor as MutableVimEditor).replaceString(start, end, str)
    }
  }
}
