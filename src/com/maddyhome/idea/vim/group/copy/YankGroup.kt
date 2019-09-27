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

package com.maddyhome.idea.vim.group.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.updown.MotionDownLess1FirstNonSpaceAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.annotations.Contract
import java.util.*
import kotlin.math.min

class YankGroup {
  /**
   * This yanks the text moved over by the motion command argument.
   *
   * @param editor   The editor to yank from
   * @param context  The data context
   * @param count    The number of times to yank
   * @param rawCount The actual count entered by the user
   * @param argument The motion command argument
   * @return true if able to yank the text, false if not
   */
  fun yankMotion(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument): Boolean {
    val motion = argument.motion

    val caretModel = editor.caretModel
    if (caretModel.caretCount <= 0) return false

    val ranges = ArrayList<Pair<Int, Int>>(caretModel.caretCount)

    // This logic is from original vim
    val startOffsets = if (argument.motion.action is MotionDownLess1FirstNonSpaceAction) null else HashMap<Caret, Int>(caretModel.caretCount)

    for (caret in caretModel.allCarets) {
      val motionRange = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument)
        ?: continue

      assert(motionRange.size() == 1)
      ranges.add(motionRange.startOffset to motionRange.endOffset)
      startOffsets?.put(caret, motionRange.normalize().startOffset)
    }

    val type = SelectionType.fromCommandFlags(motion.flags)
    val range = getTextRange(ranges, type)

    if (range.size() == 0) return false;

    val selectionType = if (type == SelectionType.CHARACTER_WISE && range.isMultiple) SelectionType.BLOCK_WISE else type
    return yankRange(editor, range, selectionType, startOffsets)
  }

  /**
   * This yanks count lines of text
   *
   * @param editor The editor to yank from
   * @param count  The number of lines to yank
   * @return true if able to yank the lines, false if not
   */
  fun yankLine(editor: Editor, count: Int): Boolean {
    val caretModel = editor.caretModel
    val ranges = ArrayList<Pair<Int, Int>>(caretModel.caretCount)
    for (caret in caretModel.allCarets) {
      val start = VimPlugin.getMotion().moveCaretToLineStart(editor, caret)
      val end = min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1, EditorHelper.getFileSize(editor, true))

      if (end == -1) continue

      ranges.add(start to end)
    }

    val range = getTextRange(ranges, SelectionType.LINE_WISE)
    return yankRange(editor, range, SelectionType.LINE_WISE, null)
  }

  /**
   * This yanks a range of text
   *
   * @param editor The editor to yank from
   * @param range  The range of text to yank
   * @param type   The type of yank
   * @return true if able to yank the range, false if not
   */
  fun yankRange(editor: Editor, range: TextRange?, type: SelectionType, moveCursor: Boolean): Boolean {
    range ?: return false

    val selectionType = if (type == SelectionType.CHARACTER_WISE && range.isMultiple) SelectionType.BLOCK_WISE else type

    if (type == SelectionType.LINE_WISE) {
      for (i in 0 until range.size()) {
        if (editor.offsetToLogicalPosition(range.startOffsets[i]).column != 0) {
          range.startOffsets[i] = EditorHelper.getLineStartForOffset(editor, range.startOffsets[i])
        }
        if (editor.offsetToLogicalPosition(range.endOffsets[i]).column != 0) {
          range.endOffsets[i] = (EditorHelper.getLineEndForOffset(editor, range.endOffsets[i]) + 1).coerceAtMost(EditorHelper.getFileSize(editor))
        }
      }
    }

    val caretModel = editor.caretModel
    val rangeStartOffsets = range.startOffsets
    val rangeEndOffsets = range.endOffsets

    return if (moveCursor) {
      val startOffsets = HashMap<Caret, Int>(caretModel.caretCount)
      if (type == SelectionType.BLOCK_WISE) {
        startOffsets[caretModel.primaryCaret] = range.normalize().startOffset
      } else {
        val carets = caretModel.allCarets
        for (i in carets.indices) {
          startOffsets[carets[i]] = TextRange(rangeStartOffsets[i], rangeEndOffsets[i]).normalize().startOffset
        }
      }

      yankRange(editor, range, selectionType, startOffsets)
    } else {
      yankRange(editor, range, selectionType, null)
    }
  }

  @Contract("_, _ -> new")
  private fun getTextRange(ranges: List<Pair<Int, Int>>, type: SelectionType): TextRange {
    val size = ranges.size
    val starts = IntArray(size)
    val ends = IntArray(size)

    if (type == SelectionType.LINE_WISE) {
      starts[size - 1] = ranges[size - 1].first
      ends[size - 1] = ranges[size - 1].second
      for (i in 0 until size - 1) {
        val range = ranges[i]
        starts[i] = range.first
        ends[i] = range.second - 1
      }
    } else {
      for (i in 0 until size) {
        val range = ranges[i]
        starts[i] = range.first
        ends[i] = range.second
      }
    }

    return TextRange(starts, ends)
  }

  private fun yankRange(editor: Editor, range: TextRange, type: SelectionType,
                        startOffsets: Map<Caret, Int>?): Boolean {
    startOffsets?.forEach { caret, offset -> MotionGroup.moveCaret(editor, caret, offset) }

    return VimPlugin.getRegister().storeText(editor, range, type, false)
  }
}
