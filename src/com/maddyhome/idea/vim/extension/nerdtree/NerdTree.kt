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

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.CommandAlias
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus

class NerdTree : VimExtension {
  override fun getName(): String = "NERDTree"

  override fun init() {
    addCommand("NERDTreeFocus", FocusHandler())
  }

  class FocusHandler : CommandAliasHandler {
    override fun execute(editor: Editor, context: DataContext) {
      callAction("ActivateProjectToolWindow", context)
    }
  }

  companion object {
    fun callAction(name: String, context: DataContext) {
      val action = ActionManager.getInstance().getAction(name) ?: run {
        VimPlugin.showMessage(MessageHelper.message("action.not.found.0", name))
        return
      }
      val application = ApplicationManager.getApplication()
      if (application.isUnitTestMode) {
        KeyHandler.executeAction(action, context)
      } else {
        runAfterGotFocus(Runnable { KeyHandler.executeAction(action, context) })
      }
    }

    private fun addCommand(alias: String, handler: CommandAliasHandler) {
      VimPlugin.getCommand().setAlias(alias, CommandAlias.Call(0, -1, alias, handler))
    }
  }
}
