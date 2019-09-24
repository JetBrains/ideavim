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

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.helper.endOffsetInclusive
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 *
 * Handler for TextObjects.
 *
 * This handler gets executed for each caret.
 */
abstract class TextObjectActionHandler : EditorActionHandlerBase.ForEachCaret() {
  final override val type: Command.Type = Command.Type.MOTION
  abstract fun getRange(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): TextRange?
  override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
    if (!editor.inVisualMode) return true

    val range = getRange(editor, caret, context, cmd.count, cmd.rawCount, cmd.argument) ?: return false

    val block = CommandFlags.FLAG_TEXT_BLOCK in cmd.flags
    val newstart = if (block || caret.offset >= caret.vimSelectionStart) range.startOffset else range.endOffsetInclusive
    val newend = if (block || caret.offset >= caret.vimSelectionStart) range.endOffsetInclusive else range.startOffset

    if (caret.vimSelectionStart == caret.offset || block) {
      caret.vimSetSelection(newstart, newstart, false)
    }

    if (CommandFlags.FLAG_MOT_LINEWISE in cmd.flags && editor.subMode != CommandState.SubMode.VISUAL_LINE) {
      VimPlugin.getVisualMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_LINE)
    } else if (CommandFlags.FLAG_MOT_LINEWISE !in cmd.flags && editor.subMode == CommandState.SubMode.VISUAL_LINE) {
      VimPlugin.getVisualMotion().toggleVisual(editor, 1, 0, CommandState.SubMode.VISUAL_CHARACTER)
    }

    MotionGroup.moveCaret(editor, caret, newend)

    return true
  }
}
