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
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :<"
 */
data class ShiftLeftCommand(val ranges: Ranges, val argument: String, val length: Int) : Command.ForEachCaret(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): ExecutionResult {
    val range = getTextRange(editor, caret, true)
    val endOffsets = range.endOffsets.map { it - 1 }.toIntArray()
    injector.changeGroup.indentRange(
      editor, caret, context,
      TextRange(range.startOffsets, endOffsets),
      length, -1, operatorArguments
    )
    return ExecutionResult.Success
  }
}
