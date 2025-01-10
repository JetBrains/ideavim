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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.put.PutData

/**
 * @author vlan
 */
sealed class PutVisualTextBaseAction(
  private val insertTextBeforeCaret: Boolean,
  private val indent: Boolean,
  private val caretAfterInsertedText: Boolean,
  private val modifyRegister: Boolean = true,
) : VisualOperatorActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun executeForAllCarets(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    caretsAndSelections: Map<VimCaret, VimSelection>,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (caretsAndSelections.isEmpty()) return false
    val count = cmd.count
    val caretToPutData =
      editor.sortedCarets().associateWith { getPutDataForCaret(editor, context, it, caretsAndSelections[it], count) }
    injector.registerGroup.resetRegister()
    var result = true
    injector.application.runWriteAction {
      caretToPutData.forEach {
        result = injector.put.putTextForCaret(editor, it.key, context, it.value, true, modifyRegister) && result
      }
    }
    return result
  }

  private fun getPutDataForCaret(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    selection: VimSelection?,
    count: Int,
  ): PutData {
    val lastRegisterChar = injector.registerGroup.lastRegisterChar
    val register = caret.registerStorage.getRegister(editor, context, lastRegisterChar)
    val textData = register?.let { PutData.TextData(register) }
    val visualSelection = selection?.let { PutData.VisualSelection(mapOf(caret to it), it.type) }
    return PutData(textData, visualSelection, count, insertTextBeforeCaret, indent, caretAfterInsertedText)
  }
}

@CommandOrMotion(keys = ["P"], modes = [Mode.VISUAL])
class PutVisualTextBeforeCursorAction : PutVisualTextBaseAction(
  insertTextBeforeCaret = true,
  indent = true,
  caretAfterInsertedText = false,
  modifyRegister = false
)

@CommandOrMotion(keys = ["p"], modes = [Mode.VISUAL])
class PutVisualTextAfterCursorAction : PutVisualTextBaseAction(insertTextBeforeCaret = false, indent = true, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["]P", "[P"], modes = [Mode.VISUAL])
class PutVisualTextBeforeCursorNoIndentAction : PutVisualTextBaseAction(insertTextBeforeCaret = true, indent = false, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["[p", "]p"], modes = [Mode.VISUAL])
class PutVisualTextAfterCursorNoIndentAction : PutVisualTextBaseAction(insertTextBeforeCaret = false, indent = false, caretAfterInsertedText = false)

@CommandOrMotion(keys = ["gP"], modes = [Mode.VISUAL])
class PutVisualTextBeforeCursorMoveCursorAction : PutVisualTextBaseAction(insertTextBeforeCaret = true, indent = true, caretAfterInsertedText = true)

@CommandOrMotion(keys = ["gp"], modes = [Mode.VISUAL])
class PutVisualTextAfterCursorMoveCursorAction : PutVisualTextBaseAction(insertTextBeforeCaret = false, indent = true, caretAfterInsertedText = true)
