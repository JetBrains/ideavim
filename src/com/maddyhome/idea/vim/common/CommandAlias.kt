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

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.MessageHelper
import org.jetbrains.annotations.NonNls

/**
 * @author Elliot Courant
 */
sealed class CommandAlias(
  protected val minimumNumberOfArguments: Int,
  protected val maximumNumberOfArguments: Int,
  val name: String
) {

  class Ex(
    minimumNumberOfArguments: Int,
    maximumNumberOfArguments: Int,
    name: String,
    val command: String
  ) : CommandAlias(minimumNumberOfArguments, maximumNumberOfArguments, name) {
    override fun getCommand(input: String, count: Int): GoalCommand {
      if (this.maximumNumberOfArguments == 0 && this.maximumNumberOfArguments == 0) {
        return GoalCommand.Ex(this.command)
      }
      var compiledCommand = this.command
      val cleanedInput = input.trim().removePrefix(name).trim()
      if (minimumNumberOfArguments > 0 && cleanedInput.isEmpty()) {
        VimPlugin.showMessage(MessageHelper.message("e471.argument.required"))
        VimPlugin.indicateError()
        return GoalCommand.Ex.EMPTY
      }
      for (symbol in arrayOf(Count, Arguments, QuotedArguments)) {
        compiledCommand = compiledCommand.replace(
          symbol, when (symbol) {
            Count -> arrayOf(count.toString())
            Arguments -> arrayOf(cleanedInput)
            QuotedArguments -> arrayOf("'$cleanedInput'")
            else -> emptyArray()
          }.joinToString(", ")
        )
      }

      // We want to escape <lt> after we've dropped in all of our args, if they are
      // using <lt> its because they are escaping something that we don't want to handle
      // yet.
      compiledCommand = compiledCommand.replace(LessThan, "<")

      return GoalCommand.Ex(compiledCommand)
    }

    override fun printValue(): String = command
  }

  val numberOfArguments =
    when {
      this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == 0 -> "0" // No arguments
      this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == -1 -> "*" // Any number of arguments
      this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == 1 -> "?" // Zero or one argument
      this.minimumNumberOfArguments == 1 && this.maximumNumberOfArguments == -1 -> "+" // One or more arguments
      else -> this.minimumNumberOfArguments.toString() // Specified number of arguments
    }

  abstract fun getCommand(input: String, count: Int): GoalCommand

  abstract fun printValue(): String

  private companion object {
    @NonNls
    const val LessThan = "<lt>"
    @NonNls
    const val Count = "<count>"
    @NonNls
    const val Arguments = "<args>"
    @NonNls
    const val QuotedArguments = "<q-args>"
  }
}

sealed class GoalCommand {
  class Ex(val command: String) : GoalCommand() {
    companion object {
      val EMPTY = Ex("")
    }
  }
}
