/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
package com.maddyhome.idea.vim.action.change.delete

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
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.endsWithNewLine
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.secondLastCharIsNewLine
import java.util.*

/**
 * @author vlan
 */
class DeleteVisualLinesAction : VisualOperatorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.DELETE

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MOT_LINEWISE, CommandFlags.FLAG_EXIT_VISUAL)

  override fun executeAction(editor: Editor,
                             caret: Caret,
                             context: DataContext,
                             cmd: Command,
                             range: VimSelection): Boolean {
    val textRange = range.toVimTextRange(false)
    val (usedCaret, usedRange, usedType) = when (range.type) {
      SelectionType.BLOCK_WISE -> Triple(editor.caretModel.primaryCaret, textRange, range.type)
      SelectionType.LINE_WISE -> Triple(caret, textRange, SelectionType.LINE_WISE)
      SelectionType.CHARACTER_WISE -> {
        val lineEndForOffset = EditorHelper.getLineEndForOffset(editor, textRange.endOffset)
        val endsWithNewLine = if (lineEndForOffset == editor.fileSize) 0 else 1
        val lineRange = TextRange(
          EditorHelper.getLineStartForOffset(editor, textRange.startOffset),
          lineEndForOffset + endsWithNewLine
        )
        Triple(caret, lineRange, SelectionType.LINE_WISE)
      }
    }
    return VimPlugin.getChange().deleteRange(editor, usedCaret, usedRange, usedType, false)
  }
}
