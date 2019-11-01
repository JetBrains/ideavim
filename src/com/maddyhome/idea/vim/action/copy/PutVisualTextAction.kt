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

package com.maddyhome.idea.vim.action.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.group.copy.PutData
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * @author vlan
 */
class PutVisualTextAction : VisualOperatorActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_EXIT_VISUAL)
  override fun executeForAllCarets(editor: Editor,
                                   context: DataContext,
                                   cmd: Command,
                                   caretsAndSelections: Map<Caret, VimSelection>): Boolean {
    if (caretsAndSelections.isEmpty()) return false
    val textData = VimPlugin.getRegister().lastRegister?.let { PutData.TextData(it.text, it.type, it.transferableData) }
    VimPlugin.getRegister().resetRegister()

    val insertTextBeforeCaret = cmd.keys[0].keyChar == 'P'
    val selection = PutData.VisualSelection(caretsAndSelections, caretsAndSelections.values.first().type)
    val putData = PutData(textData, selection, cmd.count, insertTextBeforeCaret, _indent = true, caretAfterInsertedText = false)

    return VimPlugin.getPut().putText(editor, context, putData)
  }
}
