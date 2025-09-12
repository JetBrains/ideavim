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
import com.maddyhome.idea.vim.api.setToggleOption
import com.maddyhome.idea.vim.api.unsetToggleOption
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
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
data class SetCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  SetCommandBase(range, modifier, argument) {

  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.EFFECTIVE(editor)
}

@ExCommand(command = "setg[lobal]")
data class SetglobalCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  SetCommandBase(range, modifier, argument) {

  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.GLOBAL(editor)
}

@ExCommand(command = "setl[ocal]")
data class SetlocalCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  SetCommandBase(range, modifier, argument) {

  override fun getScope(editor: VimEditor): OptionAccessScope = OptionAccessScope.LOCAL(editor)
}

abstract class SetCommandBase(range: Range, modifier: CommandModifier, argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    parseOptionLine(editor, context, commandModifier, commandArgument, getScope(editor))
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
 * @param argument      The raw text passed to the `:set` command
 * @throws ExException Thrown if any option names or operations are incorrect
 */
fun parseOptionLine(
  editor: VimEditor,
  context: ExecutionContext,
  commandModifier: CommandModifier,
  argument: String,
  scope: OptionAccessScope,
) {
  val optionGroup = injector.optionGroup

  val columnFormat = commandModifier == CommandModifier.BANG

  when {
    argument.isEmpty() -> {
      // No arguments mean we show only changed values
      val changedOptions = optionGroup.getAllOptions()
        .filter {
          !optionGroup.isDefaultValue(
            it,
            scope
          ) && (!it.isHidden || (injector.application.isInternal() && !injector.application.isUnitTest()))
        }
        .map { Pair(it.name, it.name) }
      showOptions(editor, context, changedOptions, scope, true, columnFormat)
      return
    }

    argument == "all" -> {
      val options = optionGroup.getAllOptions()
        .filter { !it.isHidden || (injector.application.isInternal() && !injector.application.isUnitTest()) }
        .map { Pair(it.name, it.name) }
      showOptions(editor, context, options, scope, true, columnFormat)
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
  val tokenizer = StringTokenizer(argument)
  val toShow = mutableListOf<Pair<String, String>>()
  while (tokenizer.hasMoreTokens()) {
    var token = tokenizer.nextToken()
    // See if a space has been backslashed, if not get the rest of the text
    while (token.endsWith("\\")) {
      token = token.take(token.length - 1) + ' '
      if (tokenizer.hasMoreTokens()) {
        token += tokenizer.nextToken()
      }
    }

    val eq = token.indexOf('=')
    val colon = token.indexOf(':')
    val isKeyValueOperation = eq != -1 || colon != -1
    if (!isKeyValueOperation) {
      when {
        token.endsWith("?") -> toShow.add(Pair(token.dropLast(1), token))
        token.startsWith("no") -> optionGroup.unsetToggleOption(getValidToggleOption(token.substring(2), token), scope)
        token.startsWith("inv") -> optionGroup.invertToggleOption(
          getValidToggleOption(token.substring(3), token),
          scope
        )

        token.endsWith("!") -> optionGroup.invertToggleOption(getValidToggleOption(token.dropLast(1), token), scope)
        token.endsWith("&") -> optionGroup.resetToDefaultValue(getValidOption(token.dropLast(1), token), scope)
        token.endsWith("<") -> optionGroup.resetToGlobalValue(getValidOption(token.dropLast(1), token), scope, editor)
        else -> {
          // `getOption` returns `Option<VimDataType>?`, but we need to treat it as `Option<out VimDataType>?` because
          // `ToggleOption` derives from `Option<out VimDataType>`, and the compiler will complain if the types are
          // different.
          val option: Option<out VimDataType>? = optionGroup.getOption(token)
          when (option) {
            null -> error = injector.messages.message("E518", token)
            is ToggleOption -> optionGroup.setToggleOption(option, scope)
            else -> toShow.add(Pair(option.name, option.abbrev))
          }
        }
      }
    } else {
      // This must be one of =, :, +=, -=, or ^=
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
      } ?: throw exExceptionMessage("E474.arg", token)
      optionGroup.setOptionValue(option, scope, newValue)
    }
    if (error != null) {
      break
    }
  }

  // Now show all options that were individually requested
  if (toShow.isNotEmpty()) {
    showOptions(editor, context, toShow, scope, false, columnFormat)
  }

  if (error != null) {
    throw ExException(error)
  }
}

private fun getValidOption(optionName: String, token: String = optionName) =
  injector.optionGroup.getOption(optionName) ?: throw exExceptionMessage("E518", token)

private fun getValidToggleOption(optionName: String, token: String) =
  getValidOption(optionName, token) as? ToggleOption ?: throw exExceptionMessage("E474.arg", token)

private fun showOptions(
  editor: VimEditor,
  context: ExecutionContext,
  nameAndToken: Collection<Pair<String, String>>,
  scope: OptionAccessScope,
  showIntro: Boolean,
  columnFormat: Boolean,
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

  val width = injector.engineEditorHelper.getApproximateOutputPanelWidth(editor).let { if (it < 20) 80 else it }
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
          repeat(lengthAtStartOfLine + (c * colWidth) - length) { append(' ') }
          append(cells[index])
        }
      }
      appendLine()
    }

    // Add any lines that are too long to fit into columns. The panel will soft-wrap text
    for (option in extra) {
      appendLine(option)
    }
  }
  injector.outputPanel.output(editor, context, output)

  if (unknownOption != null) {
    throw exExceptionMessage("E518", unknownOption.second)
  }
}

private fun formatKnownOptionValue(option: Option<out VimDataType>, scope: OptionAccessScope): String {
  if (option is ToggleOption) {
    val value = injector.optionGroup.getOptionValue(option, scope)

    // Unset global-local toggle option
    if (option.declaredScope.isGlobalLocal() && scope is OptionAccessScope.LOCAL && value == VimInt.MINUS_ONE) {
      return "--${option.name}"
    }

    return if (value.booleanValue) "  ${option.name}" else "no${option.name}"
  } else {
    val value = injector.optionGroup.getOptionValue(option, scope)
    return "  ${option.name}=${value.toOutputString()}"
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
