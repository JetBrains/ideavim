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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.DONT_REOPEN
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import com.maddyhome.idea.vim.helper.runAfterGotFocus

/**
 * @author smartbomb
 */
class ActionHandler : CommandHandler(
        commands("action"),
        flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, DONT_REOPEN)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        val actionName = cmd.argument.trim()
        val action = ActionManager.getInstance().getAction(actionName) ?: run {
            VimPlugin.showMessage("Action not found: $actionName")
            return false
        }
        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
            executeAction(editor, cmd, action, context, actionName)
        } else {
            runAfterGotFocus(Runnable { executeAction(editor, cmd, action, context, actionName) })
        }
        return true
    }

    private fun executeAction(editor: Editor, cmd: ExCommand, action: AnAction,
                              context: DataContext, actionName: String) {
        val visualAction = cmd.ranges.size() > 0
        if (visualAction) {
            // FIXME: 2019-03-05 use '< and '> marks
            VisualMotionGroup.selectPreviousVisualMode(editor)
        }
        try {
            KeyHandler.executeAction(action, context)
        } catch (e: RuntimeException) {
            assert(false) { "Error while executing :action $actionName ($action): $e" }
        } finally {
            if (visualAction) {
                // Exit visual mode selected above, but do it without resetting the selected text
                CommandState.getInstance(editor).popState()
            }
        }
    }
}
