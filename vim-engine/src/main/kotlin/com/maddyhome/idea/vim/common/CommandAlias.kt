/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.Range
import org.jetbrains.annotations.NonNls

/**
 * @author Elliot Courant
 */
sealed class CommandAlias(
  protected val minimumNumberOfArguments: Int,
  protected val maximumNumberOfArguments: Int,
  val name: String,
) {

  class Ex(
    minimumNumberOfArguments: Int,
    maximumNumberOfArguments: Int,
    name: String,
    val command: String,
  ) : CommandAlias(minimumNumberOfArguments, maximumNumberOfArguments, name) {
    override fun getCommand(input: String, count: Int): GoalCommand {
      if (this.minimumNumberOfArguments == 0 && this.maximumNumberOfArguments == 0) {
        return GoalCommand.Ex(this.command)
      }
      var compiledCommand = this.command
      val cleanedInput = input.trim().removePrefix(name).trim()
      if (minimumNumberOfArguments > 0 && cleanedInput.isEmpty()) {
        injector.messages.showErrorMessage(editor = null, injector.messages.message("E471"))
        injector.messages.indicateError()
        return GoalCommand.Ex.EMPTY
      }
      for (symbol in arrayOf(Count, Arguments, QuotedArguments)) {
        compiledCommand = compiledCommand.replace(
          symbol,
          when (symbol) {
            Count -> arrayOf(count.toString())
            Arguments -> arrayOf(cleanedInput)
            QuotedArguments -> arrayOf("'$cleanedInput'")
            else -> emptyArray()
          }.joinToString(", "),
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

  class Call(
    minimumNumberOfArguments: Int,
    maximumNumberOfArguments: Int,
    name: String,
    val handler: CommandAliasHandler,
  ) : CommandAlias(minimumNumberOfArguments, maximumNumberOfArguments, name) {
    override fun getCommand(input: String, count: Int): GoalCommand {
      return GoalCommand.Call(handler)
    }

    override fun printValue(): String = handler.javaClass.toString()
  }

  val numberOfArguments: String =
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
      val EMPTY: Ex = Ex("")
    }
  }

  class Call(val handler: CommandAliasHandler) : GoalCommand()
}

interface CommandAliasHandler {
  fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext)
}
