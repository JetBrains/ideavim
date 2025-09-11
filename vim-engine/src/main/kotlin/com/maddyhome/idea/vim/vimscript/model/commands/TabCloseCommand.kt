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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author Rieon Ke
 * see "h :tabclose"
 */
@ExCommand(command = "tabc[lose]")
data class TabCloseCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val current = injector.tabService.getCurrentTabIndex(context)
    val tabCount = injector.tabService.getTabCount(context)

    val argument = argument
    val index = getTabIndexToClose(argument, current, tabCount - 1)
      ?: throw exExceptionMessage("E475", argument)

    val select = if (index == current) index + 1 else current
    injector.tabService.removeTabAt(index, select, context)

    return ExecutionResult.Success
  }

  /**
   * parse command argument to tab index.
   * :tabclose -2  close the two previous tab page
   * :tabclose +   close the next tab page
   * :tabclose +2   close the two next tab page
   * :tabclose 3   close the third tab page
   * :tabclose $   close the last tab page
   * @param arg command argument
   * @param current current selected tab index
   * @param last the last tab index of active tabbed pane
   * @return tab index to close
   */
  private fun getTabIndexToClose(arg: String, current: Int, last: Int): Int? {
    if (arg.isEmpty()) return current
    if (last < 0) return null

    val sb = StringBuilder()
    var sign = Char.MIN_VALUE
    var end = false

    for (c in arg) {
      when {
        c in '0'..'9' && !end -> sb.append(c)

        (c == '-' || c == '+') && !end && sb.isEmpty() && sign == Char.MIN_VALUE -> sign = c

        c == '$' && sb.isEmpty() && sign == Char.MIN_VALUE -> end = true

        c == ' ' -> {
          // ignore
        }

        else -> return null
      }
    }

    val idxStr = sb.toString()

    val index = when {
      end -> last

      idxStr.isEmpty() -> {
        when (sign) {
          '+' -> current + 1
          '-' -> current - 1
          else -> current
        }
      }

      else -> {
        val idx = idxStr.toIntOrNull() ?: return null
        when (sign) {
          '+' -> current + idx
          '-' -> current - idx
          else -> idx
        }
      }
    }
    return index.coerceIn(0, last)
  }
}
