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
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :yank"
 */
@ExCommand(command = "y[ank]")
data class YankLinesCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val register = consumeRegisterFromArgument()
    if (!injector.registerGroup.selectRegister(register)) return ExecutionResult.Error

    val starts = ArrayList<Int>(editor.nativeCarets().size)
    val ends = ArrayList<Int>(editor.nativeCarets().size)
    for (caret in editor.nativeCarets()) {
      val range = getLineRangeWithCount(editor, caret).toTextRange(editor)
      starts.add(range.startOffset)
      ends.add(range.endOffset)
    }

    return if (injector.yank.yankRange(
        editor,
        context,
        TextRange(starts.toIntArray(), ends.toIntArray()),
        SelectionType.LINE_WISE,
        false
      )
    ) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}
