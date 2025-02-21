/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.copy

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.put.PutData.TextData

sealed class PutTextBaseAction(
  private val insertTextBeforeCaret: Boolean,
  private val indent: Boolean,
  private val caretAfterInsertedText: Boolean,
) : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val count = operatorArguments.count1
    val sortedCarets = editor.sortedCarets()
    return if (sortedCarets.size > 1) {
      val caretToPutData = sortedCarets.associateWith { getPutDataForCaret(editor, context, it, count) }
      var result = true
      caretToPutData.forEach {
        result = injector.put.putTextForCaret(editor, it.key, context, it.value) && result
      }
      result
    } else {
      val putData = getPutDataForCaret(editor, context, sortedCarets.single(), count)
      injector.put.putText(editor, context, putData)
    }
  }

  private fun getPutDataForCaret(
    editor: VimEditor,
    context: ExecutionContext,
    caret: ImmutableVimCaret,
    count: Int,
  ): PutData {
    val registerService = injector.registerGroup
    val registerChar = if (caret.editor.carets().size == 1) {
      registerService.currentRegister
    } else {
      registerService.getCurrentRegisterForMulticaret()
    }
    val register = caret.registerStorage.getRegister(editor, context, registerChar)
    val textData = register?.let { TextData(register) }
    return PutData(textData, null, count, insertTextBeforeCaret, indent, caretAfterInsertedText, -1)
  }
}

@CommandOrMotion(keys = ["p"], modes = [Mode.NORMAL])
class PutTextAfterCursorAction : PutTextBaseAction(insertTextBeforeCaret = false, indent = true, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["gp"], modes = [Mode.NORMAL])
class PutTextAfterCursorActionMoveCursor : PutTextBaseAction(insertTextBeforeCaret = false, indent = true, caretAfterInsertedText = true)

@CommandOrMotion(keys = ["]p"], modes = [Mode.NORMAL])
class PutTextAfterCursorNoIndentAction : PutTextBaseAction(insertTextBeforeCaret = false, indent = false, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["[P", "]P", "[p"], modes = [Mode.NORMAL])
class PutTextBeforeCursorNoIndentAction : PutTextBaseAction(insertTextBeforeCaret = true, indent = false, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["P"], modes = [Mode.NORMAL])
class PutTextBeforeCursorAction : PutTextBaseAction(insertTextBeforeCaret = true, indent = true, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["gP"], modes = [Mode.NORMAL])
class PutTextBeforeCursorActionMoveCursor : PutTextBaseAction(insertTextBeforeCaret = true, indent = true, caretAfterInsertedText = true)
