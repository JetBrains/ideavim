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
import com.maddyhome.idea.vim.options.OptionDeclaredScope
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.*
import kotlin.math.ceil

/**
 * see "h :set"
 */
@ExCommand(command = "se[t]")
public data class SetCommand(val ranges: Ranges, val argument: String) : SetCommandBase(ranges, argument) {
  override fun getScope(editor: VimEditor): OptionScope = OptionScope.AUTO(editor)
}

@ExCommand(command = "setg[lobal]")
public data class SetglobalCommand(val ranges: Ranges, val argument: String) : SetCommandBase(ranges, argument) {
  override fun getScope(editor: VimEditor): OptionScope = OptionScope.GLOBAL
}

@ExCommand(command = "setl[ocal]")
public data class SetlocalCommand(val ranges: Ranges, val argument: String) : SetCommandBase(ranges, argument) {
  override fun getScope(editor: VimEditor): OptionScope = OptionScope.LOCAL(editor)
}

public abstract class SetCommandBase(ranges: Ranges, argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): ExecutionResult {
    return if (parseOptionLine(editor, commandArgument, getScope(editor), failOnBad = true)) {
      ExecutionResult.Success
    } else {
      ExecutionResult.Error
    }
  }

  protected abstract fun getScope(editor: VimEditor): OptionScope
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
 *  * :set {option}< - set the option to a copy of the global value
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

  val columnFormat = args.startsWith("!")
  val argument = args.removePrefix("!").trimStart()

  when {
    argument.isEmpty() -> {
      val changedOptions = optionGroup.getAllOptions().filter { !optionGroup.isDefaultValue(it, scope) }
      showOptions(editor, changedOptions.map { Pair(it.name, it.name) }, scope, true, columnFormat)
      return true
    }
    argument == "all" -> {
      showOptions(editor, optionGroup.getAllOptions().map { Pair(it.name, it.name) }, scope, true, columnFormat)
      return true
    }
    argument == "all&" -> {
      // Note that `all&` resets all options in the current editor at local and global scope. This includes global,
      // global-local and local-to-buffer options, which will affect other windows. It does not affect the local values
      // of local-to-window options in other windows
      optionGroup.resetAllOptions(editor)
      return true
    }
  }

  // We now have 1 or more option operators separator by spaces
  var error: String? = null
  var token = ""
  val tokenizer = StringTokenizer(argument)
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

    // Look for the `=` or `:` first
    var eq = token.indexOf('=')
    if (eq == -1) {
      eq = token.indexOf(':')
    }

    when {
      eq == -1 && token.endsWith("?") -> toShow.add(Pair(token.dropLast(1), token))
      eq == -1 && token.startsWith("no") -> optionGroup.unsetToggleOption(getValidToggleOption(token.substring(2), token), scope)
      eq == -1 && token.startsWith("inv") -> optionGroup.invertToggleOption(getValidToggleOption(token.substring(3), token), scope)
      eq == -1 && token.endsWith("!") -> optionGroup.invertToggleOption(getValidToggleOption(token.dropLast(1), token), scope)
      eq == -1 && token.endsWith("&") -> optionGroup.resetDefaultValue(getValidOption(token.dropLast(1), token), scope)
      eq == -1 && token.endsWith("<") -> {
        // Copy the global value to the target scope. If the target scope is global, this is a no-op. When copying a
        // string global-local option to effective scope, Vim's behaviour matches setting that option at effective
        // scope. That is, it sets the global value (a no-op) and resets the local value.
        val option = getValidOption(token.dropLast(1), token)
        val globalValue = optionGroup.getOptionValue(option, OptionScope.GLOBAL)
        optionGroup.setOptionValue(option, scope, globalValue)
      }
      else -> {
        // This must be one of =, :, +=, -=, or ^=
        // No operator so only the option name was given
        if (eq == -1) {
          // We must explicitly treat the return value as covariant instead of `Option<VimDataType>?` so that we can
          // successfully check the type against `ToggleOption`.
          val option: Option<out VimDataType>? = optionGroup.getOption(token)
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
    showOptions(editor, toShow, scope, false, columnFormat)
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

private fun showOptions(
  editor: VimEditor,
  nameAndToken: Collection<Pair<String, String>>,
  scope: OptionScope,
  showIntro: Boolean,
  columnFormat: Boolean
) {
  val optionService = injector.optionGroup
  val optionsToShow = mutableListOf<Option<VimDataType>>()
  var unknownOption: Pair<String, String>? = null

  for (pair in nameAndToken.sortedWith { o1, o2 -> String.CASE_INSENSITIVE_ORDER.compare(o1.first, o2.first) }) {
    val myOption = optionService.getOption(pair.first)
    if (myOption != null) {
      optionsToShow.add(myOption)
    } else {
      unknownOption = pair
      break
    }
  }

  val colWidth = 20
  val cells = mutableListOf<String>()
  val extra = mutableListOf<String>()
  for (option in optionsToShow) {
    val optionAsString = formatKnownOptionValue(option, scope)
    if (columnFormat || optionAsString.length >= colWidth) extra.add(optionAsString) else cells.add(optionAsString)
  }

  // Note that this is the approximate width of the associated editor, not the ex output panel!
  // It excludes gutter width, for example
  val width = injector.engineEditorHelper.getApproximateScreenWidth(editor).let { if (it < 20) 80 else it }
  val colCount = width / colWidth
  val height = ceil(cells.size.toDouble() / colCount.toDouble()).toInt()

  val output = buildString {
    if (showIntro) {
      when (scope) {
        is OptionScope.AUTO -> appendLine("--- Options ---")
        is OptionScope.LOCAL -> appendLine("--- Local option values ---")
        OptionScope.GLOBAL -> appendLine("--- Global option values ---")
      }
    }

    for (h in 0 until height) {
      val lengthAtStartOfLine = length
      for (c in 0 until colCount) {
        val index = c * height + h
        if (index < cells.size) {
          val padLength = lengthAtStartOfLine + (c * colWidth) - length
          for (i in 1..padLength) {
            append(' ')
          }

          append(cells[index])
        }
      }
      appendLine()
    }

    // Add any lines that are too long to fit into columns. The panel will soft wrap text
    for (option in extra) {
      appendLine(option)
    }
  }
  injector.exOutputPanel.getPanel(editor).output(output)

  if (unknownOption != null) {
    throw exExceptionMessage("E518", unknownOption.second)
  }
}

private fun formatKnownOptionValue(option: Option<out VimDataType>, scope: OptionScope): String {
  val value = injector.optionGroup.getOptionValue(option, scope)
  if (option is ToggleOption) {

    // Unset global-local toggle option
    if ((option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
        || option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
      && scope is OptionScope.LOCAL && value == VimInt.MINUS_ONE) {
      return "--${option.name}"
    }

    return if (value.asBoolean()) "  ${option.name}" else "no${option.name}"
  } else {
    return "  ${option.name}=$value"
  }
}

private fun appendValue(option: Option<VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.appendValue(currentValue as VimString, value as VimString)
    is StringListOption -> option.appendValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.addValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

private fun prependValue(option: Option<VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.prependValue(currentValue as VimString, value as VimString)
    is StringListOption -> option.prependValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.multiplyValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

private fun removeValue(option: Option<VimDataType>, currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (option) {
    is StringOption -> option.removeValue(currentValue as VimString, value as VimString)
    is StringListOption -> option.removeValue(currentValue as VimString, value as VimString)
    is NumberOption -> option.subtractValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}
