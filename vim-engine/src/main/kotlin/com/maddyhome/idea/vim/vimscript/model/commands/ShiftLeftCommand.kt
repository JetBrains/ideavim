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
 * see "h :<"
 */
@ExCommand("<")
data class ShiftLeftCommand(val range: Range, val argument: String, val length: Int) :
  Command.ForEachCaret(range, CommandModifier.NONE, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val lineRange = getLineRangeWithCount(editor, caret)
    val textRange = lineRange.toTextRange(editor)
    val endOffsets = textRange.endOffsets.map { it - 1 }.toIntArray()
    injector.changeGroup.indentRange(
      editor,
      caret,
      context,
      TextRange(textRange.startOffsets, endOffsets),
      length,
      -1,
      operatorArguments,
    )
    // Indent will move the caret to the start line of the range, either maintaining the column or moving to the first
    // non-whitespace character, depending on the 'startofline' option. This is the behaviour for the `<` operator, but
    // the `:<` command always places the caret at the first non-whitespace character of the last line in the range.
    val offset = injector.motion.moveCaretToLineStartSkipLeading(editor, lineRange.endLine)
    caret.moveToOffset(offset)
    return ExecutionResult.Success
  }
}
