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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
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
  // Set a max height on block inlays to be made visible at the top/bottom of a line when scrolling up/down. This
  // mitigates the visible area bouncing around too much and even pushing the cursor line off screen with large
  // multiline rendered doc comments, while still providing some visibility of the block inlay (e.g. Rider's single line
  // Code Vision)
  private static final int BLOCK_INLAY_MAX_LINE_HEIGHT = 3;

  public static @NotNull Rectangle getVisibleArea(final @NotNull Editor editor) {
    return editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
  }

  public static boolean scrollVertically(@NotNull Editor editor, int verticalOffset) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    final Rectangle area = scrollingModel.getVisibleAreaOnScrollingFinished();
    scrollingModel.scroll(area.x, verticalOffset);
    return scrollingModel.getVisibleAreaOnScrollingFinished().y != area.y;
  }

  public static void scrollHorizontally(@NotNull Editor editor, int horizontalOffset) {
    final ScrollingModel scrollingModel = editor.getScrollingModel();
    final Rectangle area = scrollingModel.getVisibleAreaOnScrollingFinished();
    scrollingModel.scroll(horizontalOffset, area.y);
  }

  public static int getVisualLineAtTopOfScreen(final @NotNull Editor editor) {
    final Rectangle visibleArea = getVisibleArea(editor);
    return getFullVisualLine(editor, visibleArea.y, visibleArea.y, visibleArea.y + visibleArea.height);
  }

  public static int getVisualLineAtMiddleOfScreen(final @NotNull Editor editor) {
    final Rectangle visibleArea = getVisibleArea(editor);
    return editor.yToVisualLine(visibleArea.y + (visibleArea.height / 2));
  }

  public static int getVisualLineAtBottomOfScreen(final @NotNull Editor editor) {
    final Rectangle visibleArea = getVisibleArea(editor);
    return getFullVisualLine(editor, visibleArea.y + visibleArea.height, visibleArea.y, visibleArea.y + visibleArea.height);
  }

  /**
   * Gets the number of characters on the current line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @return The number of characters in the current line
   */
  public static int getLineLength(final @NotNull Editor editor) {
    return getLineLength(editor, editor.getCaretModel().getLogicalPosition().line);
  }

  /**
   * Gets the number of characters on the specified logical line. This will be different than the number of visual
   * characters if there are "real" tabs in the line.
   *
   * @param editor The editor
   * @param logicalLine   The logical line within the file
   * @return The number of characters in the specified line
   */
  public static int getLineLength(final @NotNull Editor editor, final int logicalLine) {
    if (getLineCount(editor) == 0) {
      return 0;
    }
    else {
      return Math.max(0, editor.offsetToLogicalPosition(editor.getDocument().getLineEndOffset(logicalLine)).column);
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
  public static int getVisualLineLength(final @NotNull Editor editor, final int line) {
    return getLineLength(editor, visualLineToLogicalLine(editor, line));
  }

  /**
   * Gets the number of visible lines in the editor. This will less then the actual number of lines in the file
   * if there are any collapsed folds.
   *
   * @param editor The editor
   * @return The number of visible lines in the file
   */
  public static int getVisualLineCount(final @NotNull Editor editor) {
    int count = getLineCount(editor);
    return count == 0 ? 0 : logicalLineToVisualLine(editor, count - 1) + 1;
  }

  /**
   * Gets the number of actual lines in the file
   *
   * @param editor The editor
   * @return The file line count
   */
  public static int getLineCount(final @NotNull Editor editor) {
    return editor.getDocument().getLineCount();
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor The editor
   * @return The file's character count
   * @deprecated please use the extension in EditorHelper.kt
   */
  @Deprecated
  public static int getFileSize(final @NotNull Editor editor) {
    return getFileSize(editor, false);
  }

  /**
   * Gets the actual number of characters in the file
   *
   * @param editor            The editor
   * @param includeEndNewLine True include newline
   * @return The file's character count
   * @deprecated please use the extension in EditorHelper.kt
   */
  @Deprecated
  public static int getFileSize(final @NotNull Editor editor, final boolean includeEndNewLine) {
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
  public static int normalizeScrollOffset(final @NotNull Editor editor, int scrollOffset) {
    return Math.min(scrollOffset, getApproximateScreenHeight(editor) / 2);
  }

  /**
   * Best efforts to ensure the side scroll offset doesn't overlap itself and remains a sensible value. Inline inlays
   * can cause this to work incorrectly.
   * @param editor The editor to use to normalize the side scroll offset
   * @param sideScrollOffset The value of the 'sidescroll' option
   * @return The side scroll offset value to use
   */
  public static int normalizeSideScrollOffset(final @NotNull Editor editor, int sideScrollOffset) {
    return Math.min(sideScrollOffset, getApproximateScreenWidth(editor) / 2);
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time.
   *
   * Note that this value is only approximate and should be avoided whenever possible!
   *
   * @param editor The editor
   * @return The number of screen lines
   */
  public static int getApproximateScreenHeight(final @NotNull Editor editor) {
    return getVisibleArea(editor).height / editor.getLineHeight();
  }

  /**
   * Gets the number of characters that are visible on a screen line, based on screen width and assuming a fixed width
   * font. It does not include inlays or folds.
   *
   * Note that this value is only approximate and should be avoided whenever possible!
   *
   * @param editor The editor
   * @return The number of screen columns
   */
  public static int getApproximateScreenWidth(final @NotNull Editor editor) {
    return getVisibleArea(editor).width / EditorUtil.getPlainSpaceWidth(editor);
  }

  /**
   * Gets the visual column at the left of the screen for the given visual line.
   * @param editor The editor
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   * @return The visual column number
   */
  public static int getVisualColumnAtLeftOfScreen(final @NotNull Editor editor, int visualLine) {
    final Rectangle area = getVisibleArea(editor);
    return getFullVisualColumn(editor, area.x, editor.visualLineToY(visualLine), area.x, area.x + area.width);
  }

  /**
   * Gets the visual column at the right of the screen for the given visual line.
   * @param editor The editor
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   * @return The visual column number
   */
  public static int getVisualColumnAtRightOfScreen(final @NotNull Editor editor, int visualLine) {
    final Rectangle area = getVisibleArea(editor);
    return getFullVisualColumn(editor, area.x + area.width - 1, editor.visualLineToY(visualLine), area.x, area.x + area.width);
  }

  /**
   * Converts a visual line number to a logical line number.
   *
   * @param editor The editor
   * @param line   The visual line number to convert
   * @return The logical line number
   */
  public static int visualLineToLogicalLine(final @NotNull Editor editor, final int line) {
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
  public static int logicalLineToVisualLine(final @NotNull Editor editor, final int line) {
    if (editor instanceof EditorImpl) {
      // This is faster than simply calling Editor#logicalToVisualPosition
      return ((EditorImpl) editor).offsetToVisualLine(editor.getDocument().getLineStartOffset(line));
    }
    return editor.logicalToVisualPosition(new LogicalPosition(line, 0)).line;
  }

  public static int getOffset(final @NotNull Editor editor, final int line, final int column) {
    return editor.logicalPositionToOffset(new LogicalPosition(line, column));
  }

  /**
   * Returns the offset of the start of the requested line.
   *
   * @param editor The editor
   * @param line   The logical line to get the start offset for.
   * @return 0 if line is &lt 0, file size of line is bigger than file, else the start offset for the line
   */
  public static int getLineStartOffset(final @NotNull Editor editor, final int line) {
    if (line < 0) {
      return 0;
    }
    else if (line >= getLineCount(editor)) {
      return EditorHelperRt.getFileSize(editor);
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
  public static int getLineEndOffset(final @NotNull Editor editor, final int line, final boolean allowEnd) {
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
  public static int normalizeVisualLine(final @NotNull Editor editor, final int line) {
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
  public static int normalizeLine(final @NotNull Editor editor, final int line) {
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
  public static int normalizeVisualColumn(final @NotNull Editor editor, final int line, final int col, final boolean allowEnd) {
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
  public static int normalizeColumn(final @NotNull Editor editor, final int line, final int col, final boolean allowEnd) {
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
  public static int normalizeOffset(final @NotNull Editor editor, final int line, final int offset, final boolean allowEnd) {
    if (getFileSize(editor, allowEnd) == 0) {
      return 0;
    }

    int min = getLineStartOffset(editor, line);
    int max = getLineEndOffset(editor, line, allowEnd);
    return Math.max(Math.min(offset, max), min);
  }

  public static int normalizeOffset(final @NotNull Editor editor, final int offset) {
    return normalizeOffset(editor, offset, true);
  }

  public static int normalizeOffset(final @NotNull Editor editor, int offset, final boolean allowEnd) {
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


  public static int getLeadingCharacterOffset(final @NotNull Editor editor, final int line) {
    return getLeadingCharacterOffset(editor, line, 0);
  }

  public static int getLeadingCharacterOffset(final @NotNull Editor editor, final int line, final int col) {
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

  public static @NotNull String getLeadingWhitespace(final @NotNull Editor editor, final int line) {
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
  public static @Nullable Editor getEditor(final @Nullable VirtualFile file) {
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
  public static int visualPositionToOffset(final @NotNull Editor editor, final @NotNull VisualPosition pos) {
    // [202] return editor.visualPositionToOffset(pos);
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
  public static @NotNull String getText(final @NotNull Editor editor, final int start, final int end) {
    if (start == end) return "";
    final CharSequence documentChars = editor.getDocument().getCharsSequence();
    return documentChars.subSequence(normalizeOffset(editor, start), normalizeOffset(editor, end)).toString();
  }

  public static @NotNull String getText(final @NotNull Editor editor, final @NotNull TextRange range) {
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
  public static int getLineStartForOffset(final @NotNull Editor editor, final int offset) {
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
  public static int getLineEndForOffset(final @NotNull Editor editor, final int offset) {
    LogicalPosition pos = editor.offsetToLogicalPosition(normalizeOffset(editor, offset));
    return editor.getDocument().getLineEndOffset(pos.line);
  }

  private static int getLineCharCount(final @NotNull Editor editor, final int line) {
    return getLineEndOffset(editor, line, true) - getLineStartOffset(editor, line);
  }

  /**
   * Returns the text of the requested logical line
   *
   * @param editor The editor
   * @param line   The logical line to get the text for
   * @return The requested line
   */
  public static @NotNull String getLineText(final @NotNull Editor editor, final int line) {
    return getText(editor, getLineStartOffset(editor, line), getLineEndOffset(editor, line, true));
  }

  public static @NotNull CharBuffer getLineBuffer(final @NotNull Editor editor, final int line) {
    int start = getLineStartOffset(editor, line);
    return CharBuffer.wrap(editor.getDocument().getCharsSequence(), start, start + getLineCharCount(editor, line));
  }

  public static boolean isLineEmpty(final @NotNull Editor editor, final int line, final boolean allowBlanks) {
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

  public static @NotNull String pad(final @NotNull Editor editor, @NotNull DataContext context, int line, final int to) {
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
  public static @NotNull List<Caret> getOrderedCaretsList(@NotNull Editor editor) {
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
  public static void scrollVisualLineToCaretLocation(final @NotNull Editor editor, int visualLine) {
    final Rectangle visibleArea = getVisibleArea(editor);
    final int caretScreenOffset = editor.visualLineToY(editor.getCaretModel().getVisualPosition().line) - visibleArea.y;

    final int yVisualLine = editor.visualLineToY(visualLine);

    // We try to keep the caret in the same location, but only if there's enough space all around for the line's
    // inlays. E.g. caret on top screen line and the line has inlays above, or caret on bottom screen line and has
    // inlays below
    final int topInlayHeight = EditorUtil.getInlaysHeight(editor, visualLine, true);
    final int bottomInlayHeight = EditorUtil.getInlaysHeight(editor, visualLine, false);

    int inlayOffset = 0;
    if (topInlayHeight > caretScreenOffset) {
      inlayOffset = topInlayHeight;
    } else if (bottomInlayHeight > visibleArea.height - caretScreenOffset + editor.getLineHeight()) {
      inlayOffset = -bottomInlayHeight;
    }

    scrollVertically(editor, yVisualLine - caretScreenOffset - inlayOffset);
  }

  /**
   * Scrolls the editor to put the given visual line at the top of the current window. Ensures that any block inlay
   * elements above the given line are also visible.
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place at the top of the current window
   * @return Returns true if the window was moved
   */
  public static boolean scrollVisualLineToTopOfScreen(final @NotNull Editor editor, int visualLine) {

    final int inlayHeight = EditorUtil.getInlaysHeight(editor, visualLine, true);
    final int maxInlayHeight = BLOCK_INLAY_MAX_LINE_HEIGHT * editor.getLineHeight();
    int y = editor.visualLineToY(visualLine) - Math.min(inlayHeight, maxInlayHeight);

    // Normalise Y so that we don't try to scroll the editor to a location it can't reach. The editor will handle this,
    // but when we ask for the target location to move the caret to match, we'll get the incorrect value.
    // E.g. from line 100 of a 175 line, with line 100 at the top of screen, hit 100<C-E>. This should scroll line 175
    // to the top of the screen. With virtual space enabled, this is fine. If it's not enabled, we end up scrolling line
    // 146 to the top of the screen, but the caret thinks we're going to 175, and the caret is put in the wrong location
    // (To complicate things, this issue doesn't show up when running headless for tests)
    if (!editor.getSettings().isAdditionalPageAtBottom()) {
      // Get the max line number that can sit at the top of the screen
      final int editorHeight = getVisibleArea(editor).height;
      final int virtualSpaceHeight = editor.getSettings().getAdditionalLinesCount() * editor.getLineHeight();
      final int yLastLine = editor.visualLineToY(getLineCount(editor));  // last line + 1
      y = Math.min(y, yLastLine + virtualSpaceHeight - editorHeight);
    }
    return scrollVertically(editor, y);
  }

  /**
   * Scrolls the editor to place the given visual line in the middle of the current window.
   *
   * <p>Snaps the line to the nearest standard line height grid, which gives a good position for both an odd and even
   * number of lines and mimics what Vim does.</p>
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place in the middle of the current window
   */
  public static void scrollVisualLineToMiddleOfScreen(@NotNull Editor editor, int visualLine) {
    final int y = editor.visualLineToY(normalizeVisualLine(editor, visualLine));
    final int screenHeight = getVisibleArea(editor).height;
    final int lineHeight = editor.getLineHeight();
    scrollVertically(editor, y - ((screenHeight - lineHeight) / lineHeight / 2 * lineHeight));
  }

  /**
   * Scrolls the editor to place the given visual line at the bottom of the screen.
   *
   * <p>When we're moving the caret down a few lines and want to scroll to keep this visible, we need to be able to
   * place a line at the bottom of the screen. Due to block inlays, we can't do this by specifying a top line to scroll
   * to.</p>
   *
   * @param editor The editor to scroll
   * @param visualLine The visual line to place at the bottom of the current window
   * @return True if the editor was scrolled
   */
  public static boolean scrollVisualLineToBottomOfScreen(@NotNull Editor editor, int visualLine) {
    int exPanelHeight = 0;
    if (ExEntryPanel.getInstance().isActive()) {
      exPanelHeight = ExEntryPanel.getInstance().getHeight();
    }
    if (ExEntryPanel.getInstanceWithoutShortcuts().isActive()) {
      exPanelHeight += ExEntryPanel.getInstanceWithoutShortcuts().getHeight();
    }

    final int normalizedVisualLine = normalizeVisualLine(editor, visualLine);
    final int lineHeight = editor.getLineHeight();
    final int inlayHeight = EditorUtil.getInlaysHeight(editor, normalizedVisualLine, false);
    final int maxInlayHeight = BLOCK_INLAY_MAX_LINE_HEIGHT * lineHeight;
    final int y = editor.visualLineToY(normalizedVisualLine) + lineHeight + Math.min(inlayHeight, maxInlayHeight) + exPanelHeight;
    final Rectangle visibleArea = getVisibleArea(editor);
    return scrollVertically(editor, max(0, y - visibleArea.height));
  }

  public static void scrollColumnToLeftOfScreen(@NotNull Editor editor, int visualLine, int visualColumn) {
    int targetVisualColumn = visualColumn;

    // Requested column might be an inlay (because we do simple arithmetic on visual position, and inlays and folds have
    // a visual position). If it is an inlay and is related to following text, we want to display it, so use it as the
    // target column. If it's an inlay related to preceding text, we don't want to display it at the left of the screen,
    // show the next column instead
    Inlay<?> inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn));
    if (inlay != null && inlay.isRelatedToPrecedingText()) {
      targetVisualColumn = visualColumn + 1;
    }
    else if (visualColumn > 0) {
      inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn - 1));
      if (inlay != null && !inlay.isRelatedToPrecedingText()) {
        targetVisualColumn = visualColumn - 1;
      }
    }

    final int columnLeftX = editor.visualPositionToXY(new VisualPosition(visualLine, targetVisualColumn)).x;
    scrollHorizontally(editor, columnLeftX);
  }

  public static void scrollColumnToMiddleOfScreen(@NotNull Editor editor, int visualLine, int visualColumn) {
    final Point point = editor.visualPositionToXY(new VisualPosition(visualLine, visualColumn));
    final int screenWidth = getVisibleArea(editor).width;

    // Snap the column to the nearest standard column grid. This positions us nicely if there are an odd or even number
    // of columns. It also works with inline inlays and folds. It is slightly inaccurate for proportional fonts, but is
    // still a good solution. Besides, what kind of monster uses Vim with proportional fonts?
    final int standardColumnWidth = EditorUtil.getPlainSpaceWidth(editor);
    final int x = point.x - (screenWidth / standardColumnWidth / 2 * standardColumnWidth);
    scrollHorizontally(editor, x);
  }

  public static void scrollColumnToRightOfScreen(@NotNull Editor editor, int visualLine, int visualColumn) {
    int targetVisualColumn = visualColumn;

    // Requested column might be an inlay (because we do simple arithmetic on visual position, and inlays and folds have
    // a visual position). If it is an inlay and is related to preceding text, we want to display it, so use it as the
    // target column. If it's an inlay related to following text, we don't want to display it at the right of the
    // screen, show the previous column
    Inlay inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn));
    if (inlay != null && !inlay.isRelatedToPrecedingText()) {
      targetVisualColumn = visualColumn - 1;
    }
    else {
      // If the target column is followed by an inlay which is associated with it, make the inlay the target column so
      // it is visible
      inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn + 1));
      if (inlay != null && inlay.isRelatedToPrecedingText()) {
        targetVisualColumn = visualColumn + 1;
      }
    }

    // Scroll to the left edge of the target column, minus a screenwidth, and adjusted for inlays
    final int targetColumnRightX = editor.visualPositionToXY(new VisualPosition(visualLine, targetVisualColumn + 1)).x;
    final int screenWidth = getVisibleArea(editor).width;
    scrollHorizontally(editor, targetColumnRightX - screenWidth);
  }

  /**
   * Scrolls the screen up or down one or more pages.
   *
   * @param editor The editor to scroll
   * @param pages The number of pages to scroll. Positive is scroll down (lines move up). Negative is scroll up.
   * @return The visual line to place the caret on. -1 if the page wasn't scrolled at all.
   */
  public static int scrollFullPage(final @NotNull Editor editor, int pages) {
    if (pages > 0) {
      return scrollFullPageDown(editor, pages);
    }
    else if (pages < 0) {
      return scrollFullPageUp(editor, pages);
    }
    return -1;  // visual lines are 1-based
  }

  public static int lastColumnForLine(final @NotNull Editor editor, int line, boolean allowEnd) {
    return editor.offsetToVisualPosition(EditorHelper.getLineEndOffset(editor, line, allowEnd)).column;
  }

  public static int prepareLastColumn(@NotNull Caret caret) {
    // In most cases vimLastColumn contains a correct value. But it would be incorrect if IJ will move the caret
    //   and IdeaVim won't catch that. Here we try to detect and process this case.

    int vimLastColumn = UserDataManager.getVimLastColumn(caret);
    VisualPosition visualPosition = caret.getVisualPosition();

    // Current column equals to vimLastColumn. It's great, everything is okay.
    int inlayAwareOffset = InlayHelperKt.toInlayAwareOffset(visualPosition, caret);
    if (inlayAwareOffset == vimLastColumn) return vimLastColumn;

    Editor editor = caret.getEditor();
    boolean isEndAllowed = CommandStateHelper.isEndAllowedIgnoringOnemore(CommandStateHelper.getMode(editor));
    final LogicalPosition logicalPosition = caret.getLogicalPosition();
    int lastColumn = EditorHelper.lastColumnForLine(editor, logicalPosition.line, isEndAllowed);

    // Current column is somewhere at the end and vimLastColumn is greater than last column. This might be because
    //  the previous vertical motion was from a longer line. In this case we just return vimLastColumn. But it
    //  also might be the case decribed above: IJ did move the caret and IdeaVim didn't catch that. We don't process
    //  this case and just return vimLastColumn with the hope that this won't be a big pain for the user.
    // This logic can be polished in the future.
    if ((lastColumn == visualPosition.column || lastColumn + 1 == visualPosition.column) &&
        vimLastColumn > visualPosition.column) {
      return vimLastColumn;
    }

    // Okay here we know that something is definitely wrong. We set vimLastColumn to the current column.
    int updatedCol = InlayHelperKt.getInlayAwareVisualColumn(caret);
    UserDataManager.setVimLastColumn(caret, updatedCol);
    return updatedCol;
  }

  public static void updateLastColumn(@NotNull Caret caret, int prevLastColumn) {
    UserDataManager.setVimLastColumn(caret, prevLastColumn);
  }

  private static int scrollFullPageDown(final @NotNull Editor editor, int pages) {
    final Rectangle visibleArea = getVisibleArea(editor);
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

  private static int scrollFullPageUp(final @NotNull Editor editor, int pages) {
    final Rectangle visibleArea = getVisibleArea(editor);
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

  private static int getFullVisualLine(final @NotNull Editor editor, int y, int topBound, int bottomBound) {
    // Note that we ignore inlays here. We're interested in the bounds of the text line. Scrolling will handle inlays as
    // it sees fit (e.g. scrolling a line to the bottom will make sure inlays below the line are visible).
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

  private static int getFullVisualColumn(final @NotNull Editor editor, int x, int y, int leftBound, int rightBound) {
    // Mapping XY to a visual position will return the position of the closest character, rather than the position of
    // the character grid that contains the XY. This means two things. Firstly, we don't get back the visual position of
    // an inline inlay, and secondly, we can get the character to the left or right of X. This is the same logic for
    // positioning the caret when you click in the editor.
    // Note that visualPos.leansRight will be true for the right half side of the character grid
    VisualPosition closestVisualPosition = editor.xyToVisualPosition(new Point(x, y));

    // Make sure we get the character that contains this XY, not the editor's decision about closest character. The
    // editor will give us the next character if X is over half way through the character grid.
    int xActualLeft = editor.visualPositionToXY(closestVisualPosition).x;
    if (xActualLeft > x) {
      closestVisualPosition = getPreviousNonInlayVisualPosition(editor, closestVisualPosition);
      xActualLeft = editor.visualPositionToXY(closestVisualPosition).x;
    }

    if (xActualLeft >= leftBound) {
      final int xActualRight = editor.visualPositionToXY(new VisualPosition(closestVisualPosition.line, closestVisualPosition.column + 1)).x - 1;
      if (xActualRight <= rightBound) {
        return closestVisualPosition.column;
      }

      return getPreviousNonInlayVisualPosition(editor, closestVisualPosition).column;
    }
    else {
      return getNextNonInlayVisualPosition(editor, closestVisualPosition).column;
    }
  }

  private static VisualPosition getNextNonInlayVisualPosition(@NotNull Editor editor, VisualPosition position) {
    final InlayModel inlayModel = editor.getInlayModel();
    final int lineLength = EditorHelper.getVisualLineLength(editor, position.line);
    position = new VisualPosition(position.line, position.column + 1);
    while (position.column < lineLength && inlayModel.hasInlineElementAt(position)) {
      position = new VisualPosition(position.line, position.column + 1);
    }
    return position;
  }

  private static VisualPosition getPreviousNonInlayVisualPosition(@NotNull Editor editor, VisualPosition position) {
    if (position.column == 0) {
      return position;
    }
    final InlayModel inlayModel = editor.getInlayModel();
    position = new VisualPosition(position.line, position.column - 1);
    while (position.column >= 0 && inlayModel.hasInlineElementAt(position)) {
      position = new VisualPosition(position.line, position.column - 1);
    }
    return position;
  }

  /**
   * Gets the virtual file associated with this editor
   *
   * @param editor The editor
   * @return The virtual file for the editor
   */
  public static @Nullable VirtualFile getVirtualFile(@NotNull Editor editor) {
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

  /**
   * Checks if the document in the editor is modified.
   */
  public static boolean hasUnsavedChanges(@NotNull Editor editor) {
    int line = 0;
    Document document = editor.getDocument();

    while (line < document.getLineCount()) {
      if (document.isLineModified(line)) {
        return true;
      }
      line++;
    }

    return false;
  }
}
