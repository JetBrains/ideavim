/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import java.nio.CharBuffer

// TODO: [visual] try to remove all uses of visual line/position. This is an IntelliJ concept
interface EngineEditorHelper {
  // Keep it for now. See the IJ implementation, there are some hacks regarding that
  fun amountOfInlaysBeforeVisualPosition(editor: VimEditor, pos: VimVisualPosition): Int
  fun getVisualLineAtTopOfScreen(editor: VimEditor): Int
  fun getVisualLineAtBottomOfScreen(editor: VimEditor): Int
  fun getApproximateScreenWidth(editor: VimEditor): Int
  fun handleWithReadonlyFragmentModificationHandler(editor: VimEditor, exception: java.lang.Exception)
  fun pad(editor: VimEditor, context: ExecutionContext, line: Int, to: Int): String
  fun inlayAwareOffsetToVisualPosition(editor: VimEditor, offset: Int): VimVisualPosition
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

fun VimEditor.normalizeVisualColumn(visualLine: Int, col: Int, allowEnd: Boolean): Int {
  return (this.getVisualLineLength(visualLine) - if (allowEnd) 0 else 1).coerceIn(0..col)
}

/**
 * Ensures that the supplied column number for the given buffer line is within the range 0 (incl) and the
 * number of columns in the line (excl).
 *
 * @param this@normalizeColumn   The editor
 * @param line     The buffer line number
 * @param col      The column number to normalize
 * @param allowEnd True if newline allowed
 * @return The normalized column number
 */
fun VimEditor.normalizeColumn(line: Int, col: Int, allowEnd: Boolean): Int {
  return (lineLength(line) - if (allowEnd) 0 else 1).coerceIn(0..col)
}

/**
 * Gets the number of characters on the buffer line equivalent to the specified visual line.
 *
 * This will be different than the number of visual characters if there are "real" tabs in the line.
 *
 * @param this@getVisualLineLength The editor
 * @param visualLine   The visual line within the file
 * @return The number of characters in the specified line
 */
// TODO: [visual] try to get rid of this. It's probably not doing what you think it's doing
// This gets the length of the visual line's buffer line, not the length of the visual line. With soft wraps, these can
// be very different values.
fun VimEditor.getVisualLineLength(visualLine: Int): Int {
  return lineLength(visualLineToBufferLine(visualLine))
}

/**
 * Gets the number of characters on the specified buffer line. This will be different than the number of visual
 * characters if there are "real" tabs in the line.
 *
 * @return The number of characters in the specified line
 */
fun VimEditor.lineLength(line: Int): Int {
  return if (lineCount() == 0) {
    0
  } else {
    offsetToBufferPosition(getLineEndOffset(line)).column.coerceAtLeast(0)
  }
}

/**
 * Converts a visual line number to a buffer line number.
 *
 * @param this@visualLineToBufferLine The editor
 * @param visualLine   The visual line number to convert
 * @return The buffer line number
 */
fun VimEditor.visualLineToBufferLine(visualLine: Int): Int {
  val bufferLine: Int = visualPositionToBufferPosition(VimVisualPosition(visualLine, 0)).line
  return normalizeLine(bufferLine)
}

/**
 * Ensures that the supplied buffer line is within the range 0 (incl) and the number of buffer lines in the file (excl).
 *
 * @param this@normalizeLine The editor
 * @param line   The buffer line number to normalize
 * @return The normalized buffer line number
 */
fun VimEditor.normalizeLine(line: Int): Int {
  return line.coerceIn(0 until lineCount().coerceAtLeast(1))
}

/**
 * Ensures that the supplied offset for the given buffer line is within the range for the line. If allowEnd is true, the
 * range will allow for the offset to be one past the last character on the line.
 *
 * @param this@normalizeOffset   The editor
 * @param line     The buffer line number
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
 * @param line     The buffer line to get the end offset for
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
  val line: Int = offsetToBufferPosition(offset).line
  return normalizeOffset(line, offset, allowEnd)
}

/**
 * Ensures that the supplied visual line is within the range 0 (incl) and the number of visual lines in the file
 * (excl).
 *
 * @param this@normalizeVisualLine The editor
 * @param visualLine   The visual line number to normalize
 * @return The normalized visual line number
 */
fun VimEditor.normalizeVisualLine(visualLine: Int): Int {
  return visualLine.coerceIn(0 until getVisualLineCount().coerceAtLeast(1))
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
  return if (count == 0) 0 else this.bufferLineToVisualLine(count - 1) + 1
}

fun VimEditor.getLineStartForOffset(offset: Int): Int {
  val pos = offsetToBufferPosition(normalizeOffset(offset, true))
  return getLineStartOffset(pos.line)
}

/**
 * Gets the offset of the end of the line containing the supplied offset
 *
 * @param this@getLineEndForOffset The editor
 * @param offset The offset within the line
 * @return The offset of the line end
 */
fun VimEditor.getLineEndForOffset(offset: Int): Int {
  val pos = offsetToBufferPosition(normalizeOffset(offset, true))
  return getLineEndOffset(pos.line)
}

/**
 * Gets a string representation of the file for the supplied offset range
 *
 * @param this@getText The editor
 * @param start  The starting offset (inclusive)
 * @param end    The ending offset (exclusive)
 * @return The string, never null but empty if start == end
 */
fun VimEditor.getText(start: Int, end: Int): String {
  if (start == end) return ""
  val documentChars: CharSequence = text()
  return documentChars.subSequence(normalizeOffset(start), normalizeOffset(end)).toString()
}

fun VimEditor.getText(range: TextRange): String {
  val len = range.size()
  return if (len == 1) {
    val start = range.startOffset
    val end = range.endOffset
    getText(start, end)
  } else {
    val res = StringBuilder()
    val max = range.maxLength
    for (i in 0 until len) {
      if (i > 0 && res.isNotEmpty() && res[res.length - 1] != '\n') {
        res.append('\n')
      }
      val start = range.startOffsets[i]
      val end = range.endOffsets[i]
      val line = getText(start, end)
      if (line.isEmpty()) {
        for (j in 0 until max) {
          res.append(' ')
        }
      } else {
        res.append(line)
      }
    }
    res.toString()
  }
}

fun VimEditor.getOffset(line: Int, column: Int): Int {
  return bufferPositionToOffset(BufferPosition(line, column))
}
fun VimEditor.getLineBuffer(line: Int): CharBuffer {
  val start: Int = getLineStartOffset(line)
  return CharBuffer.wrap(text(), start, start + getLineEndOffset(line, true) - getLineStartOffset(line))
}
fun VimEditor.anyNonWhitespace(offset: Int, dir: Int): Boolean {
  val start: Int
  val end: Int
  val fileSize = fileSize().toInt()
  if (dir > 0) {
    start = (offset + 1).coerceAtMost(fileSize - 1)
    end = getLineEndForOffset(offset).coerceAtMost(fileSize - 1)
  } else {
    start = getLineStartForOffset(offset)
    end = (offset - 1).coerceAtLeast(0)
  }
  val chars: CharSequence = text()
  for (i in start..end) {
    if (!Character.isWhitespace(chars[i])) {
      return true
    }
  }
  return false
}

fun VimEditor.isLineEmpty(line: Int, allowBlanks: Boolean): Boolean {
  val chars: CharSequence = text()
  if (chars.isEmpty()) return true
  var offset: Int = getLineStartOffset(line)
  if (offset >= chars.length || chars[offset] == '\n') {
    return true
  } else if (allowBlanks) {
    while (offset < chars.length) {
      if (chars[offset] == '\n') {
        return true
      } else if (!Character.isWhitespace(chars[offset])) {
        return false
      }
      offset++
    }
  }
  return false
}
