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
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

// Currently support only matchit
@ExCommand(command = "pa[ckadd]")
public class PackaddCommand(public val range: Range, public val argument: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (argument == "matchit" || (argument.startsWith("!") && argument.drop(1).trim() == "matchit")) {
      val option = injector.optionGroup.getOption("matchit") as ToggleOption
      injector.optionGroup.setToggleOption(option, OptionAccessScope.GLOBAL(editor))
    }
    return ExecutionResult.Success
  }
}
