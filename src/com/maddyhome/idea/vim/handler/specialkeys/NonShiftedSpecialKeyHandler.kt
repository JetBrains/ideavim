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

package com.maddyhome.idea.vim.handler.specialkeys

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.option.Options.KEYMODEL

/**
 * @author Alex Plate
 *
 * Handler for NON-SHIFTED special keys, that are defined in `:h keymodel`
 * There are: cursor keys, <End>, <Home>, <PageUp> and <PageDown>
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called for each caret
 */
abstract class NonShiftedSpecialKeyHandler : MotionActionHandler.ForEachCaret() {
    final override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        val keymodel = Options.getInstance().getListOption(KEYMODEL)
        if (CommandState.inSelectMode(editor) && (keymodel?.contains("stopsel") == true || keymodel?.contains("stopselect") == true)) {
            VimPlugin.getVisualMotion().exitSelectMode(editor, false)
        }
        if (CommandState.inVisualMode(editor) && (keymodel?.contains("stopsel") == true || keymodel?.contains("stopvisual") == true)) {
            VimPlugin.getVisualMotion().exitVisual(editor)
        }

        return offset(editor, caret, context, count, rawCount, argument)
    }

    /**
     * Calculate new offset for current [caret]
     */
    abstract fun offset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int
}