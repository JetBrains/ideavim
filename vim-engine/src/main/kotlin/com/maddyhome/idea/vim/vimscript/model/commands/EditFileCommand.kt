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
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :edit"
 */
@ExCommand(command = "e[dit],bro[wse]")
data class EditFileCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val arg = argument
    if (arg == "#") {
      injector.jumpService.saveJumpLocation(editor)
      injector.file.selectPreviousTab(context)
      return ExecutionResult.Success
    } else if (arg.isNotEmpty()) {
      val res = injector.file.openFile(arg, context)
      if (res) {
        injector.jumpService.saveJumpLocation(editor)
      }
      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    // Don't open a choose file dialog under a write action
    injector.application.invokeLater {
      injector.actionExecutor.executeAction(editor, name = "OpenFile", context = context)
    }

    return ExecutionResult.Success
  }
}
