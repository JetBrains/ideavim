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
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :put"
 */
@ExCommand(command = "pu[t]")
data class PutLinesCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

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

    val line = if (range.size() == 0) -1 else getLine(editor)
    val textData = registerGroup.getRegister(editor, context, registerGroup.lastRegisterChar)?.let {
      PutData.TextData(null, it.copiedText, SelectionType.LINE_WISE)
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
    return if (injector.put.putText(editor, context, putData)) ExecutionResult.Success else ExecutionResult.Error
  }
}
