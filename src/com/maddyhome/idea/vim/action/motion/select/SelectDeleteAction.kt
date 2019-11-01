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

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.exitSelectMode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

class SelectDeleteAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.INSERT

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
    val actions = VimPlugin.getKey().getActions(editor.component, enterKeyStroke)
    for (action in actions) {
      if (KeyHandler.executeAction(action, context)) {
        break
      }
    }
    editor.exitSelectMode(true)
    VimPlugin.getChange().insertBeforeCursor(editor, context)
    return true
  }
}
