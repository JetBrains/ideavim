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

package com.maddyhome.idea.vim.common.editor

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.inlayAwareVisualColumn
import com.maddyhome.idea.vim.helper.vimLastColumn
import kotlin.math.max
import kotlin.math.min

/**
 * Every line in [VimEditor] ends with a new line
 * TODO: What are the rules about the last actual line without the new line character?
 * TODO: Split the editor into mutable and immutable part
 * TODO: Minimize the amount of methods to implement
 */
interface VimEditor {
  /**
   * Returns actually deleted range and the according text, if any.
   *
   * TODO: How to make a clear code difference between [delete] and [deleteDryRun]. How to make sure that [deleteDryRun]
   *   will be called before [delete]?
   */
  fun delete(range: VimRange)
  fun deleteDryRun(range: VimRange): OperatedRange?
  fun fileSize(): Long
  fun addLine(atPosition: Int): Boolean
}

class IjVimEditor(val editor: Editor) : VimEditor {
  override fun delete(range: VimRange) {
    when (range) {
      is VimRange.Block -> TODO()
      is VimRange.Character.Multiple -> TODO()
      is VimRange.Character.Range -> {
        editor.document.deleteString(range.offsetAbove(), range.offsetBelow())
      }
      is VimRange.Line.Multiple -> TODO()
      is VimRange.Line.Range -> {
        val startOffset = editor.document.getLineStartOffset(range.lineAbove())
        val endOffset = editor.document.getLineEndOffset(range.lineBelow())
        editor.document.deleteString(startOffset, endOffset)
      }
      is VimRange.Line.Offsets -> {
        val startOffset = editor.document.getLineStartOffset(editor.offsetToLogicalPosition(range.offsetAbove()).line)
        var endOffset = editor.document.getLineEndOffset(editor.offsetToLogicalPosition(range.offsetBelow()).line)
        if (endOffset < editor.fileSize && editor.document.charsSequence.get(endOffset) == '\n') {
          endOffset += 1
        }
        editor.document.deleteString(startOffset, endOffset)
      }
    }
  }

  override fun deleteDryRun(range: VimRange): OperatedRange? {
    return when (range) {
      is VimRange.Block -> TODO()
      is VimRange.Character.Multiple -> TODO()
      is VimRange.Character.Range -> {
        val textToDelete =
          editor.document.getText(com.intellij.openapi.util.TextRange.create(range.offsetAbove(), range.offsetBelow()))
        OperatedRange.Characters(textToDelete, range.offsetAbove().asOffset(), range.offsetBelow().asOffset())
      }
      is VimRange.Line.Multiple -> TODO()
      is VimRange.Line.Range -> {
        val startOffset = editor.document.getLineStartOffset(range.lineAbove())
        val endOffset = editor.document.getLineEndOffset(range.lineBelow())
        val textToDelete = editor.document.getText(com.intellij.openapi.util.TextRange.create(startOffset, endOffset))
        RangeWithText(range, listOf(textToDelete))
        TODO()
      }
      is VimRange.Line.Offsets -> {
        val lineAbove = editor.offsetToLogicalPosition(range.offsetAbove()).line
        val startOffset = editor.document.getLineStartOffset(lineAbove)
        val lineBelow = editor.offsetToLogicalPosition(range.offsetBelow()).line
        var endOffset = editor.document.getLineEndOffset(lineBelow)
        val endsWithNewLine = endOffset < editor.fileSize && editor.document.charsSequence[endOffset] == '\n'
        if (endOffset < editor.fileSize && editor.document.charsSequence[endOffset] == '\n') {
          endOffset += 1
        }
        val textToDelete = editor.document.getText(com.intellij.openapi.util.TextRange.create(startOffset, endOffset))
        OperatedRange.Lines(textToDelete, lineAbove, lineBelow, !endsWithNewLine)
      }
    }
  }

  override fun fileSize(): Long = editor.fileSize.toLong()

  override fun addLine(atPosition: Int): Boolean {
    val offset = editor.document.getLineStartOffset(atPosition)
    editor.document.insertString(offset, "\n")
    return true
  }
}

interface VimCaret {
  fun moveToOffset(offset: Int)
  fun moveAtLineStart(line: Int)
  fun moveAtTextLineStart(line: Int)
}

class IjVimCaret(val caret: Caret) : VimCaret {
  override fun moveToOffset(offset: Int) {
    // TODO: 17.12.2021 Unpack internal actions
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }

  override fun moveAtLineStart(line: Int) {
    val offset = VimPlugin.getMotion().moveCaretToLineWithStartOfLineOption(caret.editor, line, caret)
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }

  // TODO: 24.12.2021 This is not really text start. It may keep the caret offset
  override fun moveAtTextLineStart(line: Int) {
    val offset = VimPlugin.getMotion().moveCaretToLineWithStartOfLineOption(caret.editor, line, caret)
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }
}

/**
 * Back direction range is possible. `start` is not lower than `end`.
 * TODO: How to show it in code and namings?
 *    How to separate methods that return "starting from" line and "the above line"
 *
 * TODO: How to represent "till last column"
 *
 * [VimRange] includes [SelectionType]
 *
 * Range normalizations (check if line and offsets really exist) are performed in editor implementations.
 */
sealed class VimRange {
  sealed class Line : VimRange() {
    class Range(val startLine: Int, val endLine: Int) : Line() {
      fun lineAbove(): Int = min(startLine, endLine)
      fun lineBelow(): Int = max(startLine, endLine)
    }

    class Multiple(val lines: List<Int>) : Line()
    class Offsets(val startOffset: OffsetIncluding, val endOffset: OffsetIncluding) : Line() {
      fun offsetAbove(): Int = min(startOffset.point, endOffset.point)
      fun offsetBelow(): Int = max(startOffset.point, endOffset.point)
    }
  }

  sealed class Character : VimRange() {
    class Range(val range: VimTextRange) : Character() {
      fun offsetAbove(): Int = min(range.start.point, range.end.point)
      fun offsetBelow(): Int = max(range.start.point, range.end.point)
    }

    class Multiple(val ranges: List<VimTextRange>) : Character()
  }

  class Block(val start: OffsetIncluding, val end: OffsetIncluding) : VimRange()
}

fun toVimRange(range: TextRange, type: SelectionType): VimRange {
  return when (type) {
    SelectionType.LINE_WISE -> {
      VimRange.Line.Offsets(range.startOffset.asOffset(), range.endOffset.asOffset())
    }
    SelectionType.CHARACTER_WISE -> VimRange.Character.Range(range.startOffset including range.endOffset)
    SelectionType.BLOCK_WISE -> VimRange.Block(range.startOffset.asOffset(), range.endOffset.asOffset())
  }
}

fun OperatedRange.toType() = when (this) {
  is OperatedRange.Characters -> SelectionType.CHARACTER_WISE
  is OperatedRange.Lines -> SelectionType.LINE_WISE
  is OperatedRange.Block -> SelectionType.BLOCK_WISE
}

fun OperatedRange.toNormalizedTextRange(editor: Editor): TextRange {
  return when (this) {
    is OperatedRange.Block -> TODO()
    is OperatedRange.Lines -> {
      val startOffset = editor.document.getLineStartOffset(this.lineAbove)
      val endOffset = editor.document.getLineEndOffset(this.lineBelow)
      TextRange(startOffset, endOffset)
    }
    is OperatedRange.Characters -> TextRange(this.leftOffset.point, this.rightOffset.point)
  }
}

class RangeWithText(val range: VimRange, private val text: List<String>) {
  init {
    val expectedSize = when (range) {
      is VimRange.Block -> 1
      is VimRange.Character.Range -> 1
      is VimRange.Character.Multiple -> range.ranges.size
      is VimRange.Line.Multiple -> range.lines.size
      is VimRange.Line.Range -> 1
      is VimRange.Line.Offsets -> 1
    }
    assert(text.size == expectedSize)
  }
}

/**
 * `start` is not lower than `end`
 */
data class VimTextRange(
  val start: OffsetIncluding,
  val end: OffsetIncluding,
)

infix fun Int.including(another: Int): VimTextRange {
  return VimTextRange(OffsetIncluding(this), OffsetIncluding(another))
}

fun Int.asOffset(): OffsetIncluding = OffsetIncluding(this)

/**
 * Can be converted to value class
 */
data class OffsetIncluding(val point: Int)

interface VimMachine {
  fun delete(range: VimRange, editor: VimEditor, caret: VimCaret): OperatedRange?

  companion object {
    val instance = VimMachineImpl()
  }
}

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

sealed class OperatedRange {
  class Lines(val text: String, val lineAbove: Int, val lineBelow: Int, val lastNewLineCharMissing: Boolean) : OperatedRange()
  class Characters(val text: String, val leftOffset: OffsetIncluding, val rightOffset: OffsetIncluding) : OperatedRange()
  class Block(text: List<String>) : OperatedRange()
}
