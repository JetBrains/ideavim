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

package com.maddyhome.idea.vim.action.motion.leftright

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.ShiftedArrowKeyHandler
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.newapi.ij

/**
 * @author Alex Plate
 */

class MotionShiftRightAction : ShiftedArrowKeyHandler() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun motionWithKeyModel(editor: VimEditor, context: ExecutionContext, cmd: Command) {
    editor.ij.vimForEachCaret { caret ->
      val vertical = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor.ij, caret, cmd.count, true)
      MotionGroup.moveCaret(editor.ij, caret, vertical)
    }
  }

  override fun motionWithoutKeyModel(editor: VimEditor, context: ExecutionContext, cmd: Command) {
    editor.ij.vimForEachCaret { caret ->
      val newOffset = VimPlugin.getMotion().findOffsetOfNextWord(editor.ij, caret.offset, cmd.count, false)
      if (newOffset is Motion.AbsoluteOffset) {
        MotionGroup.moveCaret(editor.ij, caret, newOffset.offset)
      }
    }
  }
}
