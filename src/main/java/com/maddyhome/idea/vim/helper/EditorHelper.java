/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.maddyhome.idea.vim.api.EngineEditorHelperKt;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * This is a set of helper methods for working with editors. All line and column values are zero based.
 */
public class EditorHelper {
  // Set a max height on block inlays to be made visible at the top/bottom of a line when scrolling up/down. This
  // mitigates the visible area bouncing around too much and even pushing the cursor line off screen with large
  // multiline rendered doc comments, while still providing some visibility of the block inlay (e.g. Rider's single line
  // Code Vision)
  private static final int BLOCK_INLAY_MAX_LINE_HEIGHT = 3;

  public static @NotNull
  Rectangle getVisibleArea(final @NotNull Editor editor) {
    return editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
  }

  //("Use extension function with the same name on VimEditor")
  @Deprecated
  public static boolean isLineEmpty(final @NotNull Editor editor, final int line, final boolean allowBlanks) {
    return EngineEditorHelperKt.isLineEmpty(new IjVimEditor(editor), line, allowBlanks);
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
    // The editor will return line numbers of virtual space if the text doesn't reach the end of the visible area
    // (either because it's too short, or it's been scrolled up)
    final int lastLineBaseline = editor.logicalPositionToXY(new LogicalPosition(new IjVimEditor(editor).lineCount(), 0)).y;
    final Rectangle visibleArea = getVisibleArea(editor);
    final int height = min(lastLineBaseline - visibleArea.y, visibleArea.height);
    return editor.yToVisualLine(visibleArea.y + (height / 2));
  }

  public static int getNonNormalizedVisualLineAtBottomOfScreen(final @NotNull Editor editor) {
    // The editor will return line numbers of virtual space if the text doesn't reach the end of the visible area
    // (either because it's too short, or it's been scrolled up)
    final Rectangle visibleArea = getVisibleArea(editor);
    return getFullVisualLine(editor, visibleArea.y + visibleArea.height, visibleArea.y,
      visibleArea.y + visibleArea.height);
  }

  public static int getVisualLineAtBottomOfScreen(final @NotNull Editor editor) {
    final int line = getNonNormalizedVisualLineAtBottomOfScreen(editor);
    return EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor), line);
  }

  /**
   * COMPATIBILITY-LAYER: Created a function
   * Please see: <a href="https://jb.gg/zo8n0r">doc</a>
   */
  public static int getVisualLineCount(final @NotNull Editor editor) {
    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    return EngineEditorHelperKt.getVisualLineCount(editor1);
  }

  /**
   * Best efforts to ensure that scroll offset doesn't overlap itself.
   * <p>
   * This is a sanity check that works fine if there are no visible block inlays. Otherwise, the screen height depends
   * on what block inlays are currently visible in the target scroll area. Given a large enough scroll offset (or small
   * enough screen), we can return a scroll offset that takes us over the half way point and causes scrolling issues -
   * skipped lines, or unexpected movement.
   * <p>
   * TODO: Investigate better ways of handling scroll offset
   * Perhaps apply scroll offset after the move itself? Calculate a safe offset based on a target area?
   *
   * @param editor       The editor to use to normalize the scroll offset
   * @param scrollOffset The value of the 'scrolloff' option
   * @return The scroll offset value to use
   */
  public static int normalizeScrollOffset(final @NotNull Editor editor, int scrollOffset) {
    return Math.min(scrollOffset, getApproximateScreenHeight(editor) / 2);
  }

  /**
   * Best efforts to ensure the side scroll offset doesn't overlap itself and remains a sensible value. Inline inlays
   * can cause this to work incorrectly.
   *
   * @param editor           The editor to use to normalize the side scroll offset
   * @param sideScrollOffset The value of the 'sidescroll' option
   * @return The side scroll offset value to use
   */
  public static int normalizeSideScrollOffset(final @NotNull Editor editor, int sideScrollOffset) {
    return Math.min(sideScrollOffset, getApproximateScreenWidth(editor) / 2);
  }

  /**
   * Gets the number of lines than can be displayed on the screen at one time.
   * <p>
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
   * <p>
   * Note that this value is only approximate and should be avoided whenever possible!
   * </p>
   *
   * @param editor The editor
   * @return The number of screen columns
   */
  public static int getApproximateScreenWidth(final @NotNull Editor editor) {
    return (int) (getVisibleArea(editor).width / getPlainSpaceWidthFloat(editor));
  }

  /**
   * Gets the width of the space character in the editor's plain font as a float.
   * <p>
   * Font width can be fractional, but {@link EditorUtil#getPlainSpaceWidth(Editor)} returns it as an int, which can
   * lead to rounding errors.
   * </p>
   *
   * @param editor The editor
   * @return The width of the space character in the editor's plain font in pixels. It might be a fractional value.
   */
  public static float getPlainSpaceWidthFloat(final @NotNull Editor editor) {
    return EditorUtil.fontForChar(' ', Font.PLAIN, editor).charWidth2D(' ');
  }

  /**
   * Gets the visual column at the left of the screen for the given visual line.
   *
   * @param editor     The editor
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   * @return The visual column number
   */
  public static int getVisualColumnAtLeftOfDisplay(final @NotNull Editor editor, int visualLine) {
    final Rectangle area = getVisibleArea(editor);
    return getFullVisualColumn(editor, area.x, editor.visualLineToY(visualLine), area.x, area.x + area.width);
  }

  /**
   * Gets the visual column at the right of the screen for the given visual line.
   *
   * @param editor     The editor
   * @param visualLine The visual line to use to check for inlays and support non-proportional fonts
   * @return The visual column number
   */
  public static int getVisualColumnAtRightOfDisplay(final @NotNull Editor editor, int visualLine) {
    final Rectangle area = getVisibleArea(editor);
    return getFullVisualColumn(editor, area.x + area.width - 1, editor.visualLineToY(visualLine), area.x,
      area.x + area.width);
  }

  /**
   * Gets the editor for the virtual file within the editor manager.
   *
   * @param file The virtual file get the editor for
   * @return The matching editor or null if no match was found
   */
  public static @Nullable
  Editor getEditor(final @Nullable VirtualFile file) {
    if (file == null) {
      return null;
    }

    final Document doc = FileDocumentManager.getInstance().getDocument(file);
    if (doc == null) {
      return null;
    }
    final List<Editor> editors = HelperKt.localEditors(doc);
    if (editors.size() > 0) {
      return editors.get(0);
    }

    return null;
  }

  public static @NotNull
  String pad(final @NotNull Editor editor,
             @NotNull DataContext context,
             int line,
             final int to) {
    final int len = EngineEditorHelperKt.lineLength(new IjVimEditor(editor), line);
    if (len >= to) return "";

    final int limit = to - len;
    return IndentConfig.create(editor, context).createIndentBySize(limit);
  }

  /**
   * Get list of all carets from the editor.
   *
   * @param editor The editor from which the carets are taken
   */
  public static @NotNull
  List<Caret> getOrderedCaretsList(@NotNull Editor editor) {
    @NotNull List<Caret> carets = editor.getCaretModel().getAllCarets();

    carets.sort(Comparator.comparingInt(Caret::getOffset));
    Collections.reverse(carets);

    return carets;
  }

  /**
   * Scrolls the editor to put the given visual line at the current caret location, relative to the screen, as long as
   * this doesn't add virtual space to the bottom of the file.
   * <p>
   * Due to block inlays, the caret location is maintained as a scroll offset, rather than the number of lines from the
   * top of the screen. This means the line offset can change if the number of inlays above the caret changes during
   * scrolling. It also means that after scrolling, the top screen line isn't guaranteed to be aligned to the top of
   * the screen, unlike most other motions ('M' is the only other motion that doesn't align the top line).
   * <p>
   * This method will also move the caret location to ensure that any inlays attached above or below the target line are
   * fully visible.
   *
   * @param editor     The editor to scroll
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

    // Scroll the given visual line to the caret location, but do not scroll down passed the end of file, or the current
    // virtual space at the bottom of the screen
    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    final int lastVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1) - 1;
    final int yBottomLineOffset = max(getOffsetToScrollVisualLineToBottomOfScreen(editor, lastVisualLine), visibleArea.y);
    scrollVertically(editor, min(yVisualLine - caretScreenOffset - inlayOffset, yBottomLineOffset));
  }

  /**
   * Scrolls the editor to put the given visual line at the top of the current window. Ensures that any block inlay
   * elements above the given line are also visible.
   *
   * @param editor     The editor to scroll
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
      final int yLastLine = editor.visualLineToY(new IjVimEditor(editor).lineCount());  // last line + 1
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
   * @param editor     The editor to scroll
   * @param visualLine The visual line to place in the middle of the current window
   */
  public static void scrollVisualLineToMiddleOfScreen(@NotNull Editor editor, int visualLine, boolean allowVirtualSpace) {
    final int y = editor.visualLineToY(EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor), visualLine));
    final Rectangle visibleArea = getVisibleArea(editor);
    final int screenHeight = visibleArea.height;
    final int lineHeight = editor.getLineHeight();

    final int offset = y - ((screenHeight - lineHeight) / lineHeight / 2 * lineHeight);
    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    final int lastVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1) - 1;
    final int offsetForLastLineAtBottom = getOffsetToScrollVisualLineToBottomOfScreen(editor, lastVisualLine);

    // For `zz`, we want to use virtual space and move any line, including the last one, to the middle of the screen.
    // For `G` or `zb`, do not allow virtual space, so only scroll far enough to keep the last line at the bottom of the
    // screen
    if (!allowVirtualSpace && offset > offsetForLastLineAtBottom) {
      scrollVertically(editor, offsetForLastLineAtBottom);
    } else {
      scrollVertically(editor, offset);
    }
  }

  /**
   * Scrolls the editor to place the given visual line at the bottom of the screen.
   *
   * <p>When we're moving the caret down a few lines and want to scroll to keep this visible, we need to be able to
   * place a line at the bottom of the screen. Due to block inlays, we can't do this by specifying a top line to scroll
   * to.</p>
   *
   * @param editor                  The editor to scroll
   * @param nonNormalisedVisualLine The non-normalised visual line to place at the bottom of the current window. Might
   *                                be greater than visual line count to scroll to virtual space at the end of the file
   * @return True if the editor was scrolled
   */
  public static boolean scrollVisualLineToBottomOfScreen(@NotNull Editor editor, int nonNormalisedVisualLine) {
    final int offset = getOffsetToScrollVisualLineToBottomOfScreen(editor, nonNormalisedVisualLine);
    return scrollVertically(editor, offset);
  }

  private static int getOffsetToScrollVisualLineToBottomOfScreen(@NotNull Editor editor, int nonNormalisedVisualLine) {
    int exPanelHeight = 0;
    if (ExEntryPanel.getInstance().isActive()) {
      exPanelHeight = ExEntryPanel.getInstance().getHeight();
    }
    if (ExEntryPanel.getInstanceWithoutShortcuts().isActive()) {
      exPanelHeight += ExEntryPanel.getInstanceWithoutShortcuts().getHeight();
    }

    // Note that we explicitly do not normalise the visual line, as we might be trying to scroll a virtual line, at the
    // end of the file
    final int lineHeight = editor.getLineHeight();
    final int screenHeight = getVisibleArea(editor).height - exPanelHeight;
    final int inlayHeight = EditorUtil.getInlaysHeight(editor, nonNormalisedVisualLine, false);
    final int maxInlayHeight = BLOCK_INLAY_MAX_LINE_HEIGHT * lineHeight;
    final int y = editor.visualLineToY(nonNormalisedVisualLine) + lineHeight + min(inlayHeight, maxInlayHeight);
    return max(0, y - screenHeight);
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
    } else if (visualColumn > 0) {
      inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn - 1));
      if (inlay != null && !inlay.isRelatedToPrecedingText()) {
        targetVisualColumn = visualColumn - 1;
      }
    }

    final int columnLeftX = (int) Math.round(editor.visualPositionToPoint2D(new VisualPosition(visualLine, targetVisualColumn)).getX());
    scrollHorizontally(editor, columnLeftX);
  }

  public static void scrollColumnToMiddleOfScreen(@NotNull Editor editor, int visualLine, int visualColumn) {
    final Point2D point = editor.visualPositionToPoint2D(new VisualPosition(visualLine, visualColumn));
    final int screenWidth = getVisibleArea(editor).width;

    // Snap the column to the nearest standard column grid. This positions us nicely if there are an odd or even number
    // of columns. It also works with inline inlays and folds. It is slightly inaccurate for proportional fonts, but is
    // still a good solution. Besides, what kind of monster uses Vim with proportional fonts?
    final float standardColumnWidth = EditorHelper.getPlainSpaceWidthFloat(editor);
    final int screenMidColumn = (int) (screenWidth / standardColumnWidth / 2);
    final int x = max(0, (int) Math.round(point.getX() - (screenMidColumn * standardColumnWidth)));
    scrollHorizontally(editor, x);
  }

  public static void scrollColumnToRightOfScreen(@NotNull Editor editor, int visualLine, int visualColumn) {
    int targetVisualColumn = visualColumn;

    // Requested column might be an inlay (because we do simple arithmetic on visual position, and inlays and folds have
    // a visual position). If it is an inlay and is related to preceding text, we want to display it, so use it as the
    // target column. If it's an inlay related to following text, we don't want to display it at the right of the
    // screen, show the previous column
    Inlay<?> inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn));
    if (inlay != null && !inlay.isRelatedToPrecedingText()) {
      targetVisualColumn = visualColumn - 1;
    } else {
      // If the target column is followed by an inlay which is associated with it, make the inlay the target column so
      // it is visible
      inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(visualLine, visualColumn + 1));
      if (inlay != null && inlay.isRelatedToPrecedingText()) {
        targetVisualColumn = visualColumn + 1;
      }
    }

    // Scroll to the left edge of the target column, minus a screenwidth, and adjusted for inlays
    final int targetColumnRightX = (int) Math.round(editor.visualPositionToPoint2D(new VisualPosition(visualLine, targetVisualColumn + 1)).getX());
    final int screenWidth = getVisibleArea(editor).width;
    scrollHorizontally(editor, targetColumnRightX - screenWidth);
  }

  /**
   * Scroll page down, moving text up.
   *
   * @param editor The editor to scroll
   * @param pages  How many pages to scroll
   * @return A pair consisting of a flag to show if scrolling was completed, and a visual line to position the cart on
   */
  public static Pair<Boolean, Integer> scrollFullPageDown(final @NotNull Editor editor, int pages) {
    final Rectangle visibleArea = getVisibleArea(editor);
    @NotNull final VimEditor editor2 = new IjVimEditor(editor);
    final int lastVisualLine = EngineEditorHelperKt.getVisualLineCount(editor2) - 1;

    int y = visibleArea.y + visibleArea.height;
    int topBound = visibleArea.y;
    int bottomBound = visibleArea.y + visibleArea.height;
    int targetTopVisualLine = 0;
    int caretVisualLine = -1;
    boolean completed = true;

    for (int i = 0; i < pages; i++) {
      targetTopVisualLine = getFullVisualLine(editor, y, topBound, bottomBound);
      if (targetTopVisualLine >= lastVisualLine) {
        // If we're on the last page, end nicely on the last line, otherwise move the caret to the last line of the file
        if (i == pages - 1) {
          caretVisualLine = lastVisualLine;
        } else {
          @NotNull final VimEditor editor1 = new IjVimEditor(editor);
          caretVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1) - 1;
          completed = false;
        }
        targetTopVisualLine = lastVisualLine;
        break;
      }

      // The help page for 'scrolling' states that a page is the number of lines in the window minus two. Scrolling a
      // page adds this page length to the current targetTopVisualLine. Or in other words, scrolling down a page puts
      // the last but one targetTopVisualLine at the top of the next page.
      // E.g. a window showing lines 1-35 has a page size of 33, and scrolling down a page shows 34 as the top line
      targetTopVisualLine--;

      y = editor.visualLineToY(targetTopVisualLine);
      topBound = y;
      bottomBound = y + visibleArea.height;
      y = bottomBound;
      caretVisualLine = targetTopVisualLine;
    }

    scrollVisualLineToTopOfScreen(editor, targetTopVisualLine);
    return new Pair<>(completed, caretVisualLine);
  }

  /**
   * Scroll page up, moving text down.
   *
   * @param editor The editor to scroll
   * @param pages  How many pages to scroll
   * @return A pair consisting of a flag to show if scrolling was completed, and a visual line to position the cart on
   */
  public static Pair<Boolean, Integer> scrollFullPageUp(final @NotNull Editor editor, int pages) {
    final Rectangle visibleArea = getVisibleArea(editor);
    final int lineHeight = editor.getLineHeight();
    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    final int lastVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1) - 1;

    int y = visibleArea.y;
    int topBound = visibleArea.y;
    int bottomBound = visibleArea.y + visibleArea.height;
    int targetBottomVisualLine = 0;
    int caretVisualLine = -1;
    boolean completed = true;

    for (int i = 0; i < pages; i++) {
      // Scrolling up puts the current top line plus one at the bottom of the screen
      targetBottomVisualLine = getFullVisualLine(editor, y, topBound, bottomBound) + 1;
      if (targetBottomVisualLine == 1) {
        completed = i == pages - 1;
        break;
      } else if (targetBottomVisualLine == lastVisualLine) {
        // Vim normally scrolls up window height minus two. When there are only one or two lines in the screen, due to
        // end of file and virtual space, it scrolls window height minus one, or just plain windows height. IntelliJ
        // doesn't allow us only one line when virtual space is enabled, so we only need to handle the two line case.
        // Subtract the +1 we added above.
        targetBottomVisualLine--;
      }

      y = editor.visualLineToY(targetBottomVisualLine);
      bottomBound = y + lineHeight;
      topBound = bottomBound - visibleArea.height;
      y = topBound;
      caretVisualLine = targetBottomVisualLine;
    }

    scrollVisualLineToBottomOfScreen(editor, targetBottomVisualLine);
    return new Pair<>(completed, caretVisualLine);
  }

  private static int getFullVisualLine(final @NotNull Editor editor, int y, int topBound, int bottomBound) {
    // Note that we ignore inlays here. We're interested in the bounds of the text line. Scrolling will handle inlays as
    // it sees fit (e.g. scrolling a line to the bottom will make sure inlays below the line are visible).
    int line = editor.yToVisualLine(y);
    int yActual = editor.visualLineToY(line);
    if (yActual < topBound) {
      line++;
    } else if (yActual + editor.getLineHeight() > bottomBound) {
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

    // Make sure we get the character that contains this XY, not the editor's decision about the closest character. The
    // editor will give us the next character if X is over halfway through the character grid. Take into account that
    // the font size might be fractional, but the editor's area is integer. Use floating point values and round.
    long xActualLeft = Math.round(editor.visualPositionToPoint2D(closestVisualPosition).getX());
    if (xActualLeft > x) {
      closestVisualPosition = getPreviousNonInlayVisualPosition(editor, closestVisualPosition);
      xActualLeft = Math.round(editor.visualPositionToPoint2D(closestVisualPosition).getX());
    }

    if (xActualLeft >= leftBound) {
      final VisualPosition nextVisualPosition = new VisualPosition(closestVisualPosition.line, closestVisualPosition.column + 1);
      final long xActualRight = Math.round(editor.visualPositionToPoint2D(nextVisualPosition).getX()) - 1;
      if (xActualRight <= rightBound) {
        return closestVisualPosition.column;
      }

      return getPreviousNonInlayVisualPosition(editor, closestVisualPosition).column;
    } else {
      return getNextNonInlayVisualPosition(editor, closestVisualPosition).column;
    }
  }

  private static VisualPosition getNextNonInlayVisualPosition(@NotNull Editor editor, VisualPosition position) {
    final InlayModel inlayModel = editor.getInlayModel();
    final int lineLength = EngineEditorHelperKt.getVisualLineLength(new IjVimEditor(editor), position.line);
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
  public static @Nullable
  VirtualFile getVirtualFile(@NotNull Editor editor) {
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
