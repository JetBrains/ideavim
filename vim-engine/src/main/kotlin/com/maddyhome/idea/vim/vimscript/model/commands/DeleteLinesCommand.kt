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
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :delete"
 */
@ExCommand(command = "d[elete]")
public data class DeleteLinesCommand(val range: Range, var argument: String) : Command.ForEachCaret(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val argument = this.argument
    val register = if (argument.isNotEmpty() && !argument[0].isDigit()) {
      this.argument = argument.substring(1)
      argument[0]
    } else {
      injector.registerGroup.defaultRegister
    }

    if (!injector.registerGroup.selectRegister(register)) return ExecutionResult.Error

    val textRange = getTextRange(editor, caret, true)
    return if (injector.changeGroup
        .deleteRange(editor, caret, textRange, SelectionType.LINE_WISE, false, operatorArguments)
    ) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}
