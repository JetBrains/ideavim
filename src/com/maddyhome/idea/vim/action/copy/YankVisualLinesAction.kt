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
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * @author vlan
 */
class YankVisualLinesAction : VisualOperatorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.COPY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE, CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeForAllCarets(editor: Editor,
                                   context: DataContext,
                                   cmd: Command,
                                   caretsAndSelections: Map<Caret, VimSelection>): Boolean {
    val selections = caretsAndSelections.values
    val starts: MutableList<Int> = ArrayList()
    val ends: MutableList<Int> = ArrayList()
    selections.forEach { selection: VimSelection ->
      val textRange = selection.toVimTextRange(false)
      textRange.startOffsets.forEach { e: Int -> starts.add(e) }
      textRange.endOffsets.forEach { e: Int -> ends.add(e) }
    }
    val vimSelection = selections.firstOrNull() ?: return false
    val startsArray = starts.toIntArray()
    val endsArray = ends.toIntArray()

    val selection = if (vimSelection.type == SelectionType.BLOCK_WISE) SelectionType.BLOCK_WISE else SelectionType.LINE_WISE
    return VimPlugin.getYank().yankRange(editor, TextRange(startsArray, endsArray), selection, true)
  }
}
