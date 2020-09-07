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

package com.maddyhome.idea.vim.action.motion.mark

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.MotionActionHandler
import java.util.*

class MotionGotoFileMarkLineAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override val flags: EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_SAVE_JUMP)

  override fun getOffset(editor: Editor,
                         caret: Caret,
                         context: DataContext,
                         count: Int,
                         rawCount: Int,
                         argument: Argument?): Int {
    if (argument == null) return -1

    val mark = argument.character
    return VimPlugin.getMotion().moveCaretToFileMark(editor, mark, false)
  }
}

class MotionGotoFileMarkLineNoSaveJumpAction : MotionActionHandler.ForEachCaret() {
  override val motionType: MotionType = MotionType.LINE_WISE

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun getOffset(editor: Editor,
                         caret: Caret,
                         context: DataContext,
                         count: Int,
                         rawCount: Int,
                         argument: Argument?): Int {
    if (argument == null) return -1

    val mark = argument.character
    return VimPlugin.getMotion().moveCaretToFileMark(editor, mark, true)
  }
}
