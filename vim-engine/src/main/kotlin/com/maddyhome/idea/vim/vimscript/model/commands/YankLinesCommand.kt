/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :yank"
 */
data class YankLinesCommand(val ranges: Ranges, var argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val argument = this.argument
    val registerGroup = injector.registerGroup
    val register = if (argument.isNotEmpty() && !argument[0].isDigit()) {
      this.argument = argument.substring(1)
      argument[0]
    } else {
      registerGroup.defaultRegister
    }

    if (!registerGroup.selectRegister(register)) return ExecutionResult.Error

    val starts = ArrayList<Int>(editor.nativeCarets().size)
    val ends = ArrayList<Int>(editor.nativeCarets().size)
    for (caret in editor.nativeCarets()) {
      val range = getTextRange(editor, caret, true)
      starts.add(range.startOffset)
      ends.add(range.endOffset)
    }

    return if (injector.yank.yankRange(
        editor,
        TextRange(starts.toIntArray(), ends.toIntArray()),
        SelectionType.LINE_WISE, false
      )
    ) ExecutionResult.Success else ExecutionResult.Error
  }
}
