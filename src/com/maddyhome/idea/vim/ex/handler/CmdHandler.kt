/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.Alias
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler
import com.maddyhome.idea.vim.group.CommandGroup.Companion.BLACKLISTED_ALIASES

/**
 * @author Elliot Courant
 */
class CmdHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  // Static definitions needed for aliases.
  private companion object {
    const val overridePrefix = "!"
    const val argsPrefix = "-nargs"

    const val anyNumberOfArguments = "*"
    const val zeroOrOneArguments = "?"
    const val moreThanZeroArguments = "+"

    const val errorInvalidNumberOfArguments = "E176: Invalid number of arguments"
    const val errorCannotStartWithLowercase = "E183: User defined commands must start with an uppercase letter"
    const val errorReservedName = "E841: Reserved name, cannot be used for user defined command"
    const val errorCommandAlreadyExists = "E174: Command already exists: add ! to replace it"
  }

  override fun execute(cmd: ExCommand) {
    this.addAlias(cmd, null)
  }

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    if (cmd.argument.trim().isEmpty()) {
      return this.listAlias(editor, "")
    }
    return this.addAlias(cmd, editor)
  }

  private fun listAlias(editor: Editor, filter: String): Boolean {
    val lineSeparator = "\n"
    val allAliases = VimPlugin.getCommand().listAliases()
    val aliases = allAliases.filter {
      (filter.isEmpty() || it.key.startsWith(filter))
    }.map {
      "${it.key.padEnd(12)}${it.value.numberOfArguments.padEnd(11)}${it.value.command}"
    }.sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(lineSeparator)
    ExOutputModel.getInstance(editor).output("Name        Args       Definition$lineSeparator$aliases")
    return true
  }

  private fun addAlias(cmd: ExCommand, editor: Editor?): Boolean {
    var argument = cmd.argument.trim()

    // Handle overwriting of aliases
    val overrideAlias = argument.startsWith(overridePrefix)
    if (overrideAlias) {
      argument = argument.removePrefix(overridePrefix).trim()
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
        VimPlugin.showMessage(errorInvalidNumberOfArguments)
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
            VimPlugin.showMessage(errorInvalidNumberOfArguments)
            return false
          }
        }
      } else {
        // Not sure why this isn't documented, but if you try to create a command in vim
        // with an explicit number of arguments greater than 1 it returns this error.
        if (argNum > 1 || argNum < 0) {
          VimPlugin.showMessage(errorInvalidNumberOfArguments)
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
      VimPlugin.showMessage(errorCannotStartWithLowercase)
      return false
    }

    if (alias in BLACKLISTED_ALIASES) {
      VimPlugin.showMessage(errorReservedName)
      return false
    }

    if (argument.isEmpty()) {
      if (editor == null) {
        // If there is no editor then we can't list aliases, just return false.
        // No message should be shown either, since there is no editor.
        return false
      }
      return this.listAlias(editor, alias)
    }

    // If we are not over-writing existing aliases, and an alias with the same command
    // already exists then we want to do nothing.
    if (!overrideAlias && VimPlugin.getCommand().hasAlias(alias)) {
      VimPlugin.showMessage(errorCommandAlreadyExists)
      return false
    }

    // Store the alias and the command. We don't need to parse the argument
    // at this time, if the syntax is wrong an error will be returned when
    // the alias is executed.
    VimPlugin.getCommand().setAlias(alias, Alias(minNumberOfArgs, maxNumberOfArgs, alias, argument))

    return true
  }
}
