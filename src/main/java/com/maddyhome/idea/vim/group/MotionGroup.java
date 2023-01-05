/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.MathUtil;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.handler.Motion;
import com.maddyhome.idea.vim.handler.MotionActionHandler;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.listener.AppCodeTemplates;
import com.maddyhome.idea.vim.mark.Jump;
import com.maddyhome.idea.vim.mark.Mark;
import com.maddyhome.idea.vim.newapi.IjExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.LocalOptionChangeListener;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt;
import kotlin.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.io.File;

import static com.maddyhome.idea.vim.group.ChangeGroup.*;
import static com.maddyhome.idea.vim.helper.EditorHelper.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This handles all motion related commands and marks
 */
public class MotionGroup extends VimMotionGroupBase {


  public static @Nullable TextRange getMotionRange2(@NotNull Editor editor,
                                                    @NotNull Caret caret,
                                                    DataContext context,
                                                    @NotNull Argument argument,
                                                    @NotNull OperatorArguments operatorArguments) {
    int start;
    int end;
    if (argument.getType() == Argument.Type.OFFSETS) {
      final VimSelection offsets = argument.getOffsets().get(new IjVimCaret(caret));
      if (offsets == null) return null;

      final Pair<Integer, Integer> nativeStartAndEnd = offsets.getNativeStartAndEnd();
      start = nativeStartAndEnd.getFirst();
      end = nativeStartAndEnd.getSecond();
    }
    else {
      final Command cmd = argument.getMotion();
      // Normalize the counts between the command and the motion argument
      int cnt = cmd.getCount() * operatorArguments.getCount1();
      int raw = operatorArguments.getCount0() == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
      if (cmd.getAction() instanceof MotionActionHandler) {
        MotionActionHandler action = (MotionActionHandler)cmd.getAction();

        // This is where we are now
        start = caret.getOffset();

        // Execute the motion (without moving the cursor) and get where we end
        Motion motion =
          action.getHandlerOffset(new IjVimEditor(editor), new IjVimCaret(caret), new IjExecutionContext(context), cmd.getArgument(), operatorArguments.withCount0(raw));

        // Invalid motion
        if (Motion.Error.INSTANCE.equals(motion)) return null;
        if (Motion.NoMotion.INSTANCE.equals(motion)) return null;
        end = ((Motion.AbsoluteOffset)motion).getOffset();

        // If inclusive, add the last character to the range
        if (action.getMotionType() == MotionType.INCLUSIVE && end < EditorHelperRt.getFileSize(editor)) {
          if (start > end) {
            start++;
          }
          else {
            end++;
          }
        }
      }
      else if (cmd.getAction() instanceof TextObjectActionHandler) {
        TextObjectActionHandler action = (TextObjectActionHandler)cmd.getAction();

        TextRange range = action.getRange(
          new IjVimEditor(editor),
          new IjVimCaret(caret),
          new IjExecutionContext(context),
          cnt,
          raw,
          cmd.getArgument()
        );

        if (range == null) return null;

        start = range.getStartOffset();
        end = range.getEndOffset();

        if (cmd.isLinewiseMotion()) end--;
      }
      else {
        throw new RuntimeException(
          "Commands doesn't take " + cmd.getAction().getClass().getSimpleName() + " as an operator");
      }
    }

    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
    String id = argument.getMotion().getAction().getId();
    if (id.equals(VIM_MOTION_WORD_RIGHT) || id.equals(VIM_MOTION_BIG_WORD_RIGHT) || id.equals(VIM_MOTION_CAMEL_RIGHT)) {
      String text = editor.getDocument().getCharsSequence().subSequence(start, end).toString();
      final int lastNewLine = text.lastIndexOf('\n');
      if (lastNewLine > 0) {
        if (!EngineEditorHelperKt.anyNonWhitespace(new IjVimEditor(editor), end, -1)) {
          end = start + lastNewLine;
        }
      }
    }

    return new TextRange(start, end);
  }

  private static void moveCaretToView(@NotNull Editor editor) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    final int topVisualLine = getVisualLineAtTopOfScreen(editor);
    final int bottomVisualLine = getVisualLineAtBottomOfScreen(editor);
    final int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    final int lastVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1) - 1;

    final int newVisualLine;
    if (caretVisualLine < topVisualLine + scrollOffset) {
      newVisualLine = EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor), topVisualLine + scrollOffset);
    }
    else if (bottomVisualLine < lastVisualLine && caretVisualLine > bottomVisualLine - scrollOffset) {
      newVisualLine =
        EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor), bottomVisualLine - scrollOffset);
    }
    else {
      newVisualLine = caretVisualLine;
    }

    final int sideScrollOffset = getNormalizedSideScrollOffset(editor);

    final int oldColumn = editor.getCaretModel().getVisualPosition().column;
    int col = oldColumn;
    if (col >= EngineEditorHelperKt.lineLength(new IjVimEditor(editor), new IjVimEditor(editor).currentCaret().getBufferPosition().getLine()) - 1) {
      col = UserDataManager.getVimLastColumn(editor.getCaretModel().getPrimaryCaret());
    }

    final int leftVisualColumn = getVisualColumnAtLeftOfDisplay(editor, newVisualLine);
    final int rightVisualColumn = getVisualColumnAtRightOfDisplay(editor, newVisualLine);
    int caretColumn = col;
    int newColumn = caretColumn;

    // TODO: Visual column arithmetic will be inaccurate as it include columns for inlays and folds
    if (leftVisualColumn > 0 && caretColumn < leftVisualColumn + sideScrollOffset) {
      newColumn = leftVisualColumn + sideScrollOffset;
    }
    else if (caretColumn > rightVisualColumn - sideScrollOffset) {
      newColumn = rightVisualColumn - sideScrollOffset;
    }

    if (newVisualLine == caretVisualLine && newColumn != caretColumn) {
      col = newColumn;
    }

    newColumn = EngineEditorHelperKt.normalizeVisualColumn(new IjVimEditor(editor), newVisualLine, newColumn, CommandStateHelper.isEndAllowed(editor));

    if (newVisualLine != caretVisualLine || newColumn != oldColumn) {
      int offset = editor.visualPositionToOffset(new VisualPosition(newVisualLine, newColumn));
      new IjVimCaret(editor.getCaretModel().getPrimaryCaret()).moveToOffset(offset);

      UserDataManager.setVimLastColumn(editor.getCaretModel().getPrimaryCaret(), col);
    }
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

    return EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor), targetCaretVisualLine);
  }

  private static int getScrollOption(int rawCount) {
    if (rawCount == 0) {
      return ((VimInt) VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.scrollName, OptionConstants.scrollName)).getValue();
    }
    // TODO: This needs to be reset whenever the window size changes
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL.INSTANCE, OptionConstants.scrollName, new VimInt(rawCount), OptionConstants.scrollName);
    return rawCount;
  }

  private static int getNormalizedScrollOffset(final @NotNull Editor editor) {
    final int scrollOffset = ((VimInt) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(new IjVimEditor(editor)), OptionConstants.scrolloffName, OptionConstants.scrolloffName)).getValue();
    return normalizeScrollOffset(editor, scrollOffset);
  }

  private static int getNormalizedSideScrollOffset(final @NotNull Editor editor) {
    final int sideScrollOffset = ((VimInt) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(new IjVimEditor(editor)), OptionConstants.sidescrolloffName, OptionConstants.sidescrolloffName)).getValue();
    return normalizeSideScrollOffset(editor, sideScrollOffset);
  }

  @Override
  public void onAppCodeMovement(@NotNull VimEditor editor, @NotNull VimCaret caret, int offset, int oldOffset) {
    AppCodeTemplates.onMovement(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(), oldOffset < offset);
  }

  private @Nullable Editor selectEditor(@NotNull Editor editor, @NotNull Mark mark) {
    final VirtualFile virtualFile = markToVirtualFile(mark);
    if (virtualFile != null) {
      return selectEditor(editor, virtualFile);
    }
    else {
      return null;
    }
  }

  private @Nullable VirtualFile markToVirtualFile(@NotNull Mark mark) {
    String protocol = mark.getProtocol();
    VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
    return fileSystem.findFileByPath(mark.getFilename());
  }

  private @Nullable Editor selectEditor(@NotNull Editor editor, @NotNull VirtualFile file) {
    return VimPlugin.getFile().selectEditor(editor.getProject(), file);
  }

  @Override
  public int moveCaretToMatchingPair(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret) {
    int pos = SearchHelper.findMatchingPairOnCurrentLine(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret());
    if (pos >= 0) {
      return pos;
    }
    else {
      return -1;
    }
  }

  /**
   * This moves the caret to the start of the next/previous camel word.
   *
   * @param editor The editor to move in
   * @param caret  The caret to be moved
   * @param count  The number of words to skip
   * @return position
   */
  public int moveCaretToNextCamel(@NotNull Editor editor, @NotNull Caret caret, int count) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      return SearchHelper.findNextCamelStart(editor, caret, count);
    }
  }

  /**
   * This moves the caret to the start of the next/previous camel word.
   *
   * @param editor The editor to move in
   * @param caret  The caret to be moved
   * @param count  The number of words to skip
   * @return position
   */
  public int moveCaretToNextCamelEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      return SearchHelper.findNextCamelEnd(editor, caret, count);
    }
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
      EngineEditorHelperKt.normalizeVisualColumn(editor, caretVisualPosition.line, caretVisualPosition.column + scrollOffset, false);
    scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, column);
    return true;
  }

  @Override
  public void scrollCaretIntoView(@NotNull VimEditor editor) {
    ScrollViewHelper.scrollCaretIntoView(((IjVimEditor) editor).getEditor());
  }

  @Override
  public int moveCaretToFirstDisplayLine(@NotNull VimEditor editor,
                                         @NotNull ImmutableVimCaret caret,
                                         int count,
                                         boolean normalizeToScreen) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.TOP, count - 1, normalizeToScreen);
  }

  @Override
  public int moveCaretToLastDisplayLine(@NotNull VimEditor editor,
                                        @NotNull ImmutableVimCaret caret,
                                        int count,
                                        boolean normalizeToScreen) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.BOTTOM, count - 1, normalizeToScreen);
  }

  @Override
  public int moveCaretToMiddleDisplayLine(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.MIDDLE, 0, false);
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

    moveCaretToView(ijEditor);

    return true;
  }

  @Override
  public int moveCaretToFileMark(@NotNull VimEditor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getFileMark(editor, ch);
    if (mark == null) return -1;

    final int line = mark.getLine();
    return toLineStart
           ? moveCaretToLineStartSkipLeading(editor, line)
           : editor.bufferPositionToOffset(new BufferPosition(line, mark.getCol(), false));
  }

  @Override
  public int moveCaretToMark(@NotNull VimEditor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getMark(editor, ch);
    if (mark == null) return -1;

    final VirtualFile vf = getVirtualFile(((IjVimEditor)editor).getEditor());
    if (vf == null) return -1;

    final int line = mark.getLine();
    if (vf.getPath().equals(mark.getFilename())) {
      return toLineStart
             ? moveCaretToLineStartSkipLeading(editor, line)
             : editor.bufferPositionToOffset(new BufferPosition(line, mark.getCol(), false));
    }

    final Editor selectedEditor = selectEditor(((IjVimEditor)editor).getEditor(), mark);
    if (selectedEditor != null) {
      for (Caret caret : selectedEditor.getCaretModel().getAllCarets()) {
        new IjVimCaret(caret).moveToOffset(toLineStart
                                           ? moveCaretToLineStartSkipLeading(new IjVimEditor(selectedEditor), line)
                                           : selectedEditor.logicalPositionToOffset(
                                             new LogicalPosition(line, mark.getCol())));
      }
    }
    return -2;
  }

  @Override
  public int moveCaretToJump(@NotNull VimEditor editor, int count) {
    final int spot = VimPlugin.getMark().getJumpSpot();
    final Jump jump = VimPlugin.getMark().getJump(count);

    if (jump == null) {
      return -1;
    }

    final VirtualFile vf = getVirtualFile(((IjVimEditor)editor).getEditor());
    if (vf == null) {
      return -1;
    }

    final BufferPosition lp = new BufferPosition(jump.getLine(), jump.getCol(), false);
    final LogicalPosition lpnative = new LogicalPosition(jump.getLine(), jump.getCol(), false);
    final String fileName = jump.getFilepath();
    if (!vf.getPath().equals(fileName)) {
      final VirtualFile newFile =
        LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'));
      if (newFile == null) {
        return -2;
      }

      final Editor newEditor = selectEditor(((IjVimEditor)editor).getEditor(), newFile);
      if (newEditor != null) {
        if (spot == -1) {
          VimPlugin.getMark().addJump(editor, false);
        }
        new IjVimCaret(newEditor.getCaretModel().getCurrentCaret()).moveToOffset(
          EngineEditorHelperKt.normalizeOffset(new IjVimEditor(newEditor), newEditor.logicalPositionToOffset(lpnative),
                                               false));
      }

      return -2;
    }
    else {
      if (spot == -1) {
        VimPlugin.getMark().addJump(editor, false);
      }

      return editor.bufferPositionToOffset(lp);
    }
  }

  @Override
  public @NotNull Motion moveCaretToCurrentDisplayLineMiddle(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret) {
    final int width = getApproximateScreenWidth(((IjVimEditor)editor).getEditor()) / 2;
    final int len = EngineEditorHelperKt.lineLength(editor, editor.currentCaret().getBufferPosition().getLine());

    return moveCaretToColumn(editor, caret, max(0, min(len - 1, width)), false);
  }

  @Override
  public Motion moveCaretToColumn(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret, int count, boolean allowEnd) {
    final int line = caret.getLine().getLine();
    final int column = EngineEditorHelperKt.normalizeColumn(editor, line, count, allowEnd);
    final int offset = editor.bufferPositionToOffset(new BufferPosition(line, column, false));
    if (column != count) {
      return new Motion.AdjustedOffset(offset, count);
    }
    return new Motion.AbsoluteOffset(offset);
  }


  @Override
  public boolean scrollColumns(@NotNull VimEditor editor, int columns) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final VisualPosition caretVisualPosition = ijEditor.getCaretModel().getVisualPosition();
    if (columns > 0) {
      // TODO: Don't add columns to visual position. This includes inlays and folds
      int visualColumn = EngineEditorHelperKt.normalizeVisualColumn(editor, caretVisualPosition.line,
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
    moveCaretToView(ijEditor);
    return true;
  }

  @Override
  public @NotNull Motion moveCaretToCurrentDisplayLineStart(@NotNull VimEditor editor, ImmutableVimCaret caret) {
    final int col =
      getVisualColumnAtLeftOfDisplay(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
    return moveCaretToColumn(editor, caret, col, false);
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToCurrentDisplayLineStartSkipLeading(@NotNull VimEditor editor,
                                                                                                    ImmutableVimCaret caret) {
    final int col = getVisualColumnAtLeftOfDisplay(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
    final int logicalLine = caret.getLine().getLine();
    return EngineEditorHelperKt.getLeadingCharacterOffset(editor, logicalLine, col);
  }

  @Override
  public @NotNull Motion moveCaretToCurrentDisplayLineEnd(@NotNull VimEditor editor,
                                                          ImmutableVimCaret caret,
                                                          boolean allowEnd) {
    final int col =
      getVisualColumnAtRightOfDisplay(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
    return moveCaretToColumn(editor, caret, col, allowEnd);
  }

  @Override
  public boolean scrollFullPageDown(@NotNull VimEditor editor, @NotNull VimCaret caret, int pages) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final Pair<Boolean, Integer> result = EditorHelper.scrollFullPageDown(ijEditor, pages);

    final int scrollOffset = getNormalizedScrollOffset(ijEditor);
    final int topVisualLine = getVisualLineAtTopOfScreen(ijEditor);
    int caretVisualLine = result.getSecond();
    if (caretVisualLine < topVisualLine + scrollOffset) {
      caretVisualLine =
        EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(ijEditor), caretVisualLine + scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line) {
      final int offset =
        moveCaretToLineWithStartOfLineOption(editor, EngineEditorHelperKt.visualLineToBufferLine(editor, caretVisualLine), caret);
      caret.moveToOffset(offset);
      return result.getFirst();
    }

    return false;
  }

  @Override
  public boolean scrollFullPageUp(@NotNull VimEditor editor, @NotNull VimCaret caret, int pages) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final Pair<Boolean, Integer> result = EditorHelper.scrollFullPageUp(ijEditor, pages);

    final int scrollOffset = getNormalizedScrollOffset(ijEditor);
    final int bottomVisualLine = getVisualLineAtBottomOfScreen(ijEditor);
    int caretVisualLine = result.getSecond();
    if (caretVisualLine > bottomVisualLine - scrollOffset) {
      caretVisualLine =
        EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(ijEditor), caretVisualLine - scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line && caretVisualLine != -1) {
      final int offset =
        moveCaretToLineWithStartOfLineOption(editor, EngineEditorHelperKt.visualLineToBufferLine(editor, caretVisualLine), caret);
      caret.moveToOffset(offset);
      return result.getFirst();
    }

    // We normally error if we didn't move the caret, but we have a special case for a page showing only the last two
    // lines of the file and virtual space. Vim normally scrolls window height minus two, but when the caret is on last
    // line minus one, this becomes window height minus one, meaning the top line of the current page becomes the bottom
    // line of the new page, and the caret doesn't move. Make sure we don't beep in this scenario.
    return caretVisualLine == EngineEditorHelperKt.getVisualLineCount(editor) - 2;
  }

  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineWithSameColumn(@NotNull VimEditor editor,
                                                                                    int logicalLine,
                                                                                    @NotNull ImmutableVimCaret caret) {
    int col = UserDataManager.getVimLastColumn(((IjVimCaret) caret).getCaret());
    int line = logicalLine;
    if (logicalLine < 0) {
      line = 0;
      col = 0;
    }
    else if (logicalLine >= editor.lineCount()) {
      line = EngineEditorHelperKt.normalizeLine(editor, editor.lineCount() - 1);
      col = EngineEditorHelperKt.lineLength(editor, line);
    }

    LogicalPosition newPos = new LogicalPosition(line, EngineEditorHelperKt.normalizeColumn(editor, line, col, false));

    return ((IjVimEditor) editor).getEditor().logicalPositionToOffset(newPos);
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineWithStartOfLineOption(@NotNull VimEditor editor,
                                                                                           int line,
                                                                                           @NotNull ImmutableVimCaret caret) {
    if (VimPlugin.getOptionService().isSet(new OptionScope.LOCAL(editor), OptionConstants.startoflineName, OptionConstants.startoflineName)) {
      return moveCaretToLineStartSkipLeading(editor, line);
    }
    else {
      return moveCaretToLineWithSameColumn(editor, line, caret);
    }
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
    if (Math.abs(yTargetVisualLine - yInitialCaret) > visibleArea.height) {

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

    int logicalLine = EngineEditorHelperKt.visualLineToBufferLine(editor, targetCaretVisualLine);
    int caretOffset = moveCaretToLineWithStartOfLineOption(editor, logicalLine, caret);
    caret.moveToOffset(caretOffset);

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
      final int line = EngineEditorHelperKt.normalizeLine(new IjVimEditor(editor), rawCount - 1);
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
        scrollVisualLineToBottomOfScreen(editor, EngineEditorHelperKt.normalizeVisualLine(new IjVimEditor(editor),
                                                                                          visualLine + scrollOffset));
        break;
    }

    if (visualLine != editor.getCaretModel().getVisualPosition().line || start) {
      int offset;
      if (start) {
        offset = moveCaretToLineStartSkipLeading(new IjVimEditor(editor), EngineEditorHelperKt.visualLineToBufferLine(
          new IjVimEditor(editor), visualLine));
      }
      else {
        offset = moveCaretToLineWithSameColumn(new IjVimEditor(editor),
                                               EngineEditorHelperKt.visualLineToBufferLine(new IjVimEditor(editor), visualLine),
                                               new IjVimCaret(editor.getCaretModel().getPrimaryCaret()));
      }

      new IjVimCaret(editor.getCaretModel().getPrimaryCaret()).moveToOffset(offset);
    }
  }

  /**
   * If 'absolute' is true, then set tab index to 'value', otherwise add 'value' to tab index with wraparound.
   */
  private void switchEditorTab(@Nullable EditorWindow editorWindow, int value, boolean absolute) {
    if (editorWindow != null) {
      final EditorTabbedContainer tabbedPane = editorWindow.getTabbedPane();
      if (absolute) {
        tabbedPane.setSelectedIndex(value);
      }
      else {
        int tabIndex = (value + tabbedPane.getSelectedIndex()) % tabbedPane.getTabCount();
        tabbedPane.setSelectedIndex(tabIndex < 0 ? tabIndex + tabbedPane.getTabCount() : tabIndex);
      }
    }
  }

  @Override
  public int moveCaretGotoPreviousTab(@NotNull VimEditor editor, @NotNull ExecutionContext context, int rawCount) {
    Project project = ((IjVimEditor)editor).getEditor().getProject();
    if (project == null) {
      return editor.currentCaret().getOffset().getPoint();
    }
    EditorWindow currentWindow = FileEditorManagerEx.getInstanceEx(project).getSplitters().getCurrentWindow();
    switchEditorTab(currentWindow, rawCount >= 1 ? -rawCount : -1, false);
    return editor.currentCaret().getOffset().getPoint();
  }

  @Override
  public int moveCaretGotoNextTab(@NotNull VimEditor editor, @NotNull ExecutionContext context, int rawCount) {
    final boolean absolute = rawCount >= 1;

    Project project = ((IjVimEditor)editor).getEditor().getProject();
    if (project == null) {
      return editor.currentCaret().getOffset().getPoint();
    }
    EditorWindow currentWindow = FileEditorManagerEx.getInstanceEx(project).getSplitters().getCurrentWindow();
    switchEditorTab(currentWindow, absolute ? rawCount - 1 : 1, absolute);
    return editor.currentCaret().getOffset().getPoint();
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLinePercent(@NotNull VimEditor editor,
                                                                             @NotNull ImmutableVimCaret caret,
                                                                             int count) {
    return moveCaretToLineWithStartOfLineOption(editor,
                                                EngineEditorHelperKt.normalizeLine(editor,
                                                              (editor.lineCount() * MathUtil.clamp(count, 0, 100) +
                                                               99) / 100 - 1), caret);
  }

  private enum ScreenLocation {
    TOP, MIDDLE, BOTTOM
  }

  public static void fileEditorManagerSelectionChangedCallback(@NotNull FileEditorManagerEvent event) {
    ExEntryPanel.deactivateAll();
    final FileEditor fileEditor = event.getOldEditor();
    if (fileEditor instanceof TextEditor) {
      final Editor editor = ((TextEditor)fileEditor).getEditor();
      ExOutputModel.getInstance(editor).clear();
      if (VimStateMachine.getInstance(new IjVimEditor(editor)).getMode() == VimStateMachine.Mode.VISUAL) {
        EngineModeExtensionsKt.exitVisualMode(new IjVimEditor(editor));
        KeyHandler.getInstance().reset(new IjVimEditor(editor));
      }
    }
  }

  // visualLineOffset is a zero based offset to subtract from the direction of travel, where zero is the same as a count
  // of 1. I.e. 1L = L, which is an offset of zero. 2L is an offset of 1 extra line
  // When normalizeToScreen is true, the offset is bounded to the current screen dimensions, and scrolloff is applied.
  // When false, the offset is used directly, and scrolloff is not applied. This is used for op pending motions
  // (scrolloff is applied after)
  private @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToScreenLocation(@NotNull Editor editor,
                                                                                 @NotNull Caret caret,
                                                                                 @NotNull ScreenLocation screenLocation,
                                                                                 int visualLineOffset,
                                                                                 boolean normalizeToScreen) {

    final int scrollOffset = normalizeToScreen ? getNormalizedScrollOffset(editor) : 0;

    @NotNull final VimEditor editor1 = new IjVimEditor(editor);
    final int maxVisualLine = EngineEditorHelperKt.getVisualLineCount(editor1);

    final int topVisualLine = getVisualLineAtTopOfScreen(editor);
    final int topScrollOff = topVisualLine > 0 ? scrollOffset : 0;

    final int bottomVisualLine = getVisualLineAtBottomOfScreen(editor);
    final int bottomScrollOff = bottomVisualLine < (maxVisualLine - 1) ? scrollOffset : 0;

    final int topMaxVisualLine = normalizeToScreen ? bottomVisualLine - bottomScrollOff : maxVisualLine;
    final int bottomMinVisualLine = normalizeToScreen ? topVisualLine + topScrollOff : 0;

    int targetVisualLine = 0;
    switch (screenLocation) {
      case TOP:
        targetVisualLine = min(topVisualLine + max(topScrollOff, visualLineOffset), topMaxVisualLine);
        break;
      case MIDDLE:
        targetVisualLine = getVisualLineAtMiddleOfScreen(editor);
        break;
      case BOTTOM:
        targetVisualLine = max(bottomVisualLine - max(bottomScrollOff, visualLineOffset), bottomMinVisualLine);
        break;
    }

    final int targetLogicalLine = EngineEditorHelperKt.visualLineToBufferLine(new IjVimEditor(editor), targetVisualLine);
    return moveCaretToLineWithStartOfLineOption(new IjVimEditor(editor), targetLogicalLine, new IjVimCaret(caret));
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
