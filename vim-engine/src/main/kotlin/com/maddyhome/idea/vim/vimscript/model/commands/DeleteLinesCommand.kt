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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :delete"
 */
@ExCommand(command = "d[elete]")
public data class DeleteLinesCommand(val range: Range, val argument: String) : Command.ForEachCaret(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val register = if (argument.isNotEmpty() && !argument[0].isDigit()) {
      if (!injector.registerGroup.isValid(argument[0]) || !injector.registerGroup.isRegisterWritable(argument[0])) {
        throw exExceptionMessage("E488", argument)  // E488: Trailing characters: {0}
      }
      setNextArgumentTokenOffset(1) // Skip the register
      argument[0]
    } else {
      injector.registerGroup.defaultRegister
    }

    if (!injector.registerGroup.selectRegister(register)) return ExecutionResult.Error

    val textRange = getLineRangeWithCount(editor, caret).toTextRange(editor)
    return if (injector.changeGroup
      .deleteRange(editor, caret, textRange, SelectionType.LINE_WISE, false, operatorArguments)
    ) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}
