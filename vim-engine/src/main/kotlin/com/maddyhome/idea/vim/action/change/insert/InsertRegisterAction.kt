/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.vimscript.model.Script

class InsertRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument

    if (argument?.character == '=') {
      injector.application.invokeLater {
        try {
          val expression = readExpression(editor)
          if (expression != null) {
            if (expression.isNotEmpty()) {
              val expressionValue =
                injector.vimscriptParser.parseExpression(expression)?.evaluate(editor, context, Script(listOf()))
                  ?: throw ExException("E15: Invalid expression: $expression")
              val textToStore = expressionValue.toInsertableString()
              injector.registerGroup.storeTextSpecial('=', textToStore)
            }
            injector.changeGroup.insertRegister(editor, context, argument.character)
          }
        } catch (e: ExException) {
          injector.messages.indicateError()
          injector.messages.showStatusBarMessage(e.message)
        }
      }
      return true
    } else {
      return argument != null && injector.changeGroup.insertRegister(editor, context, argument.character)
    }
  }

  private fun readExpression(editor: VimEditor): String? {
    return injector.commandLineHelper.inputString(editor, "=", null)
  }
}
