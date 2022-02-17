/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.OperatedRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.common.VimMachine
import com.maddyhome.idea.vim.common.VimRange
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.helper.inlayAwareVisualColumn
import com.maddyhome.idea.vim.helper.vimLastColumn

class VimMachineImpl : VimMachine {
  /**
   * The information I'd like to know after the deletion:
   * - What range is deleted?
   * - What text is deleted?
   * - Does text have a new line character at the end?
   * - At what offset?
   * - What caret?
   */
  override fun delete(range: VimRange, editor: VimEditor, caret: VimCaret): OperatedRange? {
    caret as IjVimCaret
    editor as IjVimEditor
    // Update the last column before we delete, or we might be retrieving the data for a line that no longer exists
    caret.caret.vimLastColumn = caret.caret.inlayAwareVisualColumn

    val operatedText = editor.deleteDryRun(range) ?: return null

    val normalizedRange = operatedText.toNormalizedTextRange(editor.editor)
    VimPlugin.getRegister()
      .storeText(editor.editor, normalizedRange, operatedText.toType(), true)

    editor.delete(range)

    val start = normalizedRange.startOffset
    VimPlugin.getMark().setMark(editor.editor, MarkGroup.MARK_CHANGE_POS, start)
    VimPlugin.getMark().setChangeMarks(editor.editor, TextRange(start, start + 1))

    return operatedText
  }
}

fun OperatedRange.toNormalizedTextRange(editor: Editor): TextRange {
  return when (this) {
    is OperatedRange.Block -> TODO()
    is OperatedRange.Lines -> {
      // TODO: 11.01.2022 This is unsafe
      val startOffset = editor.document.getLineStartOffset(this.lineAbove.line)
      val endOffset = editor.document.getLineEndOffset(lineAbove.line + linesOperated)
      TextRange(startOffset, endOffset)
    }
    is OperatedRange.Characters -> TextRange(this.leftOffset.point, this.rightOffset.point)
  }
}

