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
 * Every line in [VimEditor] ends with a new line
 * TODO: What are the rules about the last actual line without the new line character?
 * TODO: Minimize the amount of methods to implement
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
 */
interface VimEditor {

  val lfMakesNewLine: Boolean

  fun deleteDryRun(range: VimRange): OperatedRange?
  fun fileSize(): Long

  // TODO: 28.12.2021 Vim always has at least one line!!
  //   Edit: seems like it's not true. Need to recheck it with neovim
  //   Edit: ctrl-g shows "No Lines" for empty buffer
  fun lineCount(): Int

  fun getLineRange(line: Int): Pair<Inclusive, Exclusive>
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
  fun addLine(atPosition: EditorLine.Exclusive): EditorLine.Inclusive?
  fun insertText(atPosition: Exclusive, text: CharSequence)
}

abstract class LinearEditor : VimEditor {
  abstract fun getLine(offset: Int): Int
  abstract fun getText(left: Inclusive, right: Exclusive): CharSequence
}

abstract class MutableLinearEditor : MutableVimEditor, LinearEditor() {
  abstract fun deleteRange(leftOffset: Inclusive, rightOffset: Exclusive)

  override fun delete(range: VimRange) {
    when (range) {
      is VimRange.Block -> TODO()
      is VimRange.Character.Multiple -> TODO()
      is VimRange.Character.Range -> {
        deleteRange(range.offsetAbove().incl, range.offsetBelow().excl)
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
          endOffset = (endOffset.point + 1).excl
        } else if (startOffset.point > 0 && lfMakesNewLine) {
          startOffset = (startOffset.point - 1).incl
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
        val textToDelete = getText(range.offsetAbove().incl, range.offsetBelow().excl)
        OperatedRange.Characters(textToDelete, range.offsetAbove().incl, range.offsetBelow().incl)
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
        val endsWithNewLine = endOffset.point < fileSize() && charAt(endOffset.point) == '\n'
        if (endOffset.point < fileSize() && charAt(endOffset.point) == '\n') {
          endOffset = (endOffset.point + 1).excl
        } else if (startOffset.point > 0 && lfMakesNewLine) {
          startOffset = (startOffset.point - 1).incl
        }
        val textToDelete = getText(startOffset, endOffset)
        OperatedRange.Lines(textToDelete, EditorLine.Inclusive.init(lineAbove, this), lineBelow, !endsWithNewLine)
      }
    }
  }
}

class IjVimEditor(val editor: Editor) : MutableLinearEditor() {
  override val lfMakesNewLine: Boolean = true

  override fun fileSize(): Long = editor.fileSize.toLong()

  // TODO: 28.12.2021 Vim always has at least one line
  override fun lineCount(): Int {
    val lineCount = editor.document.lineCount
    return lineCount.coerceAtLeast(1)
  }

  override fun deleteRange(leftOffset: Inclusive, rightOffset: Exclusive) {
    editor.document.deleteString(leftOffset.point, rightOffset.point)
  }

  override fun addLine(atPosition: EditorLine.Exclusive): EditorLine.Inclusive {
    val offset: Int = if (atPosition.line < lineCount()) {
      editor.document.getLineStartOffset(atPosition.line)
    } else {
      fileSize().toInt()
    }
    editor.document.insertString(offset, "\n")
    return EditorLine.Inclusive.init(atPosition.line, this)
  }

  override fun insertText(atPosition: Exclusive, text: CharSequence) {
    editor.document.insertString(atPosition.point, text)
  }

  override fun getLineRange(line: Int): Pair<Inclusive, Exclusive> {
    return editor.document.getLineStartOffset(line).incl to editor.document.getLineEndOffset(line).excl
  }

  override fun getLine(offset: Int): Int {
    return editor.offsetToLogicalPosition(offset).line
  }

  override fun charAt(offset: Int): Char {
    return editor.document.charsSequence[offset]
  }

  override fun getText(left: Inclusive, right: Exclusive): CharSequence {
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

fun VimCaret.offsetForLineWithStartOfLineOption(logicalLine: EditorLine.Inclusive): Int {
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
    class Range(val startLine: Inclusive, val endLine: Inclusive) : Line() {
      fun lineAbove(): Int = min(startLine.point, endLine.point)
      fun lineBelow(): Int = max(startLine.point, endLine.point)
    }

    class Multiple(val lines: List<Int>) : Line()
    class Offsets(val startOffset: Inclusive, val endOffset: Inclusive) : Line() {
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

  class Block(val start: Inclusive, val end: Inclusive) : VimRange()
}

fun toVimRange(range: TextRange, type: SelectionType): VimRange {
  return when (type) {
    SelectionType.LINE_WISE -> {
      VimRange.Line.Offsets(range.startOffset.incl, range.endOffset.incl)
    }
    SelectionType.CHARACTER_WISE -> VimRange.Character.Range(range.startOffset including range.endOffset)
    SelectionType.BLOCK_WISE -> VimRange.Block(range.startOffset.incl, range.endOffset.incl)
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
      val startOffset = editor.document.getLineStartOffset(this.lineAbove.line)
      val endOffset = editor.document.getLineEndOffset(this.lineBelow)
      TextRange(startOffset, endOffset)
    }
    is OperatedRange.Characters -> TextRange(this.leftOffset.point, this.rightOffset.point)
  }
}

/**
 * `start` is not lower than `end`
 */
data class VimTextRange(
  val start: Inclusive,
  val end: Inclusive,
)

infix fun Int.including(another: Int): VimTextRange {
  return VimTextRange(this.incl, another.incl)
}

val Int.incl: Inclusive
  get() = Inclusive(this)
val Int.excl: Exclusive
  get() = Exclusive(this)

/**
 * Can be converted to value class
 */
data class Inclusive(val point: Int)
data class Exclusive(val point: Int)

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
  class Inclusive(line: Int) : EditorLine(line) {
    companion object {
      fun init(line: Int, forEditor: VimEditor): Inclusive {
        if (line < 0) error("")
        if (line >= forEditor.lineCount()) error("")
        return Inclusive(line)
      }
    }
  }
  class Exclusive(line: Int) : EditorLine(line) {

    // TODO: 28.12.2021 WARNING, I'm not sure that this method is safe and okay
    fun toInclusive(forEditor: VimEditor): Inclusive {
      return Inclusive.init(line, forEditor)
    }

    fun shrinkToInclusive(forEditor: VimEditor): Inclusive {
      val newLine = this.line.coerceIn(0 until forEditor.lineCount())
      return Inclusive.init(newLine, forEditor)
    }

    companion object {
      fun init(line: Int, forEditor: VimEditor): Exclusive {
        if (line < 0) error("")
        // TODO: 28.12.2021 Is this logic correct?
        //   IJ has an additional line
        if (line > forEditor.lineCount()) error("")
        return Exclusive(line)
      }
    }
  }
}

sealed class OperatedRange {
  class Lines(val text: CharSequence, val lineAbove: EditorLine.Inclusive, val lineBelow: Int, val lastNewLineCharMissing: Boolean) : OperatedRange()
  class Characters(val text: CharSequence, val leftOffset: Inclusive, val rightOffset: Inclusive) : OperatedRange()
  class Block : OperatedRange() {
    init {
        TODO()
    }
  }
}
