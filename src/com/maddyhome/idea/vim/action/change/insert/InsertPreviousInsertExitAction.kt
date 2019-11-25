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
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class InsertPreviousInsertExitAction : ChangeEditorActionHandler.SingleExecution(), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK or KeyEvent.SHIFT_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
  )

  override val type: Command.Type = Command.Type.INSERT

  override fun execute(editor: Editor,
                       context: DataContext,
                       count: Int,
                       rawCount: Int,
                       argument: Argument?): Boolean {
    VimPlugin.getChange().insertPreviousInsert(editor, context, true)
    return false
  }
}
