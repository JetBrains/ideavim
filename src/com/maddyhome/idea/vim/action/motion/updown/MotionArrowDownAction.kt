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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.motion.updown

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.NonShiftedSpecialKeyHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.vimLastColumn
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

private object MotionArrowDownActionHandler : NonShiftedSpecialKeyHandler() {
  private var col: Int = 0

  override fun offset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
    return VimPlugin.getMotion().moveCaretVertical(editor, caret, count)
  }

  override fun preOffsetComputation(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    col = caret.vimLastColumn
    return true
  }

  override fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
    val pos = caret.visualPosition
    val lastColumn = EditorHelper.lastColumnForLine(editor, pos.line, editor.mode.isEndAllowed)
    caret.vimLastColumn = if (pos.column != lastColumn) pos.column else col
  }
}

class MotionArrowDownAction : VimCommandAction(MotionArrowDownActionHandler) {
  override val mappingModes: Set<MappingMode> = MappingMode.NVOS

  override val keyStrokesSet: Set<MutableList<KeyStroke>> = setOf(parseKeys("<Down>"), mutableListOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0)))

  override val type: Command.Type = Command.Type.MOTION

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE)
}
