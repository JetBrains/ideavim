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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.Script

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
    val argument = cmd.argument as? Argument.Character ?: return false
    if (argument.character == '=') {
      injector.commandLine.readInputAndProcess(editor, context, "=", finishOn = null) { input ->
        try {
          if (input.isNotEmpty()) {
            val expression =
              injector.vimscriptParser.parseExpression(input)?.evaluate(editor, context, Script(listOf()))
                ?: throw exExceptionMessage("E15", input)
            val textToStore = expression.toInsertableString()
            injector.registerGroup.storeTextSpecial('=', textToStore)
          }
          insertRegister(editor, context, '=')
        } catch (e: ExException) {
          injector.messages.indicateError()
          injector.messages.showStatusBarMessage(editor, e.message)
        }
      }
      return true
    } else {
      return insertRegister(editor, context, argument.character)
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
@VimLockLabel.SelfSynchronized
private fun insertRegister(editor: VimEditor, context: ExecutionContext, key: Char): Boolean {
  val register: Register? = injector.registerGroup.getRegister(editor, context, key)
  if (register != null) {
    val textData = PutData.TextData(
      register.name,
      injector.clipboardManager.dumbCopiedText(register.text),
      SelectionType.CHARACTER_WISE
    )
    val putData =
      PutData(textData, null, 1, insertTextBeforeCaret = true, rawIndent = true, caretAfterInsertedText = true)
    injector.put.putText(editor, context, putData)
    return true
  }
  return false
}
