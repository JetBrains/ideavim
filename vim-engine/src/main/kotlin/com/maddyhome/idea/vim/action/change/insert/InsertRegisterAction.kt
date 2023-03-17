/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
import com.maddyhome.idea.vim.helper.RWLockLabel
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.vimscript.model.Script

public class InsertRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

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
          injector.messages.showStatusBarMessage(editor, e.message)
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
@RWLockLabel.SelfSynchronized
private fun insertRegister(
  editor: VimEditor,
  context: ExecutionContext,
  key: Char,
  operatorArguments: OperatorArguments,
): Boolean {
  val register: Register? = injector.registerGroup.getRegister(key)
  if (register != null) {
    val text = register.rawText ?: injector.parser.toPrintableString(register.keys)
    val textData = PutData.TextData(text, SelectionType.CHARACTER_WISE, emptyList(), register.name)
    val putData = PutData(textData, null, 1, insertTextBeforeCaret = true, rawIndent = true, caretAfterInsertedText = true)
    injector.put.putText(editor, context, putData, operatorArguments = operatorArguments)
    return true
  }
  return false
}
