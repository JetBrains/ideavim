/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

/**
 * see "h :set"
 */
data class SetCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  companion object {
    internal var isExecutingCommand = false
  }

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    isExecutingCommand = true
    try {
      OptionsManager.parseOptionLine(editor, argument, true)
    } catch (e: ExException) {
      // same exceptions will be thrown later, so we ignore them for now
    }
    isExecutingCommand = false
    return if (parseOptionLine(editor, argument, OptionService.Scope.GLOBAL, failOnBad = true)) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}

data class SetLocalCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    return if (parseOptionLine(editor, argument, OptionService.Scope.LOCAL(editor), failOnBad = true)) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}
/**
 * This parses a set of :set commands. The following types of commands are supported:
 *
 *  * :set - show all changed options
 *  * :set all - show all options
 *  * :set all& - reset all options to default values
 *  * :set {option} - set option of boolean, display others
 *  * :set {option}? - display option
 *  * :set no{option} - reset boolean option
 *  * :set inv{option} - toggle boolean option
 *  * :set {option}! - toggle boolean option
 *  * :set {option}& - set option to default
 *  * :set {option}={value} - set option to new value
 *  * :set {option}:{value} - set option to new value
 *  * :set {option}+={value} - append or add to option value
 *  * :set {option}-={value} - remove or subtract from option value
 *  * :set {option}^={value} - prepend or multiply option value
 *
 *
 * @param editor    The editor the command was entered for, null if no editor - reading .ideavimrc
 * @param args      The :set command arguments
 * @param failOnBad True if processing should stop when a bad argument is found, false if a bad argument is simply
 * skipped and processing continues.
 * @return True if no errors were found, false if there were any errors
 */
// todo is failOnBad used anywhere?
fun parseOptionLine(editor: Editor, args: String, scope: OptionService.Scope, failOnBad: Boolean): Boolean {
  // No arguments so we show changed values
  val optionService = (VimPlugin.getOptionService() as OptionServiceImpl)
  when {
    args.isEmpty() -> {
      val changedOptions = optionService.getOptions().filter { !optionService.isDefault(scope, it) }
      showOptions(editor, changedOptions.map { Pair(it, it) }, scope, true)
      return true
    }
    args == "all" -> {
      showOptions(editor, optionService.getOptions().map { Pair(it, it) }, scope, true)
      return true
    }
    args == "all&" -> {
      optionService.resetAllOptions()
      return true
    }
  }

  // We now have 1 or more option operators separator by spaces
  var error: String? = null
  var token = ""
  val tokenizer = StringTokenizer(args)
  val toShow = mutableListOf<Pair<String, String>>()
  while (tokenizer.hasMoreTokens()) {
    token = tokenizer.nextToken()
    // See if a space has been backslashed, if no get the rest of the text
    while (token.endsWith("\\")) {
      token = token.substring(0, token.length - 1) + ' '
      if (tokenizer.hasMoreTokens()) {
        token += tokenizer.nextToken()
      }
    }

    when {
      token.endsWith("?") -> toShow.add(Pair(token.dropLast(1), token))
      token.startsWith("no") -> optionService.unsetOption(scope, token.substring(2), token)
      token.startsWith("inv") -> optionService.toggleOption(scope, token.substring(3), token)
      token.endsWith("!") -> optionService.toggleOption(scope, token.dropLast(1), token)
      token.endsWith("&") -> optionService.resetDefault(scope, token.dropLast(1), token)
      else -> {
        // This must be one of =, :, +=, -=, or ^=
        // Look for the = or : first
        var eq = token.indexOf('=')
        if (eq == -1) {
          eq = token.indexOf(':')
        }
        // No operator so only the option name was given
        if (eq == -1) {
          if (optionService.isToggleOption(token)) {
            optionService.setOption(scope, token, token)
          } else if (!optionService.getOptions().contains(token)) {
            error = Msg.unkopt
          } else {
            toShow.add(Pair(token, token))
          }
        } else {
          // Make sure there is an option name
          if (eq > 0) {
            // See if an operator before the equal sign
            val op = token[eq - 1]
            var end = eq
            if (op in "+-^") {
              end--
            }
            // Get option name and value after operator
            val option = token.take(end)
            val value = token.substring(eq + 1)
            when (op) {
              '+' -> optionService.appendValue(scope, option, value, token)
              '-' -> optionService.removeValue(scope, option, value, token)
              '^' -> optionService.prependValue(scope, option, value, token)
              else -> optionService.setOptionValue(scope, option, value, token)
            }
          } else {
            error = Msg.unkopt
          }
        }
      }
    }
    if (failOnBad && error != null) {
      break
    }
  }

  // Now show all options that were individually requested
  if (toShow.size > 0) {
    showOptions(editor, toShow, scope, false)
  }

  if (error != null) {
    throw ExException(MessageHelper.message(error, token))
  }

  return true
}

private fun showOptions(editor: Editor, nameAndToken: Collection<Pair<String, String>>, scope: OptionService.Scope, showIntro: Boolean) {
  val optionService = VimPlugin.getOptionService()
  val optionsToShow = mutableListOf<String>()
  var unknownOption: Pair<String, String>? = null
  val optionsAndAbbrevs = optionService.getOptions() + optionService.getAbbrevs()
  for (pair in nameAndToken) {
    if (optionsAndAbbrevs.contains(pair.first)) {
      optionsToShow.add(pair.first)
    } else {
      unknownOption = pair
      break
    }
  }

  val cols = mutableListOf<String>()
  val extra = mutableListOf<String>()
  for (option in optionsToShow) {
    val optionAsString = optionToString(scope, option, editor)
    if (optionAsString.length > 19) extra.add(optionAsString) else cols.add(optionAsString)
  }

  cols.sort()
  extra.sort()

  var width = EditorHelper.getApproximateScreenWidth(editor)
  if (width < 20) {
    width = 80
  }
  val colCount = width / 20
  val height = ceil(cols.size.toDouble() / colCount.toDouble()).toInt()
  var empty = cols.size % colCount
  empty = if (empty == 0) colCount else empty

  val res = StringBuilder()
  if (showIntro) {
    res.append("--- Options ---\n")
  }
  for (h in 0 until height) {
    for (c in 0 until colCount) {
      if (h == height - 1 && c >= empty) {
        break
      }

      var pos = c * height + h
      if (c > empty) {
        pos -= c - empty
      }

      val opt = cols[pos]
      res.append(opt.padEnd(20))
    }
    res.append("\n")
  }

  for (opt in extra) {
    val seg = (opt.length - 1) / width
    for (j in 0..seg) {
      res.append(opt, j * width, min(j * width + width, opt.length))
      res.append("\n")
    }
  }
  ExOutputModel.getInstance(editor).output(res.toString())

  if (unknownOption != null) {
    throw ExException("E518: Unknown option: ${unknownOption.second}")
  }
}

private fun optionToString(scope: OptionService.Scope, name: String, editor: Editor): String {
  val value = VimPlugin.getOptionService().getOptionValue(scope, name)
  return if (VimPlugin.getOptionService().isToggleOption(name)) {
    if (value.asBoolean()) "  $name" else "no$name"
  } else {
    "$name=$value"
  }
}
