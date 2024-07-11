/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.RWLockLabel
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptorBase
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.Script
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<C-R>"], modes = [Mode.INSERT])
class InsertRegisterAction : VimActionHandler.SingleExecution() {
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
      injector.modalInput.create(editor, context, "=", ExpressionRegisterInputInterceptor(operatorArguments))
      return true
    } else {
      return argument != null && insertRegister(editor, context, argument.character, operatorArguments)
    }
  }

  private class ExpressionRegisterInputInterceptor(val operatorArguments: OperatorArguments) : VimInputInterceptorBase<String>() {
    override fun buildInput(key: KeyStroke): String? {
      val modalInput = injector.modalInput.getCurrentModalInput() ?: return ""
      if (key.isCloseKeyStroke() || key.keyCode == KeyEvent.VK_ENTER) {
        closeModalInputPrompt()
        return modalInput.text
      }
      modalInput.typeText(injector.parser.toPrintableString(listOf(key)))
      return null
    }

    override fun executeInput(input: String, editor: VimEditor, context: ExecutionContext) {
      try {
        if (input.isNotEmpty()) {
          val expression = injector.vimscriptParser.parseExpression(input)?.evaluate(editor, context, Script(listOf()))
            ?: throw ExException("E15: Invalid expression: $input")
          val textToStore = expression.toInsertableString()
          injector.registerGroup.storeTextSpecial('=', textToStore)
        }
        insertRegister(editor, context, '=', operatorArguments)
      } catch (e: ExException) {
        injector.messages.indicateError()
        injector.messages.showStatusBarMessage(editor, e.message)
      }
    }
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
