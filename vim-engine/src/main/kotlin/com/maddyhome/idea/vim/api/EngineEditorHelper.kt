/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import java.nio.CharBuffer

interface EngineEditorHelper {
  fun getText(editor: VimEditor, range: TextRange): String
  fun getOffset(editor: VimEditor, line: Int, column: Int): Int
  fun logicalLineToVisualLine(editor: VimEditor, line: Int): Int
  fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int
  fun getLineStartForOffset(editor: VimEditor, line: Int): Int
  fun getLineEndForOffset(editor: VimEditor, offset: Int): Int
  fun getVisualLineAtTopOfScreen(editor: VimEditor): Int
  fun getApproximateScreenWidth(editor: VimEditor): Int
  fun handleWithReadonlyFragmentModificationHandler(editor: VimEditor, exception: java.lang.Exception)
  fun getLineBuffer(editor: VimEditor, line: Int): CharBuffer
  fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int
  fun pad(editor: VimEditor, context: ExecutionContext, line: Int, to: Int): String
  fun getLineLength(editor: VimEditor): Int
  fun getLineBreakCount(text: CharSequence): Int
  fun inlayAwareOffsetToVisualPosition(editor: VimEditor, offset: Int): VimVisualPosition
  fun getLeadingWhitespace(editor: VimEditor, line: Int): String
  fun anyNonWhitespace(editor: VimEditor, offset: Int, dir: Int): Boolean
}

fun VimEditor.endsWithNewLine(): Boolean {
  val textLength = this.fileSize().toInt()
  if (textLength == 0) return false
  return this.text()[textLength - 1] == '\n'
}

fun VimEditor.getLeadingCharacterOffset(line: Int, col: Int = 0): Int {
    val start = getLineStartOffset(line) + col
    val end = getLineEndOffset(line)
    val chars = text()
    var pos = end
    for (offset in start until end) {
        if (offset >= chars.length) {
            break
        }
        if (!Character.isWhitespace(chars[offset])) {
            pos = offset
            break
        }
    }
    return pos
}


fun VimEditor.normalizeVisualColumn(line: Int, col: Int, allowEnd: Boolean): Int {
    return (this.getVisualLineLength(line) - if (allowEnd) 0 else 1).coerceIn(0..col)
}

/**
 * Ensures that the supplied column number for the given logical line is within the range 0 (incl) and the
 * number of columns in the line (excl).
 *
 * @param this@normalizeColumn   The editor
 * @param line     The logical line number
 * @param col      The column number to normalize
 * @param allowEnd True if newline allowed
 * @return The normalized column number
 */
fun VimEditor.normalizeColumn(line: Int, col: Int, allowEnd: Boolean): Int {
  return (lineLength(line) - if (allowEnd) 0 else 1).coerceIn(0..col)
}

/**
 * Gets the number of characters on the specified visual line. This will be different than the number of visual
 * characters if there are "real" tabs in the line.
 *
 * @param this@getVisualLineLength The editor
 * @param line   The visual line within the file
 * @return The number of characters in the specified line
 */
fun VimEditor.getVisualLineLength(line: Int): Int {
    return lineLength(visualLineToLogicalLine(line))
}

/**
 * Gets the number of characters on the specified logical line. This will be different than the number of visual
 * characters if there are "real" tabs in the line.
 *
 * @return The number of characters in the specified line
 */
fun VimEditor.lineLength(logicalLine: Int): Int {
    return if (lineCount() == 0) {
        0
    } else {
        offsetToLogicalPosition(getLineEndOffset(logicalLine)).column.coerceAtLeast(0)
    }
}

/**
 * Converts a visual line number to a logical line number.
 *
 * @param this@visualLineToLogicalLine The editor
 * @param line   The visual line number to convert
 * @return The logical line number
 */
fun VimEditor.visualLineToLogicalLine(line: Int): Int {
    val logicalLine: Int = visualToLogicalPosition(VimVisualPosition(line, 0)).line
    return normalizeLine(logicalLine)
}

/**
 * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
 * (excl).
 *
 * @param this@normalizeLine The editor
 * @param line   The logical line number to normalize
 * @return The normalized logical line number
 */
fun VimEditor.normalizeLine(line: Int): Int {
  return line.coerceIn(0 until lineCount().coerceAtLeast(1))
}

/**
 * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
 * is true, the range will allow for the offset to be one past the last character on the line.
 *
 * @param this@normalizeOffset   The editor
 * @param line     The logical line number
 * @param offset   The offset to normalize
 * @param allowEnd true if the offset can be one past the last character on the line, false if not
 * @return The normalized column number
 */
fun VimEditor.normalizeOffset(line: Int, offset: Int, allowEnd: Boolean): Int {
  if (getFileSize(allowEnd) == 0) {
    return 0
  }
  val min: Int = getLineStartOffset(line)
  val max: Int = getLineEndOffset(line, allowEnd)
  return offset.coerceIn(min..max)
}

/**
 * Returns the offset of the end of the requested line.
 *
 * @param this@getLineEndOffset   The editor
 * @param line     The logical line to get the end offset for
 * @param allowEnd True include newline
 * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
 */
fun VimEditor.getLineEndOffset(line: Int, allowEnd: Boolean): Int {
  return if (line < 0) {
    0
  } else if (line >= lineCount()) {
    getFileSize(allowEnd)
  } else {
    val startOffset: Int = getLineStartOffset(line)
    val endOffset: Int = getLineEndOffset(line)
    endOffset - if (startOffset == endOffset || allowEnd) 0 else 1
  }
}

/**
 * Gets the actual number of characters in the file
 *
 * @param this@getFileSize            The editor
 * @param includeEndNewLine True include newline
 * @return The file's character count
 */
@Deprecated("please use the extension in EditorHelper.kt")
fun VimEditor.getFileSize(includeEndNewLine: Boolean): Int {
  val len: Int = text().length
  return if (includeEndNewLine || len == 0 || text()[len - 1] != '\n') len else len - 1
}

fun VimEditor.normalizeOffset(offset: Int, allowEnd: Boolean = true): Int {
  var offset = offset
  if (offset <= 0) {
    offset = 0
  }
  val textLength: Int = text().length
  if (offset > textLength) {
    offset = textLength
  }
  val line: Int = offsetToLogicalPosition(offset).line
  return normalizeOffset(line, offset, allowEnd)
}

/**
 * Ensures that the supplied visual line is within the range 0 (incl) and the number of visual lines in the file
 * (excl).
 *
 * @param this@normalizeVisualLine The editor
 * @param line   The visual line number to normalize
 * @return The normalized visual line number
 */
fun VimEditor.normalizeVisualLine(line: Int): Int {
  return (getVisualLineCount() - 1).coerceIn(0..line)
}

/**
 * Gets the number of visible lines in the editor. This will less then the actual number of lines in the file
 * if there are any collapsed folds.
 *
 * @param this@getVisualLineCount The editor
 * @return The number of visible lines in the file
 */
fun VimEditor.getVisualLineCount(): Int {
  val count = lineCount()
  return if (count == 0) 0 else injector.engineEditorHelper.logicalLineToVisualLine(this, count - 1) + 1
}
