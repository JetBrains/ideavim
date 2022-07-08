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
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.register.Register
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
            insertRegister(editor, context, argument.character, operatorArguments)
          }
        } catch (e: ExException) {
          injector.messages.indicateError()
          injector.messages.showStatusBarMessage(e.message)
        }
      }
      return true
    } else {
      return argument != null && insertRegister(editor, context, argument.character, operatorArguments)
    }
  }

  private fun readExpression(editor: VimEditor): String? {
    return injector.commandLineHelper.inputString(editor, "=", null)
  }
}

/**
 * Inserts the contents of the specified register
 *
 * @param editor  The editor to insert the text into
 * @param context The data context
 * @param key     The register name
 * @return true if able to insert the register contents, false if not
 */
private fun insertRegister(
  editor: VimEditor,
  context: ExecutionContext,
  key: Char,
  operatorArguments: OperatorArguments
): Boolean {
  val register: Register? = injector.registerGroup.getRegister(key)
  if (register != null) {
    val text = register.rawText ?: injector.parser.toPrintableString(register.keys)
    val textData = PutData.TextData(text, SelectionType.CHARACTER_WISE, emptyList())
    val putData = PutData(textData, null, 1, insertTextBeforeCaret = true, rawIndent = true, caretAfterInsertedText = true)
    injector.put.putText(editor, context, putData, operatorArguments = operatorArguments)
    return true
  }
  return false
}
