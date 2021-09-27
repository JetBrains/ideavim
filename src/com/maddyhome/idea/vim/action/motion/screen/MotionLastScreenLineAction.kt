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
package com.maddyhome.idea.vim.action.motion.screen

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.vimLine
import java.util.*

/*
                                                *L*
L                       To line [count] from bottom of window (default: Last
                        line on the window) on the first non-blank character
                        |linewise|.  See also 'startofline' option.
                        Cursor is adjusted for 'scrolloff' option, unless an
                        operator is pending, in which case the text may
                        scroll.  E.g. "yL" yanks from the cursor to the last
                        visible line.
 */
abstract class MotionLastScreenLineActionBase(private val operatorPending: Boolean) : MotionActionHandler.ForEachCaret() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override val motionType: MotionType = MotionType.LINE_WISE

  override fun getOffset(
    editor: Editor,
    caret: Caret,
    context: DataContext,
    count: Int,
    rawCount: Int,
    argument: Argument?,
  ): Motion {
    return VimPlugin.getMotion().moveCaretToLastScreenLine(editor, caret, count, !operatorPending).toMotion()
  }

  override fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    if (operatorPending) {
      // Convert current caret line from a 0-based logical line to a 1-based logical line
      VimPlugin.getMotion().scrollLineToFirstScreenLine(editor, caret.vimLine, false)
    }
  }
}

class MotionLastScreenLineAction : MotionLastScreenLineActionBase(false)
class MotionOpPendingLastScreenLineAction : MotionLastScreenLineActionBase(true)
