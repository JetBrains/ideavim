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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope

/**
 * see "h :delfunction"
 */
data class DelfunctionCommand(
  val ranges: Ranges,
  val scope: Scope?,
  val name: String,
  val ignoreIfMissing: Boolean,
) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (ignoreIfMissing) {
      try {
        injector.functionService.deleteFunction(name, scope, this)
      } catch (e: ExException) {
        if (e.message != null && e.message!!.startsWith("E130")) {
          // "ignoreIfMissing" flag handles the "E130: Unknown function" exception
        } else {
          throw e
        }
      }
    } else {
      injector.functionService.deleteFunction(name, scope, this)
    }
    return ExecutionResult.Success
  }
}
