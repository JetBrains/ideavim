/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.option.OptionsManager
import java.util.*

class MotionLastColumnInsertAction : MotionLastColumnAction() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

open class MotionLastColumnAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.INCLUSIVE

  override fun getOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Motion {
    val allow = if (editor.inVisualMode) {
      val opt = OptionsManager.selection
      opt.value != "old"
    } else {
      if (editor.commandState.isOperatorPending) false else editor.isEndAllowed
    }

    return VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, allow).toMotion()
  }

  override fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    caret.vimLastColumn = MotionGroup.LAST_COLUMN
  }

  override fun preMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    caret.vimLastColumn = MotionGroup.LAST_COLUMN
  }
}
