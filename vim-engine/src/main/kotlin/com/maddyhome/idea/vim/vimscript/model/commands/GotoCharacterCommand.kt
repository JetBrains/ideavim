/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import kotlin.math.max
import kotlin.math.min

/**
 * see "h :goto"
 */
data class GotoCharacterCommand(val ranges: Ranges, val argument: String) : Command.ForEachCaret(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): ExecutionResult {
    val count = getCount(editor, caret, 1, true)
    if (count <= 0) return ExecutionResult.Error

    val offset = max(0, min(count - 1, editor.fileSize().toInt() - 1))
    if (offset == -1) return ExecutionResult.Error

    injector.motion.moveCaret(editor, caret, offset)

    return ExecutionResult.Success
  }
}
