/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.lang.Integer.min

/**
 * see "h :[range]"
 */
data class GoToLineCommand(val range: Range) :
  Command.ForEachCaret(range, CommandModifier.NONE) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_REQUIRED, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    // The command's range is one-based, but zero is a valid address
    val line1 = min(getLineRange(editor, caret).endLine1, editor.lineCount())
    if (line1 >= 0) {
      val offset = injector.motion.moveCaretToLineWithStartOfLineOption(editor, (line1 - 1).coerceAtLeast(0), caret)
      caret.moveToOffset(offset)
      return ExecutionResult.Success
    }

    caret.moveToOffset(0)
    return ExecutionResult.Error
  }
}
