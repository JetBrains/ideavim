/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.editor.*;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.HelperKt;
import com.maddyhome.idea.vim.helper.ScrollViewHelper;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.LocalOptionChangeListener;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.options.helpers.StrictMode;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt;
import kotlin.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

import static com.maddyhome.idea.vim.api.EngineEditorHelperKt.*;
import static com.maddyhome.idea.vim.helper.EditorHelper.*;
import static com.maddyhome.idea.vim.helper.ScrollHelperKt.getNormalizedScrollOffset;
import static com.maddyhome.idea.vim.helper.ScrollHelperKt.getNormalizedSideScrollOffset;
import static java.lang.Math.*;

public class ScrollGroup implements VimScrollGroup {
  @Override
  public void scrollCaretIntoView(@NotNull VimEditor editor) {
    ScrollViewHelper.scrollCaretIntoView(((IjVimEditor) editor).getEditor());
  }

  @Override
  public boolean scrollFullPage(@NotNull VimEditor editor, @NotNull VimCaret caret, int pages) {
    StrictMode.INSTANCE.assertTrue(pages != 0, "pages != 0");
    return pages > 0 ? scrollFullPageDown(editor, caret, pages) : scrollFullPageUp(editor, caret, abs(pages));
  }

  private boolean scrollFullPageDown(@NotNull VimEditor editor, @NotNull VimCaret caret, int pages) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final Pair<Boolean, Integer> result = EditorHelper.scrollFullPageDown(ijEditor, pages);

    final int scrollOffset = getNormalizedScrollOffset(ijEditor);
    final int topVisualLine = getVisualLineAtTopOfScreen(ijEditor);
    int caretVisualLine = result.getSecond();
    if (caretVisualLine < topVisualLine + scrollOffset) {
      caretVisualLine =
        normalizeVisualLine(new IjVimEditor(ijEditor), caretVisualLine + scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line) {
      final VimMotionGroup motion = VimInjectorKt.injector.getMotion();
      final int offset = motion.moveCaretToLineWithStartOfLineOption(editor, visualLineToBufferLine(editor, caretVisualLine), caret);
      caret.moveToOffset(offset);
      return result.getFirst();
    }

    return false;
  }

  private boolean scrollFullPageUp(@NotNull VimEditor editor, @NotNull VimCaret caret, int pages) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final Pair<Boolean, Integer> result = EditorHelper.scrollFullPageUp(ijEditor, pages);

    final int scrollOffset = getNormalizedScrollOffset(ijEditor);
    final int bottomVisualLine = getVisualLineAtBottomOfScreen(ijEditor);
    int caretVisualLine = result.getSecond();
    if (caretVisualLine > bottomVisualLine - scrollOffset) {
      caretVisualLine =
        normalizeVisualLine(new IjVimEditor(ijEditor), caretVisualLine - scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line && caretVisualLine != -1) {
      final VimMotionGroup motion = VimInjectorKt.injector.getMotion();
      final int offset = motion.moveCaretToLineWithStartOfLineOption(editor, visualLineToBufferLine(editor, caretVisualLine), caret);
      caret.moveToOffset(offset);
      return result.getFirst();
    }

    // We normally error if we didn't move the caret, but we have a special case for a page showing only the last two
    // lines of the file and virtual space. Vim normally scrolls window height minus two, but when the caret is on last
    // line minus one, this becomes window height minus one, meaning the top line of the current page becomes the bottom
    // line of the new page, and the caret doesn't move. Make sure we don't beep in this scenario.
    return caretVisualLine == getVisualLineCount(editor) - 2;
  }

  @Override
  public boolean scrollHalfPage(final @NotNull VimEditor editor, final @NotNull VimCaret caret, int rawCount, boolean down) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final CaretModel caretModel = ijEditor.getCaretModel();
    final int currentLogicalLine = caretModel.getLogicalPosition().line;

    if ((!down && currentLogicalLine <= 0) || (down && currentLogicalLine >= editor.lineCount() - 1)) {
      return false;
    }

    final Rectangle visibleArea = getVisibleArea(ijEditor);

    // We want to scroll the screen and keep the caret in the same screen-relative position. Calculate which line will
    // be at the current caret line and work the offsets out from that
    int targetCaretVisualLine = getScrollScreenTargetCaretVisualLine(ijEditor, rawCount, down);

    // Scroll at most one screen height
    final int yInitialCaret = ijEditor.visualLineToY(caretModel.getVisualPosition().line);
    final int yTargetVisualLine = ijEditor.visualLineToY(targetCaretVisualLine);
    if (abs(yTargetVisualLine - yInitialCaret) > visibleArea.height) {

      final int yPrevious = visibleArea.y;
      boolean moved;
      if (down) {
        targetCaretVisualLine = getVisualLineAtBottomOfScreen(ijEditor) + 1;
        moved = scrollVisualLineToTopOfScreen(ijEditor, targetCaretVisualLine);
      }
      else {
        targetCaretVisualLine = getVisualLineAtTopOfScreen(ijEditor) - 1;
        moved = scrollVisualLineToBottomOfScreen(ijEditor, targetCaretVisualLine);
      }
      if (moved) {
        // We'll keep the caret at the same position, although that might not be the same line offset as previously
        targetCaretVisualLine = ijEditor.yToVisualLine(yInitialCaret + getVisibleArea(ijEditor).y - yPrevious);
      }
    }
    else {
      scrollVisualLineToCaretLocation(ijEditor, targetCaretVisualLine);

      final int scrollOffset = getNormalizedScrollOffset(ijEditor);
      final int visualTop = getVisualLineAtTopOfScreen(ijEditor) + (down ? scrollOffset : 0);
      final int visualBottom = getVisualLineAtBottomOfScreen(ijEditor) - (down ? 0 : scrollOffset);

      targetCaretVisualLine = max(visualTop, min(visualBottom, targetCaretVisualLine));
    }

    int logicalLine = visualLineToBufferLine(editor, targetCaretVisualLine);
    int caretOffset = VimInjectorKt.injector.getMotion().moveCaretToLineWithStartOfLineOption(editor, logicalLine, caret);
    caret.moveToOffset(caretOffset);

    return true;
  }

  // Get the visual line that will be in the same screen relative location as the current caret line, after the screen
  // has been scrolled
  private static int getScrollScreenTargetCaretVisualLine(final @NotNull Editor editor, int rawCount, boolean down) {
    final Rectangle visibleArea = getVisibleArea(editor);
    final int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
    final int scrollOption = getScrollOption(rawCount);

    int targetCaretVisualLine;
    if (scrollOption == 0) {
      // Scroll up/down half window size by default. We can't use line count here because of block inlays
      final int offset = down ? (visibleArea.height / 2) : editor.getLineHeight() - (visibleArea.height / 2);
      targetCaretVisualLine = editor.yToVisualLine(editor.visualLineToY(caretVisualLine) + offset);
    }
    else {
      targetCaretVisualLine = down ? caretVisualLine + scrollOption : caretVisualLine - scrollOption;
    }

    return normalizeVisualLine(new IjVimEditor(editor), targetCaretVisualLine);
  }

  private static int getScrollOption(int rawCount) {
    if (rawCount == 0) {
      return ((VimInt) VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.scrollName, OptionConstants.scrollName)).getValue();
    }
    // TODO: This needs to be reset whenever the window size changes
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.scrollName, new VimInt(rawCount), OptionConstants.scrollName);
    return rawCount;
  }

  @Override
  public boolean scrollLines(@NotNull VimEditor editor, int lines) {
    assert lines != 0 : "lines cannot be 0";
    Editor ijEditor = ((IjVimEditor)editor).getEditor();

    if (lines > 0) {
      final int visualLine = getVisualLineAtTopOfScreen(ijEditor);
      scrollVisualLineToTopOfScreen(ijEditor, visualLine + lines);
    }
    else {
      final int visualLine = getNonNormalizedVisualLineAtBottomOfScreen(ijEditor);
      scrollVisualLineToBottomOfScreen(ijEditor, visualLine + lines);
    }

    MotionGroup.moveCaretToView(ijEditor);

    return true;
  }

  @Override
  public boolean scrollCurrentLineToDisplayTop(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.TOP, rawCount, start);
    return true;
  }

  @Override
  public boolean scrollCurrentLineToDisplayMiddle(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.MIDDLE, rawCount, start);
    return true;
  }

  @Override
  public boolean scrollCurrentLineToDisplayBottom(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.BOTTOM, rawCount, start);
    return true;
  }

  // Scrolls current or [count] line to given screen location
  // In Vim, [count] refers to a file line, so it's a one-based logical line
  private void scrollLineToScreenLocation(@NotNull Editor editor,
                                          @NotNull ScreenLocation screenLocation,
                                          int rawCount,
                                          boolean start) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    int visualLine;
    if (rawCount == 0) {
      visualLine = editor.getCaretModel().getVisualPosition().line;
    }
    else {
      final int line = normalizeLine(new IjVimEditor(editor), rawCount - 1);
      visualLine = new IjVimEditor(editor).bufferLineToVisualLine(line);
    }

    // This method moves the current (or [count]) line to the specified screen location
    // Scroll offset is applicable, but scroll jump isn't. Offset is applied to screen lines (visual lines)
    switch (screenLocation) {
      case TOP:
        scrollVisualLineToTopOfScreen(editor, visualLine - scrollOffset);
        break;
      case MIDDLE:
        scrollVisualLineToMiddleOfScreen(editor, visualLine, true);
        break;
      case BOTTOM:
        // Make sure we scroll to an actual line, not virtual space
        scrollVisualLineToBottomOfScreen(editor, normalizeVisualLine(new IjVimEditor(editor),
                                                                                          visualLine + scrollOffset));
        break;
    }

    if (visualLine != editor.getCaretModel().getVisualPosition().line || start) {
      final VimMotionGroup motion = VimInjectorKt.injector.getMotion();
      int offset;
      if (start) {
        offset = motion.moveCaretToLineStartSkipLeading(new IjVimEditor(editor),
                                                        visualLineToBufferLine(new IjVimEditor(editor), visualLine));
      }
      else {
        offset = motion.moveCaretToLineWithSameColumn(new IjVimEditor(editor),
                                                      visualLineToBufferLine(new IjVimEditor(editor), visualLine),
                                                      new IjVimCaret(editor.getCaretModel().getPrimaryCaret()));
      }

      new IjVimCaret(editor.getCaretModel().getPrimaryCaret()).moveToOffset(offset);
    }
  }

  @Override
  public boolean scrollColumns(@NotNull VimEditor editor, int columns) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final VisualPosition caretVisualPosition = ijEditor.getCaretModel().getVisualPosition();
    if (columns > 0) {
      // TODO: Don't add columns to visual position. This includes inlays and folds
      int visualColumn = normalizeVisualColumn(editor, caretVisualPosition.line,
                                                                    getVisualColumnAtLeftOfDisplay(ijEditor, caretVisualPosition.line) +
                                                                    columns, false);

      // If the target column has an inlay preceding it, move passed it. This inlay will have been (incorrectly)
      // included in the simple visual position, so it's ok to step over. If we don't do this, scrollColumnToLeftOfScreen
      // can get stuck trying to make sure the inlay is visible.
      // A better solution is to not use VisualPosition everywhere, especially for arithmetic
      final Inlay<?> inlay =
        ijEditor.getInlayModel().getInlineElementAt(new VisualPosition(caretVisualPosition.line, visualColumn - 1));
      if (inlay != null && !inlay.isRelatedToPrecedingText()) {
        visualColumn++;
      }

      scrollColumnToLeftOfScreen(ijEditor, caretVisualPosition.line, visualColumn);
    }
    else {
      // Don't normalise the rightmost column, or we break virtual space
      final int visualColumn = getVisualColumnAtRightOfDisplay(ijEditor, caretVisualPosition.line) + columns;
      scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, visualColumn);
    }
    MotionGroup.moveCaretToView(ijEditor);
    return true;
  }

  @Override
  public boolean scrollCaretColumnToDisplayLeftEdge(@NotNull VimEditor vimEditor) {
    Editor editor = ((IjVimEditor)vimEditor).getEditor();
    final VisualPosition caretVisualPosition = editor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(editor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column = max(0, caretVisualPosition.column - scrollOffset);
    scrollColumnToLeftOfScreen(editor, caretVisualPosition.line, column);
    return true;
  }

  @Override
  public boolean scrollCaretColumnToDisplayRightEdge(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final VisualPosition caretVisualPosition = ijEditor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(ijEditor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column =
      normalizeVisualColumn(editor, caretVisualPosition.line, caretVisualPosition.column + scrollOffset, false);
    scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, column);
    return true;
  }


  private enum ScreenLocation {
    TOP, MIDDLE, BOTTOM
  }

  public static class ScrollOptionsChangeListener implements LocalOptionChangeListener<VimDataType> {
    public static ScrollOptionsChangeListener INSTANCE = new ScrollOptionsChangeListener();

    @Contract(pure = true)
    private ScrollOptionsChangeListener() {
    }

    @Override
    public void processGlobalValueChange(@Nullable VimDataType oldValue) {
      for (Editor editor : HelperKt.localEditors()) {
        if (UserDataManager.getVimEditorGroup(editor)) {
          ScrollViewHelper.scrollCaretIntoView(editor);
        }
      }
    }

    @Override
    public void processLocalValueChange(@Nullable VimDataType oldValue, @NotNull VimEditor editor) {
      Editor ijEditor = ((IjVimEditor)editor).getEditor();

      if (UserDataManager.getVimEditorGroup(ijEditor)) {
        ScrollViewHelper.scrollCaretIntoView(ijEditor);
      }
    }
  }
}
