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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.SAVE_VISUAL
import com.maddyhome.idea.vim.helper.runAfterGotFocus

/**
 * @author smartbomb
 */
class ActionHandler : CommandHandler.SingleExecution() {

  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY, SAVE_VISUAL)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val actionName = cmd.argument.trim()
    val action = ActionManager.getInstance().getAction(actionName) ?: run {
      VimPlugin.showMessage("Action not found: $actionName")
      return false
    }
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode) {
      executeAction(action, context)
    } else {
      runAfterGotFocus(Runnable { executeAction(action, context) })
    }
    return true
  }

  private fun executeAction(action: AnAction, context: DataContext) {
    KeyHandler.executeAction(action, context)
  }
}
