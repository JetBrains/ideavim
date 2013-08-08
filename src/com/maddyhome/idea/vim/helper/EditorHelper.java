/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.CharBuffer;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper {
  private static final Logger logger = Logger.getInstance(EditorHelper.class.getName());

  public static int getVisualLineAtTopOfScreen(@NotNull final Editor editor) {
    int lh = editor.getLineHeight();
    return (editor.getScrollingModel().getVerticalScrollOffset() + lh - 1) / lh;
  }

  public static int getCurrentVisualScreenLine(@NotNull final Editor editor) {
    return editor.getCaretModel().getVisualPosition().line - getVisualLineAtTopOfScreen(editor) + 1;
  }

  /**
   * Gets the number of characters on the current line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @return The number of characters in the current line
   */
  public static int getLineLength(@NotNull final Editor editor) {
    return getLineLength(editor, editor.getCaretModel().getLogicalPosition().line);
  }

  /**
   * Gets the number of characters on the specified logical line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @param line   The logical line within the file
   * @return The number of characters in the specified line
   */
  public static int getLineLength(@NotNull final Editor editor, final int line) {
    if (getLineCount(editor) == 0) {
      return 0;
    }
    else {
      return Math.max(0, editor.offsetToLogicalPosition(editor.getDocument().getLineEndOffset(line)).column);
    }
  }

  /**
   * Gets the number of characters on the specified visual line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @param line   The visual line within the file
   * @return The number of characters in the specified line
   */
  public static int getVisualLineLength(@NotNull final Editor editor, final int line) {
    return getLineLength(editor, visualLineToLogicalLine(editor, line));
  }

  /**
   * Gets the number of visible lines in the editor. This will less then the actual number of lines in the file
   * if there are any collapsed folds.
   *
   * @param editor The editor
   * @return The number of visible lines in the file
   */
  public static int getVisualLineCount(@NotNull final Editor editor) {
    int count = getLineCount(editor);
    return count == 0 ? 0 : logicalLineToVisualLine(editor, count - 1) + 1;
  }

  /**
   * Gets the number of actual lines in the file
   *
   * @param editor The editor
   * @return The file line count
   */
  public static int getLineCount(@NotNull final Editor editor) {
    int len = editor.getDocument().getLineCount();
    if (editor.getDocument().getTextLength() > 0 &&
        editor.getDocument().getCharsSequence().charAt(editor.getDocument().getTextLength() - 1) == '\n') {
      len--;
    }

    return len;
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor The editor
   * @return The file's character count
   */
  public static int getFileSize(@NotNull final Editor editor) {
    return getFileSize(editor, false);
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor            The editor
   * @param includeEndNewLine True include newline
   * @return The file's character count
   */
  public static int getFileSize(@NotNull final Editor editor, final boolean includeEndNewLine) {
    final int len = editor.getDocument().getTextLength();
    return includeEndNewLine || len == 0 || editor.getDocument().getCharsSequence().charAt(len - 1) != '\n' ? len : len - 1;
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
   * nearest whole line if there is a partial line visible at the bottom of the screen.
   *
   * @param editor The editor
   * @return The number of screen lines
   */
  public static int getScreenHeight(@NotNull final Editor editor) {
    int lh = editor.getLineHeight();
    int height = editor.getScrollingModel().getVisibleArea().y +
                 editor.getScrollingModel().getVisibleArea().height -
                 getVisualLineAtTopOfScreen(editor) * lh;
    return height / lh;
  }

  /**
   * Gets the number of characters that are visible on a screen line
   *
   * @param editor The editor
   * @return The number of screen columns
   */
  public static int getScreenWidth(@NotNull final Editor editor) {
    Rectangle rect = editor.getScrollingModel().getVisibleArea();
    Point pt = new Point(rect.width, 0);
    VisualPosition vp = editor.xyToVisualPosition(pt);

    return vp.column;
  }

  /**
   * Gets the number of pixels per column of text.
   *
   * @param editor The editor
   * @return The number of pixels
   */
  public static int getColumnWidth(@NotNull final Editor editor) {
    Rectangle rect = editor.getScrollingModel().getVisibleArea();
    if (rect.width == 0) return 0;
    Point pt = new Point(rect.width, 0);
    VisualPosition vp = editor.xyToVisualPosition(pt);
    if (vp.column == 0) return 0;

    return rect.width / vp.column;
  }

  /**
   * Gets the column currently displayed at the left edge of the editor.
   *
   * @param editor The editor
   * @return The column number
   */
  public static int getVisualColumnAtLeftOfScreen(@NotNull final Editor editor) {
    int cw = getColumnWidth(editor);
    if (cw == 0) return 0;
    return (editor.getScrollingModel().getHorizontalScrollOffset() + cw - 1) / cw;
  }

  /**
   * Converts a visual line number to a logical line number.
   *
   * @param editor The editor
   * @param line   The visual line number to convert
   * @return The logical line number
   */
  public static int visualLineToLogicalLine(@NotNull final Editor editor, final int line) {
    int logicalLine = editor.visualToLogicalPosition(new VisualPosition(line, 0)).line;
    return normalizeLine(editor, logicalLine);
  }

  /**
   * Converts a logical line number to a visual line number. Several logical lines can map to the same
   * visual line when there are collapsed fold regions.
   *
   * @param editor The editor
   * @param line   The logical line number to convert
   * @return The visual line number
   */
  public static int logicalLineToVisualLine(@NotNull final Editor editor, final int line) {
    return editor.logicalToVisualPosition(new LogicalPosition(line, 0)).line;
  }

  /**
   * Returns the offset of the start of the requested line.
   *
   * @param editor The editor
   * @param line   The logical line to get the start offset for.
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
   */
  public static int getLineStartOffset(@NotNull final Editor editor, final int line) {
    if (line < 0) {
      return 0;
    }
    else if (line >= getLineCount(editor)) {
      return getFileSize(editor);
    }
    else {
      return editor.getDocument().getLineStartOffset(line);
    }
  }

  /**
   * Returns the offset of the end of the requested line.
   *
   * @param editor   The editor
   * @param line     The logical line to get the end offset for
   * @param allowEnd True include newline
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
   */
  public static int getLineEndOffset(@NotNull final Editor editor, final int line, final boolean allowEnd) {
    if (line < 0) {
      return 0;
    }
    else if (line >= getLineCount(editor)) {
      return getFileSize(editor, allowEnd);
    }
    else {
      return editor.getDocument().getLineEndOffset(line) - (allowEnd ? 0 : 1);
    }
  }

  /**
   * Ensures that the supplied visual line is within the range 0 (incl) and the number of visual lines in the file
   * (excl).
   *
   * @param editor The editor
   * @param line   The visual line number to normalize
   * @return The normalized visual line number
   */
  public static int normalizeVisualLine(@NotNull final Editor editor, final int line) {
    return Math.max(0, Math.min(line, getVisualLineCount(editor) - 1));
  }

  /**
   * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
   * (excl).
   *
   * @param editor The editor
   * @param line   The logical line number to normalize
   * @return The normalized logical line number
   */
  public static int normalizeLine(@NotNull final Editor editor, final int line) {
    return Math.max(0, Math.min(line, getLineCount(editor) - 1));
  }

  /**
   * Ensures that the supplied column number for the given visual line is within the range 0 (incl) and the
   * number of columns in the line (excl).
   *
   * @param editor   The editor
   * @param line     The visual line number
   * @param col      The column number to normalize
   * @param allowEnd True if newline allowed
   * @return The normalized column number
   */
  public static int normalizeVisualColumn(@NotNull final Editor editor, final int line, final int col, final boolean allowEnd) {
    return Math.max(0, Math.min(col, getVisualLineLength(editor, line) - (allowEnd ? 0 : 1)));
  }

  /**
   * Ensures that the supplied column number for the given logical line is within the range 0 (incl) and the
   * number of columns in the line (excl).
   *
   * @param editor   The editor
   * @param line     The logical line number
   * @param col      The column number to normalize
   * @param allowEnd True if newline allowed
   * @return The normalized column number
   */
  public static int normalizeColumn(@NotNull final Editor editor, final int line, final int col, final boolean allowEnd) {
    return Math.min(Math.max(0, getLineLength(editor, line) - (allowEnd ? 0 : 1)), col);
  }

  /**
   * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
   * is true, the range will allow for the offset to be one past the last character on the line.
   *
   * @param editor   The editor
   * @param line     The logical line number
   * @param offset   The offset to normalize
   * @param allowEnd true if the offset can be one past the last character on the line, false if not
   * @return The normalized column number
   */
  public static int normalizeOffset(@NotNull final Editor editor, final int line, final int offset, final boolean allowEnd) {
    if (getFileSize(editor, allowEnd) == 0) {
      return 0;
    }

    int min = getLineStartOffset(editor, line);
    int max = getLineEndOffset(editor, line, allowEnd);
    return Math.max(Math.min(offset, max), min);
  }

  public static int normalizeOffset(@NotNull final Editor editor, final int offset) {
    return normalizeOffset(editor, offset, true);
  }

  public static int normalizeOffset(@NotNull final Editor editor, int offset, final boolean allowEnd) {
    if (offset <= 0) {
      offset = 0;
    }
    final int textLength = editor.getDocument().getTextLength();
    if (offset > textLength) {
      offset = textLength;
    }
    final int line = editor.offsetToLogicalPosition(offset).line;
    return normalizeOffset(editor, line, offset, allowEnd);
  }


  public static int getLeadingCharacterOffset(@NotNull final Editor editor, final int line) {
    return getLeadingCharacterOffset(editor, line, 0);
  }

  public static int getLeadingCharacterOffset(@NotNull final Editor editor, final int line, final int col) {
    int start = getLineStartOffset(editor, line) + col;
    int end = getLineEndOffset(editor, line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = end;
    for (int offset = start; offset < end; offset++) {
      if (offset >= chars.length()) {
        break;
      }

      if (!Character.isWhitespace(chars.charAt(offset))) {
        pos = offset;
        break;
      }
    }

    return pos;
  }

  @NotNull
  public static String getLeadingWhitespace(@NotNull final Editor editor, final int line) {
    int start = getLineStartOffset(editor, line);
    int end = getLeadingCharacterOffset(editor, line);

    return editor.getDocument().getCharsSequence().subSequence(start, end).toString();
  }

  /**
   * Gets the editor for the virtual file within the editor manager.
   *
   * @param manager The file editor manager
   * @param file    The virtual file get the editor for
   * @return The matching editor or null if no match was found
   */
  @Nullable
  public static Editor getEditor(@NotNull final FileEditorManager manager, @Nullable final VirtualFile file) {
    if (file == null) {
      return null;
    }

    final Document doc = FileDocumentManager.getInstance().getDocument(file);
    if (doc == null) {
      return null;
    }
    final Editor[] editors = EditorFactory.getInstance().getEditors(doc, manager.getProject());
    if (editors.length > 0) {
      return editors[0];
    }

    return null;
  }

  /**
   * Converts a visual position to a file offset
   *
   * @param editor The editor
   * @param pos    The visual position to convert
   * @return The file offset of the visual position
   */
  public static int visualPositionToOffset(@NotNull final Editor editor, @NotNull final VisualPosition pos) {
    return editor.logicalPositionToOffset(editor.visualToLogicalPosition(pos));
  }

  /**
   * Gets a string representation of the file for the supplied offset range
   *
   * @param editor The editor
   * @param start  The starting offset (inclusive)
   * @param end    The ending offset (exclusive)
   * @return The string, never null but empty if start == end
   */
  @NotNull
  public static String getText(@NotNull final Editor editor, final int start, final int end) {
    if (start == end) return "";
    final CharSequence documentChars = editor.getDocument().getCharsSequence();
    return documentChars.subSequence(normalizeOffset(editor, start), normalizeOffset(editor, end)).toString();
  }

  @NotNull
  public static String getText(@NotNull final Editor editor, @NotNull final TextRange range) {
    int len = range.size();
    if (len == 1) {
      return getText(editor, range.getStartOffset(), range.getEndOffset());
    }
    else {
      StringBuffer res = new StringBuffer();
      int max = range.getMaxLength();

      for (int i = 0; i < len; i++) {
        if (i > 0) {
          res.append('\n');
        }
        String line = getText(editor, range.getStartOffsets()[i], range.getEndOffsets()[i]);
        if (line.length() == 0) {
          for (int j = 0; j < max; j++) {
            res.append(' ');
          }
        }
        else {
          res.append(line);
        }
      }

      return res.toString();
    }
  }

  /**
   * Gets the offset of the start of the line containing the supplied offset
   *
   * @param editor The editor
   * @param offset The offset within the line
   * @return The offset of the line start
   */
  public static int getLineStartForOffset(@NotNull final Editor editor, final int offset) {
    LogicalPosition pos = editor.offsetToLogicalPosition(normalizeOffset(editor, offset));
    return editor.getDocument().getLineStartOffset(pos.line);
  }

  /**
   * Gets the offset of the end of the line containing the supplied offset
   *
   * @param editor The editor
   * @param offset The offset within the line
   * @return The offset of the line end
   */
  public static int getLineEndForOffset(@NotNull final Editor editor, final int offset) {
    LogicalPosition pos = editor.offsetToLogicalPosition(normalizeOffset(editor, offset));
    return editor.getDocument().getLineEndOffset(pos.line);
  }

  public static int getLineCharCount(@NotNull final Editor editor, final int line) {
    return getLineEndOffset(editor, line, true) - getLineStartOffset(editor, line);
  }

  /**
   * Returns the text of the requested logical line
   *
   * @param editor The editor
   * @param line   The logical line to get the text for
   * @return The requested line
   */
  @NotNull
  public static String getLineText(@NotNull final Editor editor, final int line) {
    return getText(editor, getLineStartOffset(editor, line), getLineEndOffset(editor, line, true));
  }

  @NotNull
  public static CharacterPosition offsetToCharacterPosition(@NotNull final Editor editor, final int offset) {
    int line = editor.getDocument().getLineNumber(normalizeOffset(editor, offset));
    int col = offset - editor.getDocument().getLineStartOffset(line);
    return new CharacterPosition(line, col);
  }

  public static int characterPositionToOffset(@NotNull final Editor editor, @NotNull final CharacterPosition pos) {
    return editor.getDocument().getLineStartOffset(normalizeLine(editor, pos.line)) + pos.column;
  }

  @NotNull
  public static CharBuffer getLineBuffer(@NotNull final Editor editor, final int line) {
    int start = getLineStartOffset(editor, line);
    return CharBuffer.wrap(editor.getDocument().getCharsSequence(), start, start + getLineCharCount(editor, line));
  }

  public static boolean isLineEmpty(@NotNull final Editor editor, final int line, final boolean allowBlanks) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int offset = getLineStartOffset(editor, line);
    if (chars.charAt(offset) == '\n') {
      return true;
    }
    else if (allowBlanks) {
      for (; offset < chars.length(); offset++) {
        if (chars.charAt(offset) == '\n') {
          return true;
        }
        else if (!Character.isWhitespace(chars.charAt(offset))) {
          return false;
        }
      }
    }

    return false;
  }

  @NotNull
  public static String pad(@NotNull final Editor editor, int line, final int to) {
    StringBuffer res = new StringBuffer();

    int len = getLineLength(editor, line);
    if (logger.isDebugEnabled()) {
      logger.debug("line=" + line);
      logger.debug("len=" + len);
      logger.debug("to=" + to);
    }
    if (len < to) {
      // TODO - use tabs as needed
      for (int i = len; i < to; i++) {
        res.append(' ');
      }
    }

    return res.toString();
  }

  public static boolean canEdit(@NotNull final Project project, @NotNull final Editor editor) {
    return (editor.getDocument().isWritable() ||
            FileDocumentManager.fileForDocumentCheckedOutSuccessfully(editor.getDocument(), project)) &&
           !EditorData.isConsoleOutput(editor);
  }
}
