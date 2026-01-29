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
import com.maddyhome.idea.vim.api.VimCommandGroup.Companion.BLACKLISTED_ALIASES
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author Elliot Courant
 * see "h :command"
 */
@ExCommand(command = "com[mand]")
data class CmdCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  private val unsupportedArgs = listOf(
    Regex("-range(=[^ ])?") to "-range",
    Regex("-complete=[^ ]*") to "-complete",
    Regex("-count=[^ ]*") to "-count",
    Regex("-addr=[^ ]*") to "-addr",
    Regex("-bang") to "-bang",
    Regex("-bar") to "-bar",
    Regex("-register") to "-register",
    Regex("-buffer") to "-buffer",
    Regex("-keepscript") to "-keepscript",
  )

  // Static definitions needed for aliases.
  private companion object {
    @VimNlsSafe
    const val argsPrefix = "-nargs"

    const val anyNumberOfArguments = "*"
    const val zeroOrOneArguments = "?"
    const val moreThanZeroArguments = "+"
  }

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val result: Boolean = if (argument.trim().isEmpty()) {
      this.listAlias(editor, context, "")
    } else {
      this.addAlias(editor, context)
    }
    return if (result) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun listAlias(editor: VimEditor, context: ExecutionContext, filter: String): Boolean {
    val lineSeparator = "\n"
    val allAliases = injector.commandGroup.listAliases()
    val aliases = allAliases.filter {
      (filter.isEmpty() || it.key.startsWith(filter))
    }.map {
      "${it.key.padEnd(12)}${it.value.numberOfArguments.padEnd(11)}${it.value.printValue()}"
    }.sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(lineSeparator)
    injector.outputPanel.output(editor, context, "Name        Args       Definition$lineSeparator$aliases")
    return true
  }

  private fun addAlias(editor: VimEditor, context: ExecutionContext): Boolean {
    var argument = argument.trim()

    // Handle overwriting of aliases
    val overrideAlias = modifier == CommandModifier.BANG

    for ((arg, message) in unsupportedArgs) {
      val match = arg.find(argument)
      match?.range?.let {
        argument = argument.removeRange(it)
        injector.messages.showErrorMessage(editor, "'$message' is not supported by `command`")
      }
    }

    // Handle alias arguments
    val hasArguments = argument.startsWith(argsPrefix)
    var minNumberOfArgs = 0
    var maxNumberOfArgs = 0
    if (hasArguments) {
      // Extract the -nargs that's part of this execution, it's possible that -nargs is
      // in the actual alias being created, and we don't want to parse that one.
      val trimmedInput = argument.takeWhile { it != ' ' }
      val pattern = Regex("(?>-nargs=((|[-])\\d+|[?]|[+]|[*]))").find(trimmedInput) ?: run {
        injector.messages.showErrorMessage(editor, injector.messages.message("E176"))
        return false
      }
      val nargForTrim = pattern.groupValues[0]
      val argumentValue = pattern.groups[1]!!.value
      val argNum = argumentValue.toIntOrNull()
      if (argNum == null) { // If the argument number is null then it is not a number.
        // Make sure the argument value is a valid symbol that we can handle.
        when (argumentValue) {
          anyNumberOfArguments -> {
            minNumberOfArgs = 0
            maxNumberOfArgs = -1
          }

          zeroOrOneArguments -> maxNumberOfArgs = 1
          moreThanZeroArguments -> {
            minNumberOfArgs = 1
            maxNumberOfArgs = -1
          }

          else -> {
            // Technically this should never be reached, but is here just in case
            // I missed something, since the regex limits the value to be ? + * or
            // a valid number, its not possible (as far as I know) to have another value
            // that regex would accept that is not valid.
            injector.messages.showErrorMessage(
              editor,
              injector.messages.message("E176")
            )
            return false
          }
        }
      } else {
        // Not sure why this isn't documented, but if you try to create a command in vim
        // with an explicit number of arguments greater than 1 it returns this error.
        if (argNum > 1 || argNum < 0) {
          injector.messages.showErrorMessage(editor, injector.messages.message("E176"))
          return false
        }
        minNumberOfArgs = argNum
        maxNumberOfArgs = argNum
      }
      argument = argument.removePrefix(nargForTrim).trim()
    }

    // We want to trim off any "!" at the beginning of the arguments.
    // This will also remove any extra spaces.
    argument = argument.trim()

    // We want to get the first character sequence in the arguments.
    // eg. command! Wq wq
    // We want to extract the Wq only, and then just use the rest of
    // the argument as the alias result.
    val alias = argument.split(" ")[0]
    argument = argument.removePrefix(alias).trim()

    // User-aliases need to begin with an uppercase character.
    if (!alias[0].isUpperCase()) {
      injector.messages.showErrorMessage(
        editor,
        injector.messages.message("E183")
      )
      return false
    }

    if (alias in BLACKLISTED_ALIASES) {
      injector.messages.showErrorMessage(
        editor,
        injector.messages.message("E841")
      )
      return false
    }

    if (argument.isEmpty()) {
      return this.listAlias(editor, context, alias)
    }

    // If we are not over-writing existing aliases, and an alias with the same command
    // already exists then we want to do nothing.
    if (!overrideAlias && injector.commandGroup.hasAlias(alias)) {
      injector.messages.showErrorMessage(
        editor,
        injector.messages.message("E174")
      )
      return false
    }

    // Store the alias and the command. We don't need to parse the argument
    // at this time, if the syntax is wrong an error will be returned when
    // the alias is executed.
    injector.commandGroup.setAlias(alias, CommandAlias.Ex(minNumberOfArgs, maxNumberOfArgs, alias, argument))

    return true
  }
}
