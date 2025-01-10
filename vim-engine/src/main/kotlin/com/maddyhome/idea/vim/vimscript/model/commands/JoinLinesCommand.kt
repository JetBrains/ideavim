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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :join"
 */
@ExCommand(command = "j[oin]")
data class JoinLinesCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.ForEachCaret(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val spaces = modifier != CommandModifier.BANG

    val lineRange = getLineRangeWithCount(editor, caret)
    val textRange = lineRange.toTextRange(editor)

    // Join the given range, which might not match the current location of the caret
    val success = injector.changeGroup.deleteJoinRange(
      editor,
      context,
      caret,
      TextRange(textRange.startOffset, textRange.endOffset - 1),
      spaces,
      operatorArguments
    )
    if (success) {
      // We've joined all the lines. The end line of the range is now the start line
      val offset = injector.motion.moveCaretToLineStartSkipLeading(editor, lineRange.startLine)
      caret.moveToOffset(offset)
      return ExecutionResult.Success
    }
    return ExecutionResult.Error
  }
}
