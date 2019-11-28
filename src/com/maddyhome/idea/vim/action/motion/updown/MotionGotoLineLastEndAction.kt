/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.action.motion.updown

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.option.OptionsManager
import java.util.*

class MotionGotoLineLastEndAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.INCLUSIVE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE, CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(editor: Editor,
                         caret: Caret,
                         context: DataContext,
                         count: Int,
                         rawCount: Int,
                         argument: Argument?): Int {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode) {
      val opt = OptionsManager.selection
      if (opt.value != "old") {
        allow = true
      }
    }

    return VimPlugin.getMotion().moveCaretGotoLineLastEnd(editor, rawCount, count - 1, allow)
  }
}

class MotionGotoLineLastEndInsertAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.EXCLUSIVE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun getOffset(editor: Editor,
                         caret: Caret,
                         context: DataContext,
                         count: Int,
                         rawCount: Int,
                         argument: Argument?): Int {
    var allow = false
    if (editor.inInsertMode) {
      allow = true
    } else if (editor.inVisualMode) {
      val opt = OptionsManager.selection
      if (opt.value != "old") {
        allow = true
      }
    }

    return VimPlugin.getMotion().moveCaretGotoLineLastEnd(editor, rawCount, count - 1, allow)
  }
}
