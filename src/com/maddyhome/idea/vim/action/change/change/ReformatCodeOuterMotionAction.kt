/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.action.change.change;

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.action.DuplicableOperatorAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler

class ReformatPreservingCursorAction : ChangeEditorActionHandler.ForEachCaret(), DuplicableOperatorAction {
  override val type: Command.Type = Command.Type.CHANGE

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override val duplicateWith: Char = 'w'

  override fun execute(editor: Editor,
                       caret: Caret,
                       context: DataContext,
                       count: Int,
                       rawCount: Int,
                       argument: Argument?): Boolean {
    if (argument == null) {
      return false
    }

    val range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument) ?: return false
    val action = argument.motion.action

    return if (action.id == "VimMotionOuterParagraphAction" && action.flags.contains(CommandFlags.FLAG_TEXT_BLOCK)) {
      reformatParagraphPreservingCursor(editor, caret, range)
    } else {
      false
    }
  }

  private fun reformatParagraphPreservingCursor(editor: Editor,
                                                caret: Caret,
                                                range: TextRange): Boolean {
    val newCaretOffset: Int = ReformatCodeMotionAction.reformatParagraph(editor, caret, range)
    MotionGroup.moveCaret(editor, caret, newCaretOffset)
    return true
  }
}
