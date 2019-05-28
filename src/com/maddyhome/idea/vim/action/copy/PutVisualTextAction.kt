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

package com.maddyhome.idea.vim.action.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import java.util.*
import javax.swing.KeyStroke

private object PutVisualTextActionHandler : VisualOperatorActionHandler.SingleExecution() {

    override fun executeForAllCarets(editor: Editor,
                                     context: DataContext,
                                     cmd: Command,
                                     caretsAndSelections: Map<Caret, VimSelection>): Boolean {
        val register = VimPlugin.getRegister().lastRegister
        VimPlugin.getRegister().resetRegister()
        if (register != null && register.type == SelectionType.LINE_WISE && editor.isOneLineMode) return false
        if (caretsAndSelections.isEmpty()) return false

        val range = caretsAndSelections.values.first()

        return if (range.type == SelectionType.BLOCK_WISE) {
            val isBigP = cmd.keys[0] == parseKeys("P")[0]

            VimPlugin.getPut()
                    .putVisualRangeBlockwise(editor, context, range, cmd.count, true, false, register, isBigP)
        } else {
            VimPlugin.getPut()
                    .putVisualRangeCaL(editor, context, caretsAndSelections, cmd.count, true, false, register)
        }
    }
}

/**
 * @author vlan
 */
class PutVisualTextAction : VimCommandAction(PutVisualTextActionHandler) {

    override fun getMappingModes(): Set<MappingMode> = MappingMode.V

    override fun getKeyStrokesSet(): Set<List<KeyStroke>> = parseKeysSet("p", "P")

    override fun getType(): Command.Type = Command.Type.PASTE

    override fun getFlags(): EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_EXIT_VISUAL)
}
