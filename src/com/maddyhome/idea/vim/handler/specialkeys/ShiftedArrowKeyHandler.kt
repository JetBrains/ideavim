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
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.option.Options

/**
 * @author Alex Plate
 *
 * Handler for SHIFTED arrow keys
 *
 * This handler is used to properly handle there keys according to current `keymodel` and `selectmode` options
 *
 * Handler is called once for all carets
 */
abstract class ShiftedArrowKeyHandler : EditorActionHandlerBase() {
    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val keymodelOption = Options.getInstance().getListOption(Options.KEYMODEL)
        val startSel = keymodelOption?.contains("startsel") == true
        val aContinueSelect = keymodelOption?.contains("acontinueselect") == true
        val aContinueVisual = keymodelOption?.contains("acontinuevisual") == true
        val inVisualMode = CommandState.inVisualMode(editor)
        val inSelectMode = CommandState.inSelectMode(editor)
        if (startSel || aContinueSelect && inSelectMode || aContinueVisual && inVisualMode) {
            if (!inVisualMode && !inSelectMode) {
                if (Options.getInstance().getListOption(Options.SELECTMODE)?.contains("key") == true) {
                    VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
                } else {
                    VimPlugin.getVisualMotion()
                            .toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_CHARACTER)
                }
            }
            motionWithKeyModel(editor, context, cmd)
        } else {
            motionWithoutKeyModel(editor, context, cmd)
        }
        return true
    }

    /**
     * This method is called when `keymodel` contains `startsel`, or one of `continue*` values in corresponding mode
     */
    abstract fun motionWithKeyModel(editor: Editor, context: DataContext, cmd: Command)
    /**
     * This method is called when `keymodel` doesn't contain `startsel`,
     * or contains one of `continue*` values but in different mode.
     */
    abstract fun motionWithoutKeyModel(editor: Editor, context: DataContext, cmd: Command)
}