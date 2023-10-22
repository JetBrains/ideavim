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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.GoalCommand
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.InvalidCommandException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.UnknownCommand.Constants.MAX_RECURSION

/**
 * any command with no parser rule. we assume that it is an alias
 */
public data class UnknownCommand(val ranges: Ranges, val name: String, val argument: String) :
  Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  private object Constants {
    const val MAX_RECURSION = 100
  }

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    return processPossiblyAliasCommand("$name $argument", editor, context, MAX_RECURSION)
  }

  private fun processPossiblyAliasCommand(name: String, editor: VimEditor, context: ExecutionContext, aliasCountdown: Int): ExecutionResult {
    if (injector.commandGroup.isAlias(name)) {
      if (aliasCountdown > 0) {
        val commandAlias = injector.commandGroup.getAliasCommand(name, 1)
        when (commandAlias) {
          is GoalCommand.Ex -> {
            if (commandAlias.command.isEmpty()) {
              val message = injector.messages.message(Msg.NOT_EX_CMD, name)
              throw InvalidCommandException(message, null)
            }
            val parsedCommand = injector.vimscriptParser.parseCommand(commandAlias.command)
              ?: throw ExException("E492: Not an editor command: ${commandAlias.command}")
            return if (parsedCommand is UnknownCommand) {
              processPossiblyAliasCommand(commandAlias.command, editor, context, aliasCountdown - 1)
            } else {
              parsedCommand.vimContext = this.vimContext
              parsedCommand.execute(editor, context)
              ExecutionResult.Success
            }
          }
          is GoalCommand.Call -> {
            commandAlias.handler.execute(name, ranges, editor, context)
            return ExecutionResult.Success
          }
        }
      } else {
        injector.messages.showStatusBarMessage(editor, injector.messages.message("recursion.detected.maximum.alias.depth.reached"))
        injector.messages.indicateError()
        return ExecutionResult.Error
      }
    } else {
      throw ExException("E492: Not an editor command: $name")
    }
  }
}
