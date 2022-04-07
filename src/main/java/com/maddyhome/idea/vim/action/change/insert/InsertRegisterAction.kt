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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.CommandLineHelper
import com.maddyhome.idea.vim.newapi.IjExecutionContext
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser

class InsertRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val argument = cmd.argument

    if (argument?.character == '=') {
      ApplicationManager.getApplication().invokeLater {
        try {
          val expression = readExpression(editor.ij)
          if (expression != null) {
            if (expression.isNotEmpty()) {
              val expressionValue = VimscriptParser.parseExpression(expression)?.evaluate((editor as IjVimEditor).editor, (context as IjExecutionContext).context, Script(listOf()))
                ?: throw ExException("E15: Invalid expression: $expression")
              val textToStore = expressionValue.toInsertableString()
              VimPlugin.getRegister().storeTextSpecial('=', textToStore)
            }
            VimPlugin.getChange().insertRegister(editor, context, argument.character)
          }
        } catch (e: ExException) {
          VimPlugin.indicateError()
          VimPlugin.showMessage(e.message)
        }
      }
      return true
    } else {
      return argument != null && VimPlugin.getChange().insertRegister(editor, context, argument.character)
    }
  }

  private fun readExpression(editor: Editor): String? {
    return service<CommandLineHelper>().inputString(editor, "=", null)
  }
}
