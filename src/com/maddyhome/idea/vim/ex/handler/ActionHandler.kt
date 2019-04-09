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
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.DONT_REOPEN
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.SAVE_VISUAL
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.helper.runAfterGotFocus

/**
 * @author smartbomb
 */
class ActionHandler : CommandHandler(
        commands("action"),
        flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, DONT_REOPEN, SAVE_VISUAL)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        val actionName = cmd.argument.trim()
        val action = ActionManager.getInstance().getAction(actionName) ?: run {
            VimPlugin.showMessage("Action not found: $actionName")
            return false
        }
        val application = ApplicationManager.getApplication()
        val selections = editor.caretModel.allCarets.map { if (it.hasSelection()) it.selectionStart to it.selectionEnd else null }
        if (application.isUnitTestMode) {
            executeAction(editor, action, context, selections)
        } else {
            runAfterGotFocus(Runnable { executeAction(editor, action, context, selections) })
        }
        return true
    }

    private fun executeAction(editor: Editor, action: AnAction, context: DataContext, selections: List<Pair<Int, Int>?>) {
        SelectionVimListenerSuppressor.lock()
        for (i in selections.indices) {
            // Restore selections after runAfterGotFocus
            val selection = selections[i]
            if (selection != null) {
                editor.caretModel.allCarets[i].setSelection(selection.first, selection.second)
            }
        }
        SelectionVimListenerSuppressor.unlock()

        KeyHandler.executeAction(action, context)
    }
}
