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

package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.option.Options
import java.util.*
import javax.swing.KeyStroke

/**
 *
 */
private object VisualToggleLineModeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val listOption = Options.getInstance().getListOption(Options.SELECTMODE)
        return if (listOption != null && "cmd" in listOption) {
            VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_LINE).also {
                editor.vimForEachCaret { it.vimSetSelection(it.offset) }
            }
        } else VimPlugin.getVisualMotion()
                .toggleVisual(editor, cmd.count, cmd.rawCount, CommandState.SubMode.VISUAL_LINE)

    }
}

class VisualToggleLineModeAction : VimCommandAction(VisualToggleLineModeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.NV

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("V")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY

    override fun getFlags(): EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE)
}

