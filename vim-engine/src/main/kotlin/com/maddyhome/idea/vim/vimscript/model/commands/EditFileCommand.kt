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
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :edit"
 */
data class EditFileCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
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
      injector.actionExecutor.executeAction("OpenFile", injector.executionContextManager.createEditorDataContext(editor, context))
    }

    return ExecutionResult.Success
  }
}
