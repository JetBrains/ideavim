/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class InsertDeleteInsertedTextAction : ChangeEditorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return insertDeleteInsertedText(editor, caret, operatorArguments)
  }
}

/**
 * If the cursor is currently after the start of the current insert this deletes all the newly inserted text.
 * Otherwise it deletes all text from the cursor back to the first non-blank in the line.
 *
 * @param editor The editor to delete the text from
 * @param caret  The caret on which the action is performed
 * @return true if able to delete the text, false if not
 */
private fun insertDeleteInsertedText(
  editor: VimEditor,
  caret: VimCaret,
  operatorArguments: OperatorArguments,
): Boolean {
  var deleteTo = caret.vimInsertStart.startOffset
  val offset = caret.offset
  if (offset == deleteTo) {
    deleteTo = Offset(injector.motion.moveCaretToCurrentLineStartSkipLeading(editor, caret))
  }
  if (deleteTo.point != -1) {
    injector.changeGroup.deleteRange(
      editor,
      caret,
      TextRange(deleteTo.point, offset.point),
      SelectionType.CHARACTER_WISE,
      false,
      operatorArguments
    )
    return true
  }
  return false
}
