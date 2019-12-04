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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.max;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper {
  public static int getVisualLineAtTopOfScreen(@NotNull final Editor editor) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    return getFullVisualLine(editor, visibleArea.y, visibleArea.y, visibleArea.y + visibleArea.height);
  }

  public static int getVisualLineAtMiddleOfScreen(@NotNull final Editor editor) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    final Rectangle visibleArea = scrollingModel.getVisibleArea();
    return editor.yToVisualLine(visibleArea.y + (visibleArea.height / 2));
  }

  public static int getVisualLineAtBottomOfScreen(@NotNull final Editor editor) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    return getFullVisualLine(editor, visibleArea.y + visibleArea.height, visibleArea.y, visibleArea.y + visibleArea.height);
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
   * Best efforts to ensure that scroll offset doesn't overlap itself.
   *
   * This is a sanity check that works fine if there are no visible block inlays. Otherwise, the screen height depends
   * on what block inlays are currently visible in the target scroll area. Given a large enough scroll offset (or small
   * enough screen), we can return a scroll offset that takes us over the half way point and causes scrolling issues -
   * skipped lines, or unexpected movement.
   *
   * TODO: Investigate better ways of handling scroll offset
   * Perhaps apply scroll offset after the move itself? Calculate a safe offset based on a target area?
   *
   * @param editor The editor to use to normalize the scroll offset
   * @param scrollOffset The value of the 'scrolloff' option
   * @return The scroll offset value to use
   */
  public static int normalizeScrollOffset(@NotNull final Editor editor, int scrollOffset) {
    return Math.min(scrollOffset, getApproximateScreenHeight(editor) / 2);
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time. This is rounded down to the
   * nearest whole line if there is a partial line visible at the bottom of the screen.
   *
   * Note that this value is only approximate and should be avoided whenever possible!
   *
   * @param editor The editor
   * @return The number of screen lines
   */
  private static int getApproximateScreenHeight(@NotNull final Editor editor) {
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
    if (editor instanceof EditorImpl) {
      // This is faster than simply calling Editor#logicalToVisualPosition
      return ((EditorImpl) editor).offsetToVisualLine(editor.getDocument().getLineStartOffset(line));
    }
    return editor.logicalToVisualPosition(new LogicalPosition(line, 0)).line;
  }

  public static int getOffset(@NotNull final Editor editor, final int line, final int column) {
    return editor.logicalPositionToOffset(new LogicalPosition(line, column));
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
      final int startOffset = editor.getDocument().getLineStartOffset(line);
      final int endOffset = editor.getDocument().getLineEndOffset(line);
      return endOffset - (startOffset == endOffset || allowEnd ? 0 : 1);
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
   * @param file    The virtual file get the editor for
   * @return The matching editor or null if no match was found
   */
  @Nullable
  public static Editor getEditor(@Nullable final VirtualFile file) {
    if (file == null) {
      return null;
    }

    final Document doc = FileDocumentManager.getInstance().getDocument(file);
    if (doc == null) {
      return null;
    }
    final Editor[] editors = EditorFactory.getInstance().getEditors(doc);
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
      StringBuilder res = new StringBuilder();
      int max = range.getMaxLength();

      for (int i = 0; i < len; i++) {
        if (i > 0 && res.length() > 0 && res.charAt(res.length() - 1) != '\n') {
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

  private static int getLineCharCount(@NotNull final Editor editor, final int line) {
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
  public static CharBuffer getLineBuffer(@NotNull final Editor editor, final int line) {
    int start = getLineStartOffset(editor, line);
    return CharBuffer.wrap(editor.getDocument().getCharsSequence(), start, start + getLineCharCount(editor, line));
  }

  public static boolean isLineEmpty(@NotNull final Editor editor, final int line, final boolean allowBlanks) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    if (chars.length() == 0) return true;
    int offset = getLineStartOffset(editor, line);
    if (offset >= chars.length() || chars.charAt(offset) == '\n') {
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
  public static String pad(@NotNull final Editor editor, @NotNull DataContext context, int line, final int to) {
    final int len = getLineLength(editor, line);
    if(len >= to) return "";

    final int limit = to - len;
    return IndentConfig.create(editor, context).createIndentBySize(limit);
  }

  /**
   * Get list of all carets from the editor.
   *
   * @param editor The editor from which the carets are taken
   */
  @NotNull
  public static List<Caret> getOrderedCaretsList(@NotNull Editor editor) {
    @NotNull List<Caret> carets = editor.getCaretModel().getAllCarets();

    carets.sort(Comparator.comparingInt(Caret::getOffset));
    Collections.reverse(carets);

    return carets;
  }

  /**
   * Scrolls the editor to put the given visual line at the current caret location, relative to the screen.
   *
   * Due to block inlays, the caret location is maintained as a scroll offset, rather than the number of lines from the
   * top of the screen. This means the line offset can change if the number of inlays above the caret changes during
   * scrolling. It also means that after scrolling, the top screen line isn't guaranteed to be aligned to the top of
   * the screen, unlike most other motions ('M' is the only other motion that doesn't align the top line).
   *
   * This method will also move the caret location to ensure that any inlays attached above or below the target line are
   * fully visible.
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to scroll to the current caret location
   */
  public static void scrollVisualLineToCaretLocation(@NotNull final Editor editor, int visualLine) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    final Rectangle visibleArea = scrollingModel.getVisibleArea();
    final int caretScreenOffset = editor.visualLineToY(editor.getCaretModel().getVisualPosition().line) - visibleArea.y;

    final int yVisualLine = editor.visualLineToY(visualLine);

    // We try to keep the caret in the same location, but only if there's enough space all around for the line's
    // inlays. E.g. caret on top screen line and the line has inlays above, or caret on bottom screen line and has
    // inlays below
    final int topInlayHeight = EditorHelper.getHeightOfVisualLineInlays(editor, visualLine, true);
    final int bottomInlayHeight = EditorHelper.getHeightOfVisualLineInlays(editor, visualLine, false);

    int inlayOffset = 0;
    if (topInlayHeight > caretScreenOffset) {
      inlayOffset = topInlayHeight;
    } else if (bottomInlayHeight > visibleArea.height - caretScreenOffset + editor.getLineHeight()) {
      inlayOffset = -bottomInlayHeight;
    }

    scrollingModel.scrollVertically(yVisualLine - caretScreenOffset - inlayOffset);
  }

  /**
   * Scrolls the editor to put the given visual line at the top of the current window. Ensures that any block inlay
   * elements above the given line are also visible.
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place at the top of the current window
   * @return Returns true if the window was moved
   */
  public static boolean scrollVisualLineToTopOfScreen(@NotNull final Editor editor, int visualLine) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    int inlayHeight = getHeightOfVisualLineInlays(editor, visualLine, true);
    int y = editor.visualLineToY(visualLine) - inlayHeight;
    int verticalPos = scrollingModel.getVerticalScrollOffset();
    scrollingModel.scrollVertically(y);

    return verticalPos != scrollingModel.getVerticalScrollOffset();
  }

  /**
   * Scrolls the editor to place the given visual line in the middle of the current window.
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place in the middle of the current window
   */
  public static void scrollVisualLineToMiddleOfScreen(@NotNull Editor editor, int visualLine) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    int y = editor.visualLineToY(visualLine);
    int lineHeight = editor.getLineHeight();
    int height = scrollingModel.getVisibleArea().height;
    scrollingModel.scrollVertically(y - ((height - lineHeight) / 2));
  }

  /**
   * Scrolls the editor to place the given visual line at the bottom of the screen.
   *
   * When we're moving the caret down a few lines and want to scroll to keep this visible, we need to be able to place a
   * line at the bottom of the screen. Due to block inlays, we can't do this by specifying a top line to scroll to.
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place at the bottom of the current window
   * @return True if the editor was scrolled
   */
  public static boolean scrollVisualLineToBottomOfScreen(@NotNull Editor editor, int visualLine) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    int inlayHeight = getHeightOfVisualLineInlays(editor, visualLine, false);
    int exPanelHeight = 0;
    int exPanelWithoutShortcutsHeight = 0;
    if (ExEntryPanel.getInstance().isActive()) {
      exPanelHeight = ExEntryPanel.getInstance().getHeight();
    }
    if (ExEntryPanel.getInstanceWithoutShortcuts().isActive()) {
      exPanelWithoutShortcutsHeight = ExEntryPanel.getInstanceWithoutShortcuts().getHeight();
    }
    int y = editor.visualLineToY(visualLine);
    int verticalPos = scrollingModel.getVerticalScrollOffset();
    int height = inlayHeight + editor.getLineHeight() + exPanelHeight + exPanelWithoutShortcutsHeight;

    Rectangle visibleArea = scrollingModel.getVisibleArea();

    scrollingModel.scrollVertically(y - visibleArea.height + height);

    return verticalPos != scrollingModel.getVerticalScrollOffset();
  }

  /**
   * Scrolls the screen up or down one or more pages.
   *
   * @param editor The editor to scroll
   * @param pages The number of pages to scroll. Positive is scroll down (lines move up). Negative is scroll up.
   * @return The visual line to place the caret on. -1 if the page wasn't scrolled at all.
   */
  public static int scrollFullPage(@NotNull final Editor editor, int pages) {
    if (pages > 0) {
      return scrollFullPageDown(editor, pages);
    }
    else if (pages < 0) {
      return scrollFullPageUp(editor, pages);
    }
    return -1;  // visual lines are 1-based
  }

  public static int lastColumnForLine(@NotNull final Editor editor, int line, boolean allowEnd) {
    return editor.offsetToVisualPosition(EditorHelper.getLineEndOffset(editor, line, allowEnd)).column;
  }

  public static int prepareLastColumn(@NotNull Editor editor, @NotNull Caret caret) {
    VisualPosition pos = caret.getVisualPosition();
    final LogicalPosition logicalPosition = caret.getLogicalPosition();
    final int lastColumn = EditorHelper.lastColumnForLine(editor, logicalPosition.line, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));
    if (pos.column != lastColumn) {
      int lColumn = pos.column;
      int startOffset = editor.getDocument().getLineStartOffset(logicalPosition.line);
      lColumn -= max(0, editor.getInlayModel().getInlineElementsInRange(startOffset, caret.getOffset()).size());
      return lColumn;
    } else {
      return UserDataManager.getVimLastColumn(caret);
    }
  }

  public static void updateLastColumn(@NotNull Editor editor, @NotNull Caret caret, int prevLastColumn) {
    VisualPosition pos = caret.getVisualPosition();
    final LogicalPosition logicalPosition = caret.getLogicalPosition();
    final int lastColumn = EditorHelper.lastColumnForLine(editor, logicalPosition.line, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));
    int targetColumn;
    if (pos.column != lastColumn) {
      targetColumn = pos.column;
      int startOffset = editor.getDocument().getLineStartOffset(logicalPosition.line);
      targetColumn -= max(0, editor.getInlayModel().getInlineElementsInRange(startOffset, caret.getOffset()).size());
    }
    else {
      targetColumn = prevLastColumn;
    }
    UserDataManager.setVimLastColumn(caret, targetColumn);
  }

  private static int scrollFullPageDown(@NotNull final Editor editor, int pages) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    final int lineCount = getVisualLineCount(editor);

    if (editor.getCaretModel().getVisualPosition().line == lineCount - 1)
      return -1;

    int y = visibleArea.y + visibleArea.height;
    int topBound = visibleArea.y;
    int bottomBound = visibleArea.y + visibleArea.height;
    int line = 0;
    int caretLine = -1;

    for (int i = 0; i < pages; i++) {
      line = getFullVisualLine(editor, y, topBound, bottomBound);
      if (line >= lineCount - 1) {
        // If we're on the last page, end nicely on the last line, otherwise return the overrun so we can "beep"
        if (i == pages - 1) {
          caretLine = lineCount - 1;
        }
        else {
          caretLine = line;
        }
        break;
      }

      // The help page for 'scrolling' states that a page is the number of lines in the window minus two. Scrolling a
      // page adds this page length to the current line. Or in other words, scrolling down a page puts the last but one
      // line at the top of the next page.
      // E.g. a window showing lines 1-35 has a page size of 33, and scrolling down a page shows 34 as the top line
      line--;

      y = editor.visualLineToY(line);
      topBound = y;
      bottomBound = y + visibleArea.height;
      y = bottomBound;
      caretLine = line;
    }

    scrollVisualLineToTopOfScreen(editor, line);
    return caretLine;
  }

  private static int scrollFullPageUp(@NotNull final Editor editor, int pages) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    final int lineHeight = editor.getLineHeight();

    int y = visibleArea.y;
    int topBound = visibleArea.y;
    int bottomBound = visibleArea.y + visibleArea.height;
    int line = 0;
    int caretLine = -1;

    // We know pages is negative
    for (int i = pages; i < 0; i++) {
      // E.g. a window showing 73-107 has page size 33. Scrolling up puts 74 at the bottom of the screen
      line = getFullVisualLine(editor, y, topBound, bottomBound) + 1;
      if (line == 1) {
        break;
      }

      y = editor.visualLineToY(line);
      bottomBound = y + lineHeight;
      topBound = bottomBound - visibleArea.height;
      y = topBound;
      caretLine = line;
    }

    scrollVisualLineToBottomOfScreen(editor, line);
    return caretLine;
  }

  private static int getFullVisualLine(@NotNull final Editor editor, int y, int topBound, int bottomBound) {
    int line = editor.yToVisualLine(y);
    int yActual = editor.visualLineToY(line);
    if (yActual < topBound) {
      line++;
    }
    else if (yActual + editor.getLineHeight() > bottomBound) {
      line--;
    }
    return line;
  }

  private static int getHeightOfVisualLineInlays(@NotNull final Editor editor, int visualLine, boolean above) {
    InlayModel inlayModel = editor.getInlayModel();
    List<Inlay> inlays = inlayModel.getBlockElementsForVisualLine(visualLine, above);
    int inlayHeight = 0;
    for (Inlay inlay : inlays) {
      inlayHeight += inlay.getHeightInPixels();
    }
    return inlayHeight;
  }

  /**
   * Gets the virtual file associated with this editor
   *
   * @param editor The editor
   * @return The virtual file for the editor
   */
  @Nullable
  public static VirtualFile getVirtualFile(@NotNull Editor editor) {
    return FileDocumentManager.getInstance().getFile(editor.getDocument());
  }

  /**
   * Checks if editor is file editor, also it takes into account that editor can be placed in editors hierarchy
   */
  public static boolean isFileEditor(@NotNull Editor editor) {
    final VirtualFile virtualFile = getVirtualFile(editor);
    return virtualFile != null && !(virtualFile instanceof LightVirtualFile);
  }

  /**
   * Checks if the editor is a diff window
   */
  public static boolean isDiffEditor(@NotNull Editor editor) {
    return editor.getEditorKind() == EditorKind.DIFF;
  }
}
