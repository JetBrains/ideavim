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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Access.READ_ONLY
import com.maddyhome.idea.vim.ex.CommandHandler.ArgumentFlag.ARGUMENT_OPTIONAL
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.SAVE_VISUAL
import com.maddyhome.idea.vim.ex.CommandHandler.RangeFlag.RANGE_OPTIONAL
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.runAfterGotFocus

/**
 * @author smartbomb
 */
class ActionHandler : CommandHandler.SingleExecution() {

  override val argFlags: CommandHandlerFlags = flags(RANGE_OPTIONAL, ARGUMENT_OPTIONAL, READ_ONLY, SAVE_VISUAL)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val actionName = cmd.argument.trim()
    val action = ActionManager.getInstance().getAction(actionName) ?: run {
      VimPlugin.showMessage(MessageHelper.message("action.not.found.0", actionName))
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
