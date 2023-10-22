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
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionDeclaredScope
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
  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.EFFECTIVE(editor)
}

@ExCommand(command = "setg[lobal]")
public data class SetglobalCommand(val ranges: Ranges, val argument: String) : SetCommandBase(ranges, argument) {
  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.GLOBAL(editor)
}

@ExCommand(command = "setl[ocal]")
public data class SetlocalCommand(val ranges: Ranges, val argument: String) : SetCommandBase(ranges, argument) {
  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.LOCAL(editor)
}

public abstract class SetCommandBase(ranges: Ranges, argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): ExecutionResult {
    parseOptionLine(editor, commandArgument, getScope(editor))
    return ExecutionResult.Success
  }

  protected abstract fun getScope(editor: VimEditor): OptionAccessScope
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
 *  * Also supports repeated statements, e.g. `:set {option} {option} {option}`. Will try to evaluate everything up to
 *    the first error, such as an unknown option or an incorrectly formatted operation.
 *
 * @param editor    The editor the command was entered for, null if no editor - reading .ideavimrc
 * @param args      The raw text passed to the `:set` command
 * @throws ExException Thrown if any option names or operations are incorrect
 */
public fun parseOptionLine(editor: VimEditor, args: String, scope: OptionAccessScope) {
  val optionGroup = injector.optionGroup

  val columnFormat = args.startsWith("!")
  val argument = args.removePrefix("!").trimStart()

  when {
    argument.isEmpty() -> {
      // No arguments mean we show only changed values
      val changedOptions = optionGroup.getAllOptions().filter { !optionGroup.isDefaultValue(it, scope) }
      showOptions(editor, changedOptions.map { Pair(it.name, it.name) }, scope, true, columnFormat)
      return
    }
    argument == "all" -> {
      showOptions(editor, optionGroup.getAllOptions().map { Pair(it.name, it.name) }, scope, true, columnFormat)
      return
    }
    argument == "all&" -> {
      // Note that `all&` resets all options in the current editor at local and global scope. This includes global,
      // global-local and local-to-buffer options, which will affect other windows. It does not affect the local values
      // of local-to-window options in other windows
      optionGroup.resetAllOptions(editor)
      return
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

    val isKeyValueOperation = token.indexOf('=') != -1 || token.indexOf(':') != -1
    if (!isKeyValueOperation) {
      when {
        token.endsWith("?") -> toShow.add(Pair(token.dropLast(1), token))
        token.startsWith("no") -> optionGroup.unsetToggleOption(getValidToggleOption(token.substring(2), token), scope)
        token.startsWith("inv") -> optionGroup.invertToggleOption(
          getValidToggleOption(token.substring(3), token),
          scope
        )

        token.endsWith("!") -> optionGroup.invertToggleOption(getValidToggleOption(token.dropLast(1), token), scope)
        token.endsWith("&") -> optionGroup.resetDefaultValue(getValidOption(token.dropLast(1), token), scope)
        token.endsWith("<") -> {
          // Copy the global value to the target scope. If the target scope is global, this is a no-op. When copying a
          // string global-local option to effective scope, Vim's behaviour matches setting that option at effective
          // scope. That is, it sets the global value (a no-op) and resets the local value.
          val option = getValidOption(token.dropLast(1), token)
          val globalValue = optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(editor))
          optionGroup.setOptionValue(option, scope, globalValue)
        }
        else -> {
          // `getOption` returns `Option<VimDataType>?`, but we need to treat it as `Option<out VimDataType>?` because
          // `ToggleOption` derives from `Option<out VimDataType>`, and the compiler will complain if the types are
          // different.
          val option: Option<out VimDataType>? = optionGroup.getOption(token)
          when (option) {
            null -> error = Msg.unkopt
            is ToggleOption -> optionGroup.setToggleOption(option, scope)
            else -> toShow.add(Pair(option.name, option.abbrev))
          }
        }
      }
    } else {
      // This must be one of =, :, +=, -=, or ^=
      val eq = token.indexOf('=')
      val colon = token.indexOf(':')
      if (eq > 0 || colon > 0) {
        // Could be option:value, option=value, option+=value, option-=value or option^=value
        val idx = if (eq > 0) eq else colon
        val op = if (eq > 0) token[eq - 1] else Char(0)
        val end = if (eq > 0 && op in "+-^") idx - 1 else idx

        // Get option name and value after operator
        val optionName = token.take(end)
        val option = getValidOption(optionName)
        val existingValue = optionGroup.getOptionValue(option, scope)
        val value = option.parseValue(token.substring(idx + 1), token)
        val newValue = when (op) {
          '+' -> appendValue(option, existingValue, value)
          '^' -> prependValue(option, existingValue, value)
          '-' -> removeValue(option, existingValue, value)
          else -> value
        } ?: throw exExceptionMessage("E474", token)
        optionGroup.setOptionValue(option, scope, newValue)
      } else {
        // We're either missing the equals sign, the colon, or the option name itself
        error = Msg.unkopt
      }
    }
    if (error != null) {
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
}

private fun getValidOption(optionName: String, token: String = optionName) =
  injector.optionGroup.getOption(optionName) ?: throw exExceptionMessage("E518", token)

private fun getValidToggleOption(optionName: String, token: String) =
  getValidOption(optionName, token) as? ToggleOption ?: throw exExceptionMessage("E474", token)

private fun showOptions(
  editor: VimEditor,
  nameAndToken: Collection<Pair<String, String>>,
  scope: OptionAccessScope,
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
        is OptionAccessScope.EFFECTIVE -> appendLine("--- Options ---")
        is OptionAccessScope.LOCAL -> appendLine("--- Local option values ---")
        is OptionAccessScope.GLOBAL -> appendLine("--- Global option values ---")
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

private fun formatKnownOptionValue(option: Option<out VimDataType>, scope: OptionAccessScope): String {
  val value = injector.optionGroup.getOptionValue(option, scope)
  if (option is ToggleOption) {

    // Unset global-local toggle option
    if ((option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
        || option.declaredScope == OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_WINDOW)
      && scope is OptionAccessScope.LOCAL && value == VimInt.MINUS_ONE) {
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
