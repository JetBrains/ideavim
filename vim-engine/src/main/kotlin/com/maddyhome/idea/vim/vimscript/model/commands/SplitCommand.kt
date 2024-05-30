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
 * see "h :split" / "h :vsplit"
 */
@ExCommand(command = "sp[lit],vs[plit]")
public data class SplitCommand(val range: Range, val argument: String, val splitType: SplitType) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (splitType == SplitType.VERTICAL) {
      injector.window.splitWindowVertical(context, argument)
    } else {
      injector.window.splitWindowHorizontal(context, argument)
    }

    return ExecutionResult.Success
  }
}

public enum class SplitType {
  VERTICAL,
  HORIZONTAL,
}
