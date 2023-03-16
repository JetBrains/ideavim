/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.invertToggleOption
import com.maddyhome.idea.vim.api.isDefaultValue
import com.maddyhome.idea.vim.api.resetDefaultValue
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.api.unsetToggleOption
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

/**
 * see "h :set"
 */
public data class SetCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val result = parseOptionLine(editor, argument, OptionScope.GLOBAL, failOnBad = true)
    return if (result) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }
}

public data class SetLocalCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    return if (parseOptionLine(editor, argument, OptionScope.LOCAL(editor), failOnBad = true)) {
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
public fun parseOptionLine(editor: VimEditor, args: String, scope: OptionScope, failOnBad: Boolean): Boolean {
  // No arguments so we show changed values
  val optionGroup = injector.optionGroup
  when {
    args.isEmpty() -> {
      val changedOptions = optionGroup.getAllOptions().filter { !optionGroup.isDefaultValue(it, scope) }
      showOptions(editor, changedOptions.map { Pair(it.name, it.name) }, scope, true)
      return true
    }
    args == "all" -> {
      showOptions(editor, optionGroup.getAllOptions().map { Pair(it.name, it.name) }, scope, true)
      return true
    }
    args == "all&" -> {
      optionGroup.resetAllOptions()
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
      token.startsWith("no") -> optionGroup.unsetToggleOption(getValidToggleOption(token.substring(2), token), scope)
      token.startsWith("inv") -> optionGroup.invertToggleOption(getValidToggleOption(token.substring(3), token), scope)
      token.endsWith("!") -> optionGroup.invertToggleOption(getValidToggleOption(token.dropLast(1), token), scope)
      token.endsWith("&") -> optionGroup.resetDefaultValue(getValidOption(token.dropLast(1), token), scope)
      else -> {
        // This must be one of =, :, +=, -=, or ^=
        // Look for the = or : first
        var eq = token.indexOf('=')
        if (eq == -1) {
          eq = token.indexOf(':')
        }
        // No operator so only the option name was given
        if (eq == -1) {
          val option = optionGroup.getOption(token)
          when (option) {
            null -> error = Msg.unkopt
            is ToggleOption -> optionGroup.setToggleOption(option, scope)
            else -> toShow.add(Pair(option.name, option.abbrev))
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
            val optionName = token.take(end)
            val option = getValidOption(optionName)
            val existingValue = optionGroup.getOptionValue(option, scope)
            val value = option.parseValue(token.substring(eq + 1), token)
            val newValue = when (op) {
              '+' -> appendValue(option, existingValue, value)
              '^' -> prependValue(option, existingValue, value)
              '-' -> removeValue(option, existingValue, value)
              else -> value
            } ?: throw exExceptionMessage("E474", token)
            optionGroup.setOptionValue(option, scope, newValue)
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
    throw ExException(injector.messages.message(error, token))
  }

  return true
}

private fun getValidOption(optionName: String, token: String = optionName) =
  injector.optionGroup.getOption(optionName) ?: throw exExceptionMessage("E518", token)

private fun getValidToggleOption(optionName: String, token: String) =
  getValidOption(optionName, token) as? ToggleOption ?: throw exExceptionMessage("E474", token)

private fun showOptions(editor: VimEditor, nameAndToken: Collection<Pair<String, String>>, scope: OptionScope, showIntro: Boolean) {
  val optionService = injector.optionGroup
  val optionsToShow = mutableListOf<Option<out VimDataType>>()
  var unknownOption: Pair<String, String>? = null
  for (pair in nameAndToken) {
    val myOption = optionService.getOption(pair.first)
    if (myOption != null) {
      optionsToShow.add(myOption)
    } else {
      unknownOption = pair
      break
    }
  }

  val cols = mutableListOf<String>()
  val extra = mutableListOf<String>()
  for (option in optionsToShow) {
    val optionAsString = formatKnownOptionValue(option, scope)
    if (optionAsString.length > 19) extra.add(optionAsString) else cols.add(optionAsString)
  }

  cols.sort()
  extra.sort()

  var width = injector.engineEditorHelper.getApproximateScreenWidth(editor)
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
  injector.exOutputPanel.getPanel(editor).output(res.toString())

  if (unknownOption != null) {
    throw exExceptionMessage("E518", unknownOption.second)
  }
}

private fun formatKnownOptionValue(option: Option<out VimDataType>, scope: OptionScope): String {
  val value = injector.optionGroup.getOptionValue(option, scope)
  return if (option is ToggleOption) {
    if (value.asBoolean()) "  ${option.name}" else "no${option.name}"
  } else {
    "${option.name}=$value"
  }
}

private fun appendValue(option: Option<out VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.appendValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.addValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

private fun prependValue(option: Option<out VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.prependValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.multiplyValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

private fun removeValue(option: Option<out VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.removeValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.subtractValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}
