/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author Rieon Ke
 * see "h :tabclose"
 */
data class TabCloseCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    val current = injector.tabService.getCurrentTabIndex(context)
    val tabCount = injector.tabService.getTabCount(context)

    val argument = argument
    val index = getTabIndexToClose(argument, current, tabCount - 1)

    if (index != null) {
      val select = if (index == current) index + 1 else current
      injector.tabService.removeTabAt(index, select, context)
    } else {
      injector.messages.showStatusBarMessage(injector.messages.message("error.invalid.command.argument"))
    }

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
