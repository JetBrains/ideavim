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

package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author Alex Plate
 */
// FIXME: 2019-07-05 Workaround to make jump through methods work
class CtrlDownAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Down>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val keyStroke = keySet.first().first()
    val actions = VimPlugin.getKey().getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (KeyHandler.executeAction(action, context)) break
    }
    return true
  }
}

class CtrlUpAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Up>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val keyStroke = keySet.first().first()
    val actions = VimPlugin.getKey().getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (KeyHandler.executeAction(action, context)) break
    }
    return true
  }
}
