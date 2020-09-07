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

package com.maddyhome.idea.vim.action.motion.leftright

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.ShiftedArrowKeyHandler
import com.maddyhome.idea.vim.helper.vimForEachCaret

/**
 * @author Alex Plate
 */

class MotionShiftLeftAction : ShiftedArrowKeyHandler() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motionWithKeyModel(editor: Editor, context: DataContext, cmd: Command) {
    editor.vimForEachCaret { caret ->
      val vertical = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -cmd.count, true)
      MotionGroup.moveCaret(editor, caret, vertical)
    }
  }

  override fun motionWithoutKeyModel(editor: Editor, context: DataContext, cmd: Command) {
    editor.vimForEachCaret { caret ->
      val newOffset = VimPlugin.getMotion().findOffsetOfNextWord(editor, caret.offset, -cmd.count, false)
      MotionGroup.moveCaret(editor, caret, newOffset)
    }
  }
}
