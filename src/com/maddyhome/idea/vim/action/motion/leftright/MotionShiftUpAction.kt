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

package com.maddyhome.idea.vim.action.motion.leftright

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.option.Options
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object MotionShiftUpActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        if (Options.getInstance().getListOption(Options.KEYMODEL)?.contains("startsel") == true) {
            if (!CommandState.inVisualMode(editor)) {
                VimPlugin.getVisualMotion()
                        .toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_CHARACTER)
            }
            editor.caretModel.allCarets.forEach { caret ->
                val vertical = VimPlugin.getMotion().moveCaretVertical(editor, caret, -cmd.count)
                MotionGroup.moveCaret(editor, caret, vertical)
            }
            return true
        }
        return VimPlugin.getMotion().scrollFullPage(editor, -cmd.count)
    }
}

class MotionShiftUpAction : VimCommandAction(MotionShiftUpActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.NV

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<S-Up>")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}