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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/*
 * see "h :tabmove"
 */
@ExCommand(command = "tabm[ove]")
data class TabMoveCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (range.size() != 0) {
      throw ExException("Range form of tabmove command is not supported. Please use the argument form")
    }

    val tabService = injector.tabService
    val tabCount = tabService.getTabCount(context)
    val currentIndex = tabService.getCurrentTabIndex(context)
    val index: Int

    try {
      var argument = argument.trim()
      if (argument == "+" || argument == "-") {
        argument += "1"
      }
      index = if (argument.startsWith("+")) {
        val number = Integer.parseInt(argument.substring(1))
        if (number == 0) {
          throw ExException("E474: Invalid argument")
        }
        currentIndex + number
      } else if (argument.startsWith("-")) {
        val number = Integer.parseInt(argument.substring(1))
        if (number == 0) {
          throw ExException("E474: Invalid argument")
        }
        currentIndex - number
      } else if (argument == "$" || argument.isBlank()) {
        tabCount - 1
      } else {
        var number = Integer.parseInt(argument)

        // it's strange, but it is the way Vim works
        if (number > currentIndex) number -= 1
        number
      }
    } catch (_: NumberFormatException) {
      throw ExException("E474: Invalid argument")
    }

    if (index < 0 || index >= tabCount) {
      throw ExException("E474: Invalid argument")
    }
    tabService.moveCurrentTabToIndex(index, context)
    return ExecutionResult.Success
  }
}
