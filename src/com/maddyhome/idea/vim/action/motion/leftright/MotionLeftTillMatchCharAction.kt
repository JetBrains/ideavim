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
package com.maddyhome.idea.vim.action.motion.leftright

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class MotionLeftTillMatchCharAction : MotionActionHandler.ForEachCaret() {
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_ALLOW_DIGRAPH)

  override fun getOffset(editor: Editor,
                         caret: Caret,
                         context: DataContext,
                         count: Int,
                         rawCount: Int,
                         argument: Argument?): Int {
    if (argument == null) {
      VimPlugin.indicateError()
      return caret.offset
    }
    val res = VimPlugin.getMotion().moveCaretToBeforeNextCharacterOnLine(editor, caret, -count, argument.character)
    VimPlugin.getMotion().setLastFTCmd(MotionGroup.LAST_T, argument.character)
    return res
  }

  override val motionType: MotionType = MotionType.EXCLUSIVE
}
