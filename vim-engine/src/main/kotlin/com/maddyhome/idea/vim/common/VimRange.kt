/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.api.toType
import com.maddyhome.idea.vim.mark.VimMarkConstants
import kotlin.math.max
import kotlin.math.min

/**
 * Back direction range is possible. `start` is not lower than `end`.
 * TODO: How to show it in code and namings?
 *    How to separate methods that return "starting from" line and "the above line"
 *
 * TODO: How to represent "till last column"
 *
 * [VimRange] includes selection type (line, character, block)
 *
 * Range normalizations (check if line and offsets really exist) are performed in editor implementations.
 */
public sealed class VimRange {
  public sealed class Line : VimRange() {
    public class Range(public val startLine: EditorLine.Pointer, public val endLine: EditorLine.Pointer) : Line() {
      public fun lineAbove(): EditorLine.Pointer = listOf(startLine, endLine).minByOrNull { it.line }!!
      public fun lineBelow(): EditorLine.Pointer = listOf(startLine, endLine).maxByOrNull { it.line }!!
    }

    public class Multiple(public val lines: List<Int>) : Line()

    // TODO: 11.01.2022 How converting offsets to lines work?
    public class Offsets(public val startOffset: Offset, public val endOffset: Offset) : Line() {
      public fun offsetAbove(): Offset = min(startOffset.point, endOffset.point).offset
      public fun offsetBelow(): Offset = max(startOffset.point, endOffset.point).offset
    }
  }

  public sealed class Character : VimRange() {
    public class Range(public val range: VimTextRange) : Character() {
      public fun offsetAbove(): Offset = min(range.start.point, range.end.point).offset
      public fun offsetBelow(): Offset = max(range.start.point, range.end.point).offset
    }

    public class Multiple(public val ranges: List<VimTextRange>) : Character()
  }

  public class Block(public val start: Offset, public val end: Offset) : VimRange()
}

/**
 * `start` is not lower than `end`
 */
public data class VimTextRange(
  val start: Offset,
  val end: Offset,
) {
  init {
    if (start.point > end.point) {
      println()
    }
  }
}

public infix fun Int.including(another: Int): VimTextRange {
  return VimTextRange(this.offset, another.offset)
}

public data class Offset(val point: Int)
public data class Pointer(val point: Int)

public val Int.offset: Offset
  get() = Offset(this)
public val Int.pointer: Pointer
  get() = Pointer(this)

public interface VimMachine {
  public fun delete(range: VimRange, editor: VimEditor, caret: ImmutableVimCaret): OperatedRange?
}

public abstract class VimMachineBase : VimMachine {
  /**
   * The information I'd like to know after the deletion:
   * - What range is deleted?
   * - What text is deleted?
   * - Does text have a new line character at the end?
   * - At what offset?
   * - What caret?
   */
  public override fun delete(range: VimRange, editor: VimEditor, caret: ImmutableVimCaret): OperatedRange? {
    val operatedText = editor.deleteDryRun(range) ?: return null

    val normalizedRange = operatedText.toNormalizedTextRange(editor)
    caret.registerStorage.storeText(editor, normalizedRange, operatedText.toType(), true)
    (editor as MutableVimEditor).delete(range)

    val start = normalizedRange.startOffset
    injector.markService.setMark(caret, VimMarkConstants.MARK_CHANGE_POS, start)
    injector.markService.setChangeMarks(caret, TextRange(start, start + 1))

    return operatedText
  }
}

public fun OperatedRange.toNormalizedTextRange(editor: VimEditor): TextRange {
  return when (this) {
    is OperatedRange.Block -> TODO()
    is OperatedRange.Lines -> {
      // TODO: 11.01.2022 This is unsafe
      val startOffset = editor.getLineStartOffset(this.lineAbove.line)
      val endOffset = editor.getLineEndOffset(lineAbove.line + linesOperated, true)
      TextRange(startOffset, endOffset)
    }
    is OperatedRange.Characters -> TextRange(this.leftOffset.point, this.rightOffset.point)
  }
}

public sealed class EditorLine private constructor(public val line: Int) {
  public class Pointer(line: Int) : EditorLine(line) {
    public companion object {
      public fun init(line: Int, forEditor: VimEditor): Pointer {
        if (line < 0) error("")
        if (line >= forEditor.lineCount()) error("")
        return Pointer(line)
      }
    }
  }

  public class Offset(line: Int) : EditorLine(line) {

    public fun toPointer(forEditor: VimEditor): Pointer {
      return Pointer.init(line.coerceAtMost(forEditor.lineCount() - 1), forEditor)
    }

    public companion object {
      public fun init(line: Int, forEditor: VimEditor): Offset {
        if (line < 0) error("")
        // TODO: 28.12.2021 Is this logic correct?
        //   IJ has an additional line
        if (line > forEditor.lineCount()) error("")
        return Offset(line)
      }
    }
  }
}

public sealed class OperatedRange {
  public class Lines(
    public val text: CharSequence,
    public val lineAbove: EditorLine.Offset,
    public val linesOperated: Int,
    public val shiftType: LineDeleteShift,
  ) : OperatedRange()

  public class Characters(public val text: CharSequence, public val leftOffset: Offset, public val rightOffset: Offset) : OperatedRange()
  public class Block : OperatedRange() {
    init {
      TODO()
    }
  }
}
