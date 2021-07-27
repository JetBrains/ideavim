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

package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.handler.IdeActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

class VimEditorBackSpace : IdeActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
  )
  override val type: Command.Type = Command.Type.DELETE
}

class VimEditorDelete : IdeActionHandler(IdeActions.ACTION_EDITOR_DELETE), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
  )
  override val type: Command.Type = Command.Type.DELETE
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

class VimEditorDown : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
  )
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

class VimEditorTab : IdeActionHandler(IdeActions.ACTION_EDITOR_TAB), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
  )
  override val type: Command.Type = Command.Type.INSERT
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

class VimEditorUp : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
  )
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

class VimQuickJavaDoc : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(IdeActions.ACTION_QUICK_JAVADOC, context)
    return true
  }
}
