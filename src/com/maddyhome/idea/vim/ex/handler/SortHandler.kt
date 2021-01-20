/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset

/**
 * @author Alex Selesse
 */
class SortHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val arg = cmd.argument
    val nonEmptyArg = arg.trim().isNotEmpty()

    val reverse = nonEmptyArg && "!" in arg
    val ignoreCase = nonEmptyArg && "i" in arg
    val number = nonEmptyArg && "n" in arg

    val lineComparator = LineComparator(ignoreCase, number, reverse)
    if (editor.inBlockSubMode) {
      val primaryCaret = editor.caretModel.primaryCaret
      val range = getLineRange(editor, primaryCaret, cmd)
      val worked = VimPlugin.getChange().sortRange(editor, range, lineComparator)
      primaryCaret.moveToInlayAwareOffset(
        VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, range.startLine)
      )
      return worked
    }

    var worked = true
    for (caret in editor.caretModel.allCarets) {
      val range = getLineRange(editor, caret, cmd)
      if (!VimPlugin.getChange().sortRange(editor, range, lineComparator)) {
        worked = false
      }
      caret.moveToInlayAwareOffset(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, range.startLine))
    }

    return worked
  }

  private fun getLineRange(editor: Editor, caret: Caret, cmd: ExCommand): LineRange {
    val range = cmd.getLineRange(editor, caret)

    // Something like "30,20sort" gets converted to "20,30sort"
    val normalizedRange = if (range.endLine < range.startLine) LineRange(range.endLine, range.startLine) else range

    // If we don't have a range, we either have "sort", a selection, or a block
    if (normalizedRange.endLine - normalizedRange.startLine == 0) {
      // If we have a selection.
      val selectionModel = editor.selectionModel
      return if (selectionModel.hasSelection()) {
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val startLine = editor.offsetToLogicalPosition(start).line
        val endLine = editor.offsetToLogicalPosition(end).line

        LineRange(startLine, endLine)
      } else {
        LineRange(0, editor.document.lineCount - 1)
      }// If we have a generic selection, i.e. "sort" entire document
    }

    return normalizedRange
  }

  private class LineComparator(
    private val myIgnoreCase: Boolean,
    private val myNumber: Boolean,
    private val myReverse: Boolean
  ) : Comparator<String> {

    override fun compare(o1: String, o2: String): Int {
      var o1ToCompare = o1
      var o2ToCompare = o2
      if (myReverse) {
        val tmp = o2ToCompare
        o2ToCompare = o1ToCompare
        o1ToCompare = tmp
      }
      if (myIgnoreCase) {
        o1ToCompare = o1ToCompare.toUpperCase()
        o2ToCompare = o2ToCompare.toUpperCase()
      }
      return if (myNumber) StringUtil.naturalCompare(o1ToCompare, o2ToCompare) else o1ToCompare.compareTo(o2ToCompare)
    }
  }
}
