package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.TextRange;

import java.awt.*;
import java.nio.CharBuffer;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper {
  /**
   * Gets the visual line number the cursor is on
   *
   * @param editor The editor
   * @return The cursor's visual line number
   */
  public static int getCurrentVisualLine(Editor editor) {
    return editor.getCaretModel().getVisualPosition().line;
  }

  /**
   * Gets the visual column number the cursor is on
   *
   * @param editor The editor
   * @return The cursor's visual column number
   */
  public static int getCurrentVisualColumn(Editor editor) {
    return editor.getCaretModel().getVisualPosition().column;
  }

  /**
   * Gets the logical line number the cursor is on
   *
   * @param editor The editor
   * @return The cursor's logical line number
   */
  public static int getCurrentLogicalLine(Editor editor) {
    return editor.getCaretModel().getLogicalPosition().line;
  }

  /**
   * Gets the logical column number the cursor is on
   *
   * @param editor The editor
   * @return The cursor's logical column number
   */
  public static int getCurrentLogicalColumn(Editor editor) {
    return editor.getCaretModel().getLogicalPosition().column;
  }

  public static int getVisualLineAtTopOfScreen(Editor editor) {
    int lh = editor.getLineHeight();
    return (editor.getScrollingModel().getVerticalScrollOffset() + lh - 1) / lh;
  }

  public static int getCurrentVisualScreenLine(Editor editor) {
    return getCurrentVisualLine(editor) - getVisualLineAtTopOfScreen(editor) + 1;
  }

  /**
   * Gets the number of characters on the current line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @return The number of characters in the current line
   */
  public static int getLineLength(Editor editor) {
    int lline = getCurrentLogicalLine(editor);

    return getLineLength(editor, lline);
  }

  /**
   * Gets the number of characters on the specified logical line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @param lline  The logical line within the file
   * @return The number of characters in the specified line
   */
  public static int getLineLength(Editor editor, int lline) {
    if (getLineCount(editor) == 0) {
      return 0;
    }
    else {
      return Math.max(0, editor.offsetToLogicalPosition(editor.getDocument().getLineEndOffset(lline)).column);
    }
  }

  /*
  public static int getMaximumLineLength(Editor editor)
  {
      int width = editor.getScrollingModel().
  }
  */

  /**
   * Gets the number of characters on the specified visual line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @param vline  The visual line within the file
   * @return The number of characters in the specified line
   */
  public static int getVisualLineLength(Editor editor, int vline) {
    int lline = visualLineToLogicalLine(editor, vline);
    return getLineLength(editor, lline);
  }

  /**
   * Gets the number of visible lines in the editor. This will less then the actual number of lines in the file
   * if there are any collapsed folds.
   *
   * @param editor The editor
   * @return The number of visible lines in the file
   */
  public static int getVisualLineCount(Editor editor) {
    int count = getLineCount(editor);
    return count == 0 ? 0 : logicalLineToVisualLine(editor, count - 1) + 1;
  }

  /**
   * Gets the number of actual lines in the file
   *
   * @param editor The editor
   * @return The file line count
   */
  public static int getLineCount(Editor editor) {
    int len = editor.getDocument().getLineCount();
    if (editor.getDocument().getTextLength() > 0 &&
        EditorHelper.getDocumentChars(editor).charAt(editor.getDocument().getTextLength() - 1) == '\n') {
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
  public static int getFileSize(Editor editor) {
    return getFileSize(editor, false);
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor The editor
   * @param incEnd True include newline
   * @return The file's character count
   */
  public static int getFileSize(Editor editor, boolean incEnd) {
    Document doc = editor.getDocument();
    int len = doc.getTextLength();
    if (!incEnd && len >= 1 && EditorHelper.getDocumentChars(editor).charAt(len - 1) == '\n') {
      len--;
    }

    return len;
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
   * nearest whole line if there is a parial line visible at the bottom of the screen.
   *
   * @param editor The editor
   * @return The number of screen lines
   */
  public static int getScreenHeight(Editor editor) {
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
  public static int getScreenWidth(Editor editor) {
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
  public static int getColumnWidth(Editor editor) {
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
  public static int getVisualColumnAtLeftOfScreen(Editor editor) {
    int cw = getColumnWidth(editor);
    if (cw == 0) return 0;
    return (editor.getScrollingModel().getHorizontalScrollOffset() + cw - 1) / cw;
  }

  /**
   * Converts a visual line number to a logical line number.
   *
   * @param editor The editor
   * @param vline  The visual line number to convert
   * @return The logical line number
   */
  public static int visualLineToLogicalLine(Editor editor, int vline) {
    int lline = editor.visualToLogicalPosition(new VisualPosition(vline, 0)).line;
    return normalizeLine(editor, lline);
  }

  /**
   * Converts a logical line number to a visual line number. Several logical lines can map to the same
   * visual line when there are collapsed fold regions.
   *
   * @param editor The editor
   * @param lline  The logical line number to convert
   * @return The visual line number
   */
  public static int logicalLineToVisualLine(Editor editor, int lline) {
    return editor.logicalToVisualPosition(new LogicalPosition(lline, 0)).line;
  }

  /**
   * Returns the offset of the start of the requested line.
   *
   * @param editor The editor
   * @param lline  The logical line to get the start offset for.
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
   */
  public static int getLineStartOffset(Editor editor, int lline) {
    if (lline < 0) {
      return 0;
    }
    else if (lline >= getLineCount(editor)) {
      return getFileSize(editor);
    }
    else {
      return editor.getDocument().getLineStartOffset(lline);
    }
  }

  /**
   * Returns the offset of the end of the requested line.
   *
   * @param editor The editor
   * @param lline  The logical line to get the end offset for.
   * @param incEnd True include newline
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the end offset for the line
   */
  public static int getLineEndOffset(Editor editor, int lline, boolean incEnd) {
    if (lline < 0) {
      return 0;
    }
    else if (lline >= getLineCount(editor)) {
      return getFileSize(editor, incEnd);
    }
    else {
      return editor.getDocument().getLineEndOffset(lline) - (incEnd ? 0 : 1);
    }
  }

  /**
   * Ensures that the supplied visual line is within the range 0 (incl) and the number of visual lines in the file
   * (excl).
   *
   * @param editor The editor
   * @param vline  The visual line number to normalize
   * @return The normalized visual line number
   */
  public static int normalizeVisualLine(Editor editor, int vline) {
    vline = Math.max(0, Math.min(vline, getVisualLineCount(editor) - 1));

    return vline;
  }

  /**
   * Ensures that the supplied logical line is within the range 0 (incl) and the number of logical lines in the file
   * (excl).
   *
   * @param editor The editor
   * @param lline  The logical line number to normalize
   * @return The normalized logical line number
   */
  public static int normalizeLine(Editor editor, int lline) {
    lline = Math.max(0, Math.min(lline, getLineCount(editor) - 1));

    return lline;
  }

  /**
   * Ensures that the supplied column number for the given visual line is within the range 0 (incl) and the
   * number of columns in the line (excl).
   *
   * @param editor   The editor
   * @param vline    The visual line number
   * @param col      The column number to normalize
   * @param allowEnd True if newline allowed
   * @return The normalized column number
   */
  public static int normalizeVisualColumn(Editor editor, int vline, int col, boolean allowEnd) {
    col = Math.max(0, Math.min(col, getVisualLineLength(editor, vline) - (allowEnd ? 0 : 1)));

    return col;
  }

  /**
   * Ensures that the supplied column number for the given logical line is within the range 0 (incl) and the
   * number of columns in the line (excl).
   *
   * @param editor   The editor
   * @param lline    The logical line number
   * @param col      The column number to normalize
   * @param allowEnd True if newline allowed
   * @return The normalized column number
   */
  public static int normalizeColumn(Editor editor, int lline, int col, boolean allowEnd) {
    col = Math.min(Math.max(0, getLineLength(editor, lline) - (allowEnd ? 0 : 1)), col);

    return col;
  }

  /**
   * Ensures that the supplied offset for the given logical line is within the range for the line. If allowEnd
   * is true, the range will allow for the offset to be one past the last character on the line.
   *
   * @param editor   The editor
   * @param lline    The logical line number
   * @param offset   The offset to normalize
   * @param allowEnd true if the offset can be one past the last character on the line, false if not
   * @return The normalized column number
   */
  public static int normalizeOffset(Editor editor, int lline, int offset, boolean allowEnd) {
    if (getFileSize(editor, allowEnd) == 0) {
      return 0;
    }

    int min = getLineStartOffset(editor, lline);
    int max = getLineEndOffset(editor, lline, allowEnd);
    offset = Math.max(Math.min(offset, max), min);

    return offset;
  }

  public static int normalizeOffset(Editor editor, int offset, boolean allowEnd) {
    int lline = editor.offsetToLogicalPosition(offset).line;

    return normalizeOffset(editor, lline, offset, allowEnd);
  }

  public static int getLeadingCharacterOffset(Editor editor, int lline) {
    return getLeadingCharacterOffset(editor, lline, 0);
  }

  public static int getLeadingCharacterOffset(Editor editor, int lline, int col) {
    int start = getLineStartOffset(editor, lline) + col;
    int end = getLineEndOffset(editor, lline, true);
    CharSequence chars = EditorHelper.getDocumentChars(editor);
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

  public static String getLeadingWhitespace(Editor editor, int lline) {
    int start = getLineStartOffset(editor, lline);
    int end = getLeadingCharacterOffset(editor, lline);

    return EditorHelper.getDocumentChars(editor).subSequence(start, end).toString();
  }

  /**
   * Gets the editor for the virtual file within the editor mananger.
   *
   * @param manager The file editor manager
   * @param file    The virtual file get the editor for
   * @return The matching editor or null if no match was found
   */
  public static Editor getEditor(FileEditorManager manager, VirtualFile file) {
    if (file == null) {
      return null;
    }

    Document doc = FileDocumentManager.getInstance().getDocument(file);
    Editor[] editors = EditorFactory.getInstance().getEditors(doc, EditorData.getProject(manager));
    if (editors != null && editors.length > 0) {
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
  public static int visualPostionToOffset(Editor editor, VisualPosition pos) {
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
  public static String getText(Editor editor, int start, int end) {
    // Fix for IOOBE
    final CharSequence documentChars = EditorHelper.getDocumentChars(editor);
    if (!(0 <= start && start < end && start < documentChars.length() && 0 <= end && end <= documentChars.length())) {
      return "";
    }
    return documentChars.subSequence(start, end).toString();
  }

  public static String getText(Editor editor, TextRange range) {
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
  public static int getLineStartForOffset(Editor editor, int offset) {
    LogicalPosition pos = editor.offsetToLogicalPosition(offset);
    return editor.getDocument().getLineStartOffset(pos.line);
  }

  /**
   * Gets the offset of the end of the line containing the supplied offset
   *
   * @param editor The editor
   * @param offset The offset within the line
   * @return The offset of the line end
   */
  public static int getLineEndForOffset(Editor editor, int offset) {
    if (logger.isDebugEnabled()) logger.debug("editor=" + editor);
    LogicalPosition pos = editor.offsetToLogicalPosition(offset);
    return editor.getDocument().getLineEndOffset(pos.line);
  }

  public static int getLineCharCount(Editor editor, int lline) {
    return getLineEndOffset(editor, lline, true) - getLineStartOffset(editor, lline);
  }

  /**
   * Returns the text of the requested logical line
   *
   * @param editor The editor
   * @param lline  The logical line to get the text for
   * @return The requested line
   */
  public static String getLineText(Editor editor, int lline) {
    return getText(editor, getLineStartOffset(editor, lline), getLineEndOffset(editor, lline, true));
  }

  public static CharacterPosition offsetToCharacterPosition(Editor editor, int offset) {
    int line = editor.getDocument().getLineNumber(offset);
    int col = offset - editor.getDocument().getLineStartOffset(line);

    return new CharacterPosition(line, col);
  }

  public static int characterPositionToOffset(Editor editor, CharacterPosition pos) {
    return editor.getDocument().getLineStartOffset(pos.line) + pos.column;
  }

  public static CharBuffer getLineBuffer(Editor editor, int lline) {
    int start = getLineStartOffset(editor, lline);
    return CharBuffer.wrap(EditorHelper.getDocumentChars(editor), start, start + getLineCharCount(editor, lline));
  }

  public static boolean isLineEmpty(Editor editor, int lline, boolean allowBlanks) {
    CharSequence chars = EditorHelper.getDocumentChars(editor);
    int offset = getLineStartOffset(editor, lline);
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

  public static String pad(Editor editor, int lline, int to) {
    StringBuffer res = new StringBuffer();

    int len = getLineLength(editor, lline);
    if (logger.isDebugEnabled()) {
      logger.debug("lline=" + lline);
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

  public static CharSequence getDocumentChars(Editor editor) {
    return editor.getDocument().getCharsSequence(); // API change - don't merge
  }

  public static boolean canEdit(Project project, Editor editor) {
    return (editor.getDocument().isWritable() ||  // API change - don't merge
            FileDocumentManager.fileForDocumentCheckedOutSuccessfully(editor.getDocument(), project)) &&  // API change - don't merge
           !EditorData.isConsoleOutput(editor);
  }

  private static final Logger logger = Logger.getInstance(EditorHelper.class.getName());
}
