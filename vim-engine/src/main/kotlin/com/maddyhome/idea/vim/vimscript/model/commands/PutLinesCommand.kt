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
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :put"
 */
@ExCommand(command = "pu[t]")
public data class PutLinesCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (editor.isOneLineMode()) return ExecutionResult.Error

    val registerGroup = injector.registerGroup
    val arg = argument
    if (arg.isNotEmpty()) {
      if (!registerGroup.selectRegister(arg[0])) {
        return ExecutionResult.Error
      }
    } else {
      registerGroup.selectRegister(registerGroup.defaultRegister)
    }

    val line = if (ranges.size() == 0) -1 else getLine(editor)
    val textData = registerGroup.lastRegister?.let {
      PutData.TextData(
        it.text ?: injector.parser.toKeyNotation(it.keys),
        SelectionType.LINE_WISE,
        it.transferableData,
        null,
      )
    }
    val putData = PutData(
      textData,
      null,
      1,
      insertTextBeforeCaret = false,
      rawIndent = false,
      caretAfterInsertedText = false,
      putToLine = line,
    )
    return if (injector.put.putText(editor, context, putData, operatorArguments = operatorArguments)) ExecutionResult.Success else ExecutionResult.Error
  }
}
