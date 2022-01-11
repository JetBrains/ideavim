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
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.inlayAwareVisualColumn
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.vimscript.services.OptionService.Scope.LOCAL
import kotlin.math.max
import kotlin.math.min

/**
 * Every line in [VimEditor] ends with a new line TODO <- this is probably not true already
 *
 * # New line and line count
 *
 * In vim **every** line always ends with a new line character. If you'll open a file that doesn't end with
 *   a new line and save it without a modification, the new line character will be automatically added.
 * This is not how the most editors work. The file and the line may end without a new character symbol.
 * This affects a [lineCount] function. A three-lines file in vim contains four lines in other editors.
 * Also this complicated deletion logic.
 *
 * At the moment, we consider a what-user-sees approach. A file with two lines where every line ends with
 *   a new line character, is considered as a three-lines file because it's represented in the editor like this.
 *
 * TODO: We should understand what logic changes if we use a two or three lines editors.
 *
 * ---
 * # Offset and range deletion
 *
 * [Offset] is a position between characters.
 * [delete] method, that works on offset-offset, deltes the character *between* two offsets. That means that:
 * - It's possible and simply applicable when the start offset is larger than end offset
 * - Inclusive/Exclusive words are not applicable to such operation. So we don't define the operation
 *      like "... with end offset exclusive". However, technically, the "right" or the "larger" offset is exclusive.
 *
 * ## Other decisions review:
 * - End offset exclusive:
 *   This is a classic approach, that is used in most APIs, however, it's inconvenient for the case where the
 *     start offset is larger than end offset (direction switching) because for such offset switching it turns that
 *     start offset is exclusive and end offset is inclusive.
 * - End offset inclusive:
 *   This approach is convenient for direction switching, however, makes the situation that empty range cannot be
 *     specified. E.g. range 1:1 would delete the character under `1`, however it looks like that nothing should be
 *     deleted.
 *  Also, during the development it turned out that using such appriach causes a lot of `+1` and `-1` operations that
 *     seem to be redundant.
 *
 * ---
 * # Lines
 *
 * We use two types of line reference: Offset and pointer. Offset referrers to a between-lines position.
 *   Pointer referrers to the concrete line.
 */
interface VimEditor {

  val lfMakesNewLine: Boolean

  fun deleteDryRun(range: VimRange): OperatedRange?
  fun fileSize(): Long

  /**
   * Vim has always at least one line. When we need to understand that there are no lines, it has a flag "ML_EMPTY"
   *   which indicated that the buffer is empty. However, the line count is still 1.
   *
   * The variable for line count is named `ml_line_count` in `memline` structure. There is a single spot where
   *   `0` is assigned to this variable (at the end of `buf_freeall` function), however I'm not sure that this affects
   *   the opened buffer.
   * Another thing that I don't understand is that I don't see where this variable is updated. There is a small chance
   *   that this variable doesn't present the line count, so I may be wrong and line count can return zero.
   * I've explored this question by looking at the implementation of ctrl-g command in normal mode.
   */
  fun lineCount(): Int

  fun getLineRange(line: Int): Pair<Offset, Offset>
  fun charAt(offset: Int): Char
}

fun VimEditor.indentForLine(line: Int): Int {
  val editor = (this as IjVimEditor).editor
  return EditorHelper.getLeadingCharacterOffset(editor, line)
}

interface MutableVimEditor {
  /**
   * Returns actually deleted range and the according text, if any.
   *
   * TODO: How to make a clear code difference between [delete] and [deleteDryRun]. How to make sure that [deleteDryRun]
   *   will be called before [delete]?
   */
  fun delete(range: VimRange)
  fun addLine(atPosition: EditorLine.Offset): EditorLine.Pointer?
  fun insertText(atPosition: Offset, text: CharSequence)
}

abstract class LinearEditor : VimEditor {
  abstract fun getLine(offset: Offset): Int
  abstract fun getText(left: Offset, right: Offset): CharSequence
}

abstract class MutableLinearEditor : MutableVimEditor, LinearEditor() {
  abstract fun deleteRange(leftOffset: Offset, rightOffset: Offset)

  override fun delete(range: VimRange) {
    when (range) {
      is VimRange.Block -> TODO()
      is VimRange.Character.Multiple -> TODO()
      is VimRange.Character.Range -> {
        deleteRange(range.offsetAbove(), range.offsetBelow())
      }
      is VimRange.Line.Multiple -> TODO()
      is VimRange.Line.Range -> {
        val startOffset = getLineRange(range.lineAbove()).first
        val endOffset = getLineRange(range.lineBelow()).second
        deleteRange(startOffset, endOffset)
      }
      is VimRange.Line.Offsets -> {
        var startOffset = getLineRange(getLine(range.offsetAbove())).first
        var endOffset = getLineRange(getLine(range.offsetBelow())).second
        if (endOffset.point < fileSize() && charAt(endOffset.point) == '\n') {
          endOffset = (endOffset.point + 1).offset
        } else if (startOffset.point > 0 && lfMakesNewLine) {
          startOffset = (startOffset.point - 1).offset
        }
        deleteRange(startOffset, endOffset)
      }
    }
  }

  override fun deleteDryRun(range: VimRange): OperatedRange? {
    return when (range) {
      is VimRange.Block -> TODO()
      is VimRange.Character.Multiple -> TODO()
      is VimRange.Character.Range -> {
        val textToDelete = getText(range.offsetAbove(), range.offsetBelow())
        OperatedRange.Characters(textToDelete, range.offsetAbove(), range.offsetBelow())
      }
      is VimRange.Line.Multiple -> TODO()
      is VimRange.Line.Range -> {
        val startOffset = getLineRange(range.lineAbove()).first
        val endOffset = getLineRange(range.lineBelow()).second
        val textToDelete = getText(startOffset, endOffset)
        TODO()
      }
      is VimRange.Line.Offsets -> {
        val lineAbove = getLine(range.offsetAbove())
        var startOffset = getLineRange(lineAbove).first
        val lineBelow = getLine(range.offsetBelow())
        var endOffset = getLineRange(lineBelow).second
        if (endOffset.point < fileSize() && charAt(endOffset.point) == '\n') {
          endOffset = (endOffset.point + 1).offset
        } else if (startOffset.point > 0 && lfMakesNewLine) {
          startOffset = (startOffset.point - 1).offset
        }
        val textToDelete = getText(startOffset, endOffset)
        OperatedRange.Lines(textToDelete, EditorLine.Offset.init(lineAbove, this), lineBelow - lineAbove)
      }
    }
  }
}

class IjVimEditor(val editor: Editor) : MutableLinearEditor() {
  override val lfMakesNewLine: Boolean = true

  override fun fileSize(): Long = editor.fileSize.toLong()

  override fun lineCount(): Int {
    val lineCount = editor.document.lineCount
    return lineCount.coerceAtLeast(1)
  }

  override fun deleteRange(leftOffset: Offset, rightOffset: Offset) {
    editor.document.deleteString(leftOffset.point, rightOffset.point)
  }

  override fun addLine(atPosition: EditorLine.Offset): EditorLine.Pointer {
    val offset: Int = if (atPosition.line < lineCount()) {
      editor.document.getLineStartOffset(atPosition.line)
    } else {
      fileSize().toInt()
    }
    editor.document.insertString(offset, "\n")
    return EditorLine.Pointer.init(atPosition.line, this)
  }

  override fun insertText(atPosition: Offset, text: CharSequence) {
    editor.document.insertString(atPosition.point, text)
  }

  // TODO: 30.12.2021 Is end offset inclusive?
  override fun getLineRange(line: Int): Pair<Offset, Offset> {
    // TODO: 30.12.2021 getLineEndOffset returns the same value for "xyz" and "xyz\n"
    return editor.document.getLineStartOffset(line).offset to editor.document.getLineEndOffset(line).offset
  }

  override fun getLine(offset: Offset): Int {
    return editor.offsetToLogicalPosition(offset.point).line
  }

  override fun charAt(offset: Int): Char {
    return editor.document.charsSequence[offset]
  }

  override fun getText(left: Offset, right: Offset): CharSequence {
    return editor.document.charsSequence.subSequence(left.point, right.point)
  }
}

// TODO: 29.12.2021 Split interface to mutable and immutable
interface VimCaret {
  val editor: VimEditor
  fun moveToOffset(offset: Int)
  fun offsetForLineStartSkipLeading(line: Int): Int
  fun getLine(): Int
}

class IjVimCaret(val caret: Caret) : VimCaret {
  override val editor: VimEditor
    get() = IjVimEditor(caret.editor)

  override fun moveToOffset(offset: Int) {
    // TODO: 17.12.2021 Unpack internal actions
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }

  override fun offsetForLineStartSkipLeading(line: Int): Int {
    return VimPlugin.getMotion().moveCaretToLineStartSkipLeading((editor as IjVimEditor).editor, line)
  }

  override fun getLine(): Int {
    return caret.logicalPosition.line
  }
}

fun VimCaret.offsetForLineWithStartOfLineOption(logicalLine: EditorLine.Pointer): Int {
  val ijEditor = (this.editor as IjVimEditor).editor
  val caret = (this as IjVimCaret).caret
  return if (VimPlugin.getOptionService().isSet(LOCAL(ijEditor), "startofline")) {
    offsetForLineStartSkipLeading(logicalLine.line)
  } else {
    VimPlugin.getMotion().moveCaretToLineWithSameColumn(ijEditor, logicalLine.line, caret)
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
    class Range(val startLine: EditorLine.Pointer, val endLine: EditorLine.Pointer) : Line() {
      fun lineAbove(): Int = min(startLine.line, endLine.line)
      fun lineBelow(): Int = max(startLine.line, endLine.line)
    }

    class Multiple(val lines: List<Int>) : Line()

    // TODO: 11.01.2022 How converting offsets to lines work?
    class Offsets(val startOffset: Offset, val endOffset: Offset) : Line() {
      fun offsetAbove(): Offset = min(startOffset.point, endOffset.point).offset
      fun offsetBelow(): Offset = max(startOffset.point, endOffset.point).offset
    }
  }

  sealed class Character : VimRange() {
    class Range(val range: VimTextRange) : Character() {
      fun offsetAbove(): Offset = min(range.start.point, range.end.point).offset
      fun offsetBelow(): Offset = max(range.start.point, range.end.point).offset
    }

    class Multiple(val ranges: List<VimTextRange>) : Character()
  }

  class Block(val start: Offset, val end: Offset) : VimRange()
}

fun toVimRange(range: TextRange, type: SelectionType): VimRange {
  return when (type) {
    SelectionType.LINE_WISE -> {
      VimRange.Line.Offsets(range.startOffset.offset, range.endOffset.offset)
    }
    SelectionType.CHARACTER_WISE -> VimRange.Character.Range(range.startOffset including range.endOffset)
    SelectionType.BLOCK_WISE -> VimRange.Block(range.startOffset.offset, range.endOffset.offset)
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
      // TODO: 11.01.2022 This is unsafe
      val startOffset = editor.document.getLineStartOffset(this.lineAbove.line)
      val endOffset = editor.document.getLineEndOffset(lineAbove.line + linesOperated)
      TextRange(startOffset, endOffset)
    }
    is OperatedRange.Characters -> TextRange(this.leftOffset.point, this.rightOffset.point)
  }
}

/**
 * `start` is not lower than `end`
 */
data class VimTextRange(
  val start: Offset,
  val end: Offset,
) {
  init {
    if (start.point > end.point) {
      println()
    }
  }
}

infix fun Int.including(another: Int): VimTextRange {
  return VimTextRange(this.offset, another.offset)
}

data class Offset(val point: Int)
val Int.offset: Offset
  get() = Offset(this)

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

sealed class EditorLine private constructor(val line: Int) {
  class Pointer(line: Int) : EditorLine(line) {
    companion object {
      fun init(line: Int, forEditor: VimEditor): Pointer {
        if (line < 0) error("")
        if (line >= forEditor.lineCount()) error("")
        return Pointer(line)
      }
    }
  }
  class Offset(line: Int) : EditorLine(line) {

    fun toPointer(forEditor: VimEditor): Pointer {
      return Pointer.init(line.coerceAtMost(forEditor.lineCount() - 1), forEditor)
    }

    companion object {
      fun init(line: Int, forEditor: VimEditor): Offset {
        if (line < 0) error("")
        // TODO: 28.12.2021 Is this logic correct?
        //   IJ has an additional line
        if (line > forEditor.lineCount()) error("")
        return Offset(line)
      }
    }
  }
}

sealed class OperatedRange {
  class Lines(val text: CharSequence, val lineAbove: EditorLine.Offset, val linesOperated: Int) : OperatedRange()
  class Characters(val text: CharSequence, val leftOffset: Offset, val rightOffset: Offset) : OperatedRange()
  class Block : OperatedRange() {
    init {
      TODO()
    }
  }
}
