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
    deleteTo = Offset(injector.motion.moveCaretToLineStartSkipLeading(editor, caret))
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
