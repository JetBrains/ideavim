/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
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
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
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
import java.util.EnumSet;

import static com.maddyhome.idea.vim.group.ChangeGroup.*;
import static com.maddyhome.idea.vim.helper.EditorHelper.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This handles all motion related commands and marks
 */
public class MotionGroup extends VimMotionGroupBase {

  public @Nullable TextRange getMotionRange(@NotNull VimEditor editor,
                                            @NotNull VimCaret caret,
                                            ExecutionContext context,
                                            @NotNull Argument argument,
                                            @NotNull OperatorArguments operatorArguments) {
    return getMotionRange(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), ((IjExecutionContext) context).getContext(), argument, operatorArguments);
  }
  /**
   * This helper method calculates the complete range a motion will move over taking into account whether
   * the motion is FLAG_MOT_LINEWISE or FLAG_MOT_CHARACTERWISE (FLAG_MOT_INCLUSIVE or FLAG_MOT_EXCLUSIVE).
   *
   * @param editor   The editor the motion takes place in
   * @param caret    The caret the motion takes place on
   * @param context  The data context
   * @param argument Any argument needed by the motion
   * @param operatorArguments
   * @return The motion's range
   */
  public static @Nullable TextRange getMotionRange(@NotNull Editor editor,
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

        TextRange range = action.getRange(new IjVimEditor(editor), new IjVimCaret(caret), new IjExecutionContext(context), cnt, raw, cmd.getArgument());

        if (range == null) return null;

        start = range.getStartOffset();
        end = range.getEndOffset();

        if (cmd.isLinewiseMotion()) end--;
      }
      else {
        throw new RuntimeException(
          "Commands doesn't take " + cmd.getAction().getClass().getSimpleName() + " as an operator");
      }

      // Normalize the range
      if (start > end) {
        int t = start;
        start = end;
        end = t;
      }

      // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
      // of the line and move the end to the end of the line.
      if (cmd.isLinewiseMotion()) {
        if (caret.getLogicalPosition().line != getLineCount(editor) - 1) {
          start = getLineStartForOffset(editor, start);
          end = min(getLineEndForOffset(editor, end) + 1, EditorHelperRt.getFileSize(editor));
        }
        else {
          start = getLineStartForOffset(editor, start);
          end = getLineEndForOffset(editor, end);
        }
      }
    }

    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
    String text = editor.getDocument().getCharsSequence().subSequence(start, end).toString();
    final int lastNewLine = text.lastIndexOf('\n');
    if (lastNewLine > 0) {
      String id = argument.getMotion().getAction().getId();
      if (id.equals(VIM_MOTION_WORD_RIGHT) ||
          id.equals(VIM_MOTION_BIG_WORD_RIGHT) ||
          id.equals(VIM_MOTION_CAMEL_RIGHT)) {
        if (!SearchHelper.anyNonWhitespace(editor, end, -1)) {
          end = start + lastNewLine;
        }
      }
    }

    return new TextRange(start, end);
  }

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
        if (!SearchHelper.anyNonWhitespace(editor, end, -1)) {
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
    final int lastVisualLine = EditorHelper.getVisualLineCount(new IjVimEditor(editor)) - 1;

    final int newVisualLine;
    if (caretVisualLine < topVisualLine + scrollOffset) {
      newVisualLine = normalizeVisualLine(editor, topVisualLine + scrollOffset);
    }
    else if (bottomVisualLine < lastVisualLine && caretVisualLine > bottomVisualLine - scrollOffset) {
      newVisualLine = normalizeVisualLine(editor, bottomVisualLine - scrollOffset);
    }
    else {
      newVisualLine = caretVisualLine;
    }

    final int sideScrollOffset = getNormalizedSideScrollOffset(editor);

    final int oldColumn = editor.getCaretModel().getVisualPosition().column;
    int col = oldColumn;
    if (col >= getLineLength(editor) - 1) {
      col = UserDataManager.getVimLastColumn(editor.getCaretModel().getPrimaryCaret());
    }

    final int leftVisualColumn = getVisualColumnAtLeftOfScreen(editor, newVisualLine);
    final int rightVisualColumn = getVisualColumnAtRightOfScreen(editor, newVisualLine);
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

    newColumn = normalizeVisualColumn(editor, newVisualLine, newColumn, CommandStateHelper.isEndAllowed(editor));

    if (newVisualLine != caretVisualLine || newColumn != oldColumn) {
      int offset = visualPositionToOffset(editor, new VisualPosition(newVisualLine, newColumn));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);

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

    return EditorHelper.normalizeVisualLine(editor, targetCaretVisualLine);
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

  public void moveCaret(@NotNull VimEditor editor, @NotNull VimCaret caret, int offset) {
    moveCaret(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), offset);
  }

  public static void moveCaret(@NotNull Editor editor, @NotNull Caret caret, int offset) {
    if (offset < 0 || offset > editor.getDocument().getTextLength() || !caret.isValid()) return;

    if (CommandStateHelper.inBlockSubMode(editor)) {
      VisualGroupKt.vimMoveBlockSelectionToOffset(editor, offset);
      Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
      UserDataManager.setVimLastColumn(primaryCaret, primaryCaret.getVisualPosition().column);
      scrollCaretIntoView(editor);
      return;
    }

    // Make sure to always reposition the caret, even if the offset hasn't changed. We might need to reposition due to
    // changes in surrounding text, especially with inline inlays.
    final int oldOffset = caret.getOffset();
    InlayHelperKt.moveToInlayAwareOffset(caret, offset);
    if (oldOffset != offset) {
      UserDataManager.setVimLastColumn(caret, InlayHelperKt.getInlayAwareVisualColumn(caret));
    }

    // Similarly, always make sure the caret is positioned within the view. Adding or removing text could move the caret
    // position relative to the view, without changing offset.
    if (caret == editor.getCaretModel().getPrimaryCaret()) {
      scrollCaretIntoView(editor);
    }

    if (CommandStateHelper.inVisualMode(editor) || CommandStateHelper.inSelectMode(editor)) {
      VisualGroupKt.vimMoveSelectionToCaret(caret);
    }
    else {
      ModeHelper.exitVisualMode(editor);
    }

    AppCodeTemplates.onMovement(editor, caret, oldOffset < offset);
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
  public int moveCaretToMatchingPair(@NotNull VimEditor editor, @NotNull VimCaret caret) {
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

  /**
   * This moves the caret to the end of the next/previous word/WORD.
   *
   * @param editor  The editor to move in
   * @param caret   The caret to be moved
   * @param count   The number of words to skip
   * @param bigWord If true then find WORD, if false then find word
   * @return position
   */
  public Motion moveCaretToNextWordEnd(@NotNull Editor editor, @NotNull Caret caret, int count, boolean bigWord) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return Motion.Error.INSTANCE;
    }

    // If we are doing this move as part of a change command (e.q. cw), we need to count the current end of
    // word if the cursor happens to be on the end of a word already. If this is a normal move, we don't count
    // the current word.
    int pos = SearchHelper.findNextWordEnd(editor, caret, count, bigWord);
    if (pos == -1) {
      if (count < 0) {
        return new Motion.AbsoluteOffset(moveCaretToLineStart(new IjVimEditor(editor), 0));
      }
      else {
        return new Motion.AbsoluteOffset(moveCaretToLineEnd(new IjVimEditor(editor), getLineCount(editor) - 1, false));
      }
    }
    else {
      return new Motion.AbsoluteOffset(pos);
    }
  }

  public int moveCaretToNextSentenceStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = SearchHelper.findNextSentenceStart(editor, caret, count, false, true);
    if (res >= 0) {
      res = normalizeOffset(editor, res, true);
    }
    else {
      res = -1;
    }

    return res;
  }

  @Override
  public boolean scrollLineToFirstScreenLine(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.TOP, rawCount, start);
    return true;
  }

  @Override
  public boolean scrollLineToMiddleScreenLine(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.MIDDLE, rawCount, start);
    return true;
  }

  @Override
  public boolean scrollLineToLastScreenLine(@NotNull VimEditor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(((IjVimEditor)editor).getEditor(), ScreenLocation.BOTTOM, rawCount, start);
    return true;
  }

  @Override
  public boolean scrollCaretColumnToFirstScreenColumn(@NotNull VimEditor vimEditor) {
    Editor editor = ((IjVimEditor)vimEditor).getEditor();
    final VisualPosition caretVisualPosition = editor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(editor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column = max(0, caretVisualPosition.column - scrollOffset);
    scrollColumnToLeftOfScreen(editor, caretVisualPosition.line, column);
    return true;
  }

  @Override
  public boolean scrollCaretColumnToLastScreenColumn(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final VisualPosition caretVisualPosition = ijEditor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(ijEditor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column =
      normalizeVisualColumn(ijEditor, caretVisualPosition.line, caretVisualPosition.column + scrollOffset, false);
    scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, column);
    return true;
  }

  @Override
  public void scrollCaretIntoView(@NotNull VimEditor editor) {
    scrollCaretIntoView(((IjVimEditor) editor).getEditor());
  }

  public static void scrollCaretIntoView(@NotNull Editor editor) {
    final VisualPosition position = editor.getCaretModel().getVisualPosition();
    scrollCaretIntoViewVertically(editor, position.line);
    scrollCaretIntoViewHorizontally(editor, position);
  }

  // Vim's version of this method is move.c:update_topline, which will first scroll to fit the current line number at
  // the top of the window and then ensure that the current line fits at the bottom of the window
  private static void scrollCaretIntoViewVertically(@NotNull Editor editor, final int caretLine) {

    // TODO: Make this work with soft wraps
    // Vim's algorithm works by counting line heights for wrapped lines. We're using visual lines, which handles
    // collapsed folds, but treats soft wrapped lines as individual lines.
    // Ironically, after figuring out how Vim's algorithm works (although not *why*) and reimplementing, it looks likely
    // that this needs to be replaced as a more or less dumb line for line rewrite.

    final int topLine = getVisualLineAtTopOfScreen(editor);
    final int bottomLine = getVisualLineAtBottomOfScreen(editor);
    final int lastLine = EditorHelper.getVisualLineCount(new IjVimEditor(editor)) - 1;

    // We need the non-normalised value here, so we can handle cases such as so=999 to keep the current line centred
    final int scrollOffset = ((VimInt) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(new IjVimEditor(editor)), OptionConstants.scrolloffName, OptionConstants.scrolloffName)).getValue();
    final int topBound = topLine + scrollOffset;
    final int bottomBound = max(topBound, bottomLine - scrollOffset);

    // If we need to scroll the current line more than half a screen worth of lines then we just centre the new
    // current line. This mimics vim behavior of e.g. 100G in a 300 line file with a screen size of 25 centering line
    // 100. It also handles so=999 keeping the current line centred.
    // Note that block inlays means that the pixel height we are scrolling can be larger than half the screen, even if
    // the number of lines is less. I'm not sure what impact this has.
    final int height = getNonNormalizedVisualLineAtBottomOfScreen(editor) - topLine + 1;

    // Scrolljump isn't handled as you might expect. It is the minimal number of lines to scroll, but that doesn't mean
    // newLine = caretLine +/- MAX(sj, so)
    // <editor-fold desc="// Details">
    // When scrolling up (`k` - scrolling window up in the buffer; more lines are visible at the top of the window), Vim
    // will start at the new cursor line and repeatedly advance lines above and below. The new top line must be at least
    // scrolloff above caretLine. If this takes the new top line above the current top line, we must scroll at least
    // scrolljump. If the new caret line was already above the current top line, this counts as one scroll, and we
    // scroll from the caret line. Otherwise, we scroll from the current top line.
    // (See move.c:scroll_cursor_top)
    //
    // When scrolling down (`j` - scrolling window down in the buffer; more lines are visible at the bottom), Vim again
    // expands lines above and below the new bottom line, but calculates things a little differently. The total number
    // of lines expanded is at least scrolljump and there must be at least scrolloff lines below.
    // Since the lines are advancing simultaneously, it is only possible to get scrolljump/2 above the new cursor line.
    // If there are fewer than scrolljump/2 lines between the current bottom line and the new cursor line, the extra
    // lines are pushed below the new cursor line. Due to the algorithm advancing the "above" line before the "below"
    // line, we can end up with more than just scrolljump/2 lines on the top (hence the sj+1).
    // Therefore, the new top line is (cln + max(so, sj - min(cln-bl, ceiling((sj + 1)/2))))
    // (where cln is caretLine, bl is bottomLine, so is scrolloff and sj is scrolljump)
    // (See move.c:scroll_cursor_bot)
    //
    // On top of that, if the scroll distance is "too large", the new cursor line is positioned in the centre of the
    // screen. What "too large" means depends on scroll direction. There is an initial approximate check before working
    // out correct scroll locations
    // </editor-fold>
    final int scrollJump = getScrollJump(editor, height);

    // Unavoidable fudge value. Multiline rendered doc comments can mean we have very few actual lines, and scrolling
    // can get stuck in a loop as we re-centre the cursor instead of actually moving it. But if we ignore all inlays
    // and use the approximate screen height instead of the actual screen height (in lines), we make incorrect
    // assumptions about the top/bottom line numbers and can scroll to the wrong location. E.g. if there are enough doc
    // comments (String.java) it's possible to get 12 lines of actual code on screen. Given scrolloff=5, it's very easy
    // to hit problems, and have (scrolloffset > height / 2) and scroll to the middle of the screen. We'll use this
    // fudge value to make sure we're working with sensible values. Note that this problem doesn't affect code without
    // block inlays as positioning the cursor in the middle of the screen always positions it in a deterministic manner,
    // relative to other text in the file.
    final int inlayAwareMinHeightFudge = getApproximateScreenHeight(editor) / 2;

    // Note that while these calculations do the same thing that Vim does, it processes them differently. E.g. it
    // optionally checks and moves the top line, then optionally checks the bottom line. This gives us the same results
    // via the tests.
    if (height > inlayAwareMinHeightFudge && scrollOffset > height / 2) {
      scrollVisualLineToMiddleOfScreen(editor, caretLine, false);
    }
    else if (caretLine < topBound) {
      // Scrolling up, put the cursor at the top of the window (minus scrolloff)
      // Initial approximation in move.c:update_topline (including same calculation for halfHeight)
      if (topLine + scrollOffset - caretLine >= max(2, (height / 2) - 1)) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine, false);
      }
      else {
        // New top line must be at least scrolloff above caretLine. If this is above current top line, we must scroll
        // at least scrolljump. If caretLine was already above topLine, this counts as one scroll, and we scroll from
        // here. Otherwise, we scroll from topLine
        final int scrollJumpTopLine = max(0, (caretLine < topLine) ? caretLine - scrollJump + 1 : topLine - scrollJump);
        final int scrollOffsetTopLine = max(0, caretLine - scrollOffset);
        final int newTopLine = min(scrollOffsetTopLine, scrollJumpTopLine);

        // Used is set to the line height of caretLine (1 or how many lines soft wraps take up), and then incremented by
        // the line heights of the lines above and below caretLine (up to scrolloff or end of file).
        // Our implementation ignores soft wrap line heights. Folds already have a line height of 1.
        final int usedAbove = caretLine - newTopLine;
        final int usedBelow = min(scrollOffset, getVisualLineCount(new IjVimEditor(editor)) - caretLine);
        final int used = 1 + usedAbove + usedBelow;
        if (used > height) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine, false);
        }
        else {
          scrollVisualLineToTopOfScreen(editor, newTopLine);
        }
      }
    }
    else if (caretLine > bottomBound && bottomLine < lastLine) {
      // Scrolling down, put the cursor at the bottom of the window (minus scrolloff)
      // Do nothing if the bottom of the file is already above the bottom of the screen
      // Vim does a quick approximation before going through the full algorithm. It checks the line below the bottom
      // line in the window (bottomLine + 1). See move.c:update_topline
      int lineCount = caretLine - (bottomLine + 1) + 1 + scrollOffset;
      if (lineCount > height) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine, false);
      }
      else {
        // Vim expands out from caretLine at least scrolljump lines. It stops expanding above when it hits the
        // current bottom line, or (because it's expanding above and below) when it's scrolled scrolljump/2. It expands
        // above first, and the initial scroll count is 1, so we used (scrolljump+1)/2
        final int scrolledAbove = caretLine - bottomLine;
        final int extra = max(scrollOffset, scrollJump - min(scrolledAbove, Math.round((scrollJump + 1) / 2.0f)));
        final int scrolled = scrolledAbove + extra;

        // "used" is the count of lines expanded above and below. We expand below until we hit EOF (or when we've
        // expanded over a screen full) or until we've scrolled enough and we've expanded at least linesAbove
        // We expand above until usedAbove + usedBelow >= height. Or until we've scrolled enough (scrolled > sj and extra > so)
        // and we've expanded at least linesAbove (and at most, linesAbove - scrolled - scrolledAbove - 1)
        // The minus one is for the current line
        //noinspection UnnecessaryLocalVariable
        final int usedAbove = scrolledAbove;
        final int usedBelow = min(getVisualLineCount(new IjVimEditor(editor)) - caretLine, usedAbove - 1);
        final int used = min(height + 1, usedAbove + usedBelow);

        // If we've expanded more than a screen full, redraw with the cursor in the middle of the screen. If we're going
        // scroll more than a screen full or more than scrolloff, redraw with the cursor in the middle of the screen.
        lineCount = used > height ? used : scrolled;
        if (lineCount >= height && lineCount > scrollOffset) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine, false);
        }
        else {
          scrollVisualLineToBottomOfScreen(editor, caretLine + extra);
        }
      }
    }
  }

  private static int getScrollJump(@NotNull Editor editor, int height) {
    final EnumSet<CommandFlags> flags = VimStateMachine.getInstance(new IjVimEditor(editor)).getExecutingCommandFlags();
    final boolean scrollJump = !flags.contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP);

    // Default value is 1. Zero is a valid value, but we normalise to 1 - we always want to scroll at least one line
    // If the value is negative, it's a percentage of the height.
    if (scrollJump) {
      final int scrollJumpSize = ((VimInt) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(new IjVimEditor(editor)), OptionConstants.scrolljumpName, OptionConstants.scrolljumpName)).getValue();
      if (scrollJumpSize < 0) {
        return (int)(height * (min(100, -scrollJumpSize) / 100.0));
      }
      else {
        return max(1, scrollJumpSize);
      }
    }
    return 1;
  }

  private static void scrollCaretIntoViewHorizontally(@NotNull Editor editor, @NotNull VisualPosition position) {
    final int currentVisualLeftColumn = getVisualColumnAtLeftOfScreen(editor, position.line);
    final int currentVisualRightColumn = getVisualColumnAtRightOfScreen(editor, position.line);
    final int caretColumn = position.column;

    final int halfWidth = getApproximateScreenWidth(editor) / 2;
    final int scrollOffset = getNormalizedSideScrollOffset(editor);

    final EnumSet<CommandFlags> flags = VimStateMachine.getInstance(new IjVimEditor(editor)).getExecutingCommandFlags();
    final boolean allowSidescroll = !flags.contains(CommandFlags.FLAG_IGNORE_SIDE_SCROLL_JUMP);
    int sidescroll = ((VimInt) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(new IjVimEditor(editor)), OptionConstants.sidescrollName, OptionConstants.sidescrollName)).getValue();

    final int offsetLeft = caretColumn - (currentVisualLeftColumn + scrollOffset);
    final int offsetRight = caretColumn - (currentVisualRightColumn - scrollOffset);
    if (offsetLeft < 0 || offsetRight > 0) {
      int diff = offsetLeft < 0 ? -offsetLeft : offsetRight;

      if ((allowSidescroll && sidescroll == 0) || diff >= halfWidth || offsetRight >= offsetLeft) {
        scrollColumnToMiddleOfScreen(editor, position.line, caretColumn);
      }
      else {
        if (allowSidescroll && diff < sidescroll) {
          diff = sidescroll;
        }
        if (offsetLeft < 0) {
          scrollColumnToLeftOfScreen(editor, position.line, max(0, currentVisualLeftColumn - diff));
        }
        else {
          scrollColumnToRightOfScreen(editor, position.line,
                                      normalizeVisualColumn(editor, position.line, currentVisualRightColumn + diff,
                                                            false));
        }
      }
    }
  }

  @Override
  public int moveCaretToFirstScreenLine(@NotNull VimEditor editor,
                                        @NotNull VimCaret caret,
                                        int count,
                                        boolean normalizeToScreen) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.TOP, count - 1, normalizeToScreen);
  }

  @Override
  public int moveCaretToLastScreenLine(@NotNull VimEditor editor,
                                       @NotNull VimCaret caret,
                                       int count,
                                       boolean normalizeToScreen) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.BOTTOM, count - 1, normalizeToScreen);
  }

  @Override
  public int moveCaretToMiddleScreenLine(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    return moveCaretToScreenLocation(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(),
                                     ScreenLocation.MIDDLE, 0, false);
  }

  @Override
  public boolean scrollLine(@NotNull VimEditor editor, int lines) {
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

    final int line = mark.getLogicalLine();
    return toLineStart
           ? moveCaretToLineStartSkipLeading(editor, line)
           : editor.logicalPositionToOffset(new VimLogicalPosition(line, mark.getCol(), false));
  }

  @Override
  public int moveCaretToMark(@NotNull VimEditor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getMark(editor, ch);
    if (mark == null) return -1;

    final VirtualFile vf = getVirtualFile(((IjVimEditor)editor).getEditor());
    if (vf == null) return -1;

    final int line = mark.getLogicalLine();
    if (vf.getPath().equals(mark.getFilename())) {
      return toLineStart
             ? moveCaretToLineStartSkipLeading(editor, line)
             : editor.logicalPositionToOffset(new VimLogicalPosition(line, mark.getCol(), false));
    }

    final Editor selectedEditor = selectEditor(((IjVimEditor)editor).getEditor(), mark);
    if (selectedEditor != null) {
      for (Caret caret : selectedEditor.getCaretModel().getAllCarets()) {
        moveCaret(selectedEditor, caret, toLineStart
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

    final VimLogicalPosition lp = new VimLogicalPosition(jump.getLogicalLine(), jump.getCol(), false);
    final LogicalPosition lpnative = new LogicalPosition(jump.getLogicalLine(), jump.getCol(), false);
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
        moveCaret(newEditor, newEditor.getCaretModel().getCurrentCaret(),
                  normalizeOffset(newEditor, newEditor.logicalPositionToOffset(lpnative), false));
      }

      return -2;
    }
    else {
      if (spot == -1) {
        VimPlugin.getMark().addJump(editor, false);
      }

      return editor.logicalPositionToOffset(lp);
    }
  }

  @Override
  public @NotNull Motion moveCaretToMiddleColumn(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    final int width = getApproximateScreenWidth(((IjVimEditor)editor).getEditor()) / 2;
    final int len = getLineLength(((IjVimEditor)editor).getEditor());

    return moveCaretToColumn(editor, caret, max(0, min(len - 1, width)), false);
  }

  @Override
  public Motion moveCaretToColumn(@NotNull VimEditor editor, @NotNull VimCaret caret, int count, boolean allowEnd) {
    int line = caret.getLine().getLine();
    int pos = normalizeColumn(((IjVimEditor)editor).getEditor(), line, count, allowEnd);

    return new Motion.AbsoluteOffset(editor.logicalPositionToOffset(new VimLogicalPosition(line, pos, false)));
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineStartSkipLeading(@NotNull VimEditor editor,
                                                                                      int line) {
    return getLeadingCharacterOffset(((IjVimEditor)editor).getEditor(), line);
  }

  public int moveCaretToLineEnd(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    final VisualPosition visualPosition = ((IjVimCaret) caret).getCaret().getVisualPosition();
    final int lastVisualLineColumn = EditorUtil.getLastVisualLineColumnNumber(((IjVimEditor) editor).getEditor(), visualPosition.line);
    final VisualPosition visualEndOfLine = new VisualPosition(visualPosition.line, lastVisualLineColumn, true);
    return moveCaretToLineEnd(editor, ((IjVimEditor) editor).getEditor().visualToLogicalPosition(visualEndOfLine).line, true);
  }

  @Override
  public boolean scrollColumns(@NotNull VimEditor editor, int columns) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final VisualPosition caretVisualPosition = ijEditor.getCaretModel().getVisualPosition();
    if (columns > 0) {
      // TODO: Don't add columns to visual position. This includes inlays and folds
      int visualColumn = normalizeVisualColumn(ijEditor, caretVisualPosition.line,
                                               getVisualColumnAtLeftOfScreen(ijEditor, caretVisualPosition.line) +
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
      final int visualColumn = getVisualColumnAtRightOfScreen(ijEditor, caretVisualPosition.line) + columns;
      scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, visualColumn);
    }
    moveCaretToView(ijEditor);
    return true;
  }

  @Override
  public @NotNull Motion moveCaretToLineScreenStart(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    final int col =
      getVisualColumnAtLeftOfScreen(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
    return moveCaretToColumn(editor, caret, col, false);
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineScreenStartSkipLeading(@NotNull VimEditor editor,
                                                                                            @NotNull VimCaret caret) {
    final int col = getVisualColumnAtLeftOfScreen(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
    final int logicalLine = caret.getLine().getLine();
    return getLeadingCharacterOffset(((IjVimEditor)editor).getEditor(), logicalLine, col);
  }

  @Override
  public @NotNull Motion moveCaretToLineScreenEnd(@NotNull VimEditor editor,
                                                  @NotNull VimCaret caret,
                                                  boolean allowEnd) {
    final int col =
      getVisualColumnAtRightOfScreen(((IjVimEditor)editor).getEditor(), caret.getVisualPosition().getLine());
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
      caretVisualLine = normalizeVisualLine(ijEditor, caretVisualLine + scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line) {
      final int offset =
        moveCaretToLineWithStartOfLineOption(editor, visualLineToLogicalLine(ijEditor, caretVisualLine), caret);
      moveCaret(ijEditor, ijCaret, offset);
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
      caretVisualLine = normalizeVisualLine(ijEditor, caretVisualLine - scrollOffset);
    }

    if (caretVisualLine != ijCaret.getVisualPosition().line && caretVisualLine != -1) {
      final int offset =
        moveCaretToLineWithStartOfLineOption(editor, visualLineToLogicalLine(ijEditor, caretVisualLine), caret);
      moveCaret(ijEditor, ijCaret, offset);
      return result.getFirst();
    }

    // We normally error if we didn't move the caret, but we have a special case for a page showing only the last two
    // lines of the file and virtual space. Vim normally scrolls window height minus two, but when the caret is on last
    // line minus one, this becomes window height minus one, meaning the top line of the current page becomes the bottom
    // line of the new page, and the caret doesn't move. Make sure we don't beep in this scenario.
    return caretVisualLine == EditorHelper.getVisualLineCount(editor) - 2;
  }

  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineWithSameColumn(@NotNull VimEditor editor,
                                                                                    int logicalLine,
                                                                                    @NotNull VimCaret caret) {
    int col = UserDataManager.getVimLastColumn(((IjVimCaret) caret).getCaret());
    int line = logicalLine;
    if (logicalLine < 0) {
      line = 0;
      col = 0;
    }
    else if (logicalLine >= editor.lineCount()) {
      line = normalizeLine(((IjVimEditor) editor).getEditor(), editor.lineCount() - 1);
      col = getLineLength(((IjVimEditor) editor).getEditor(), line);
    }

    LogicalPosition newPos = new LogicalPosition(line, normalizeColumn(((IjVimEditor) editor).getEditor(), line, col, false));

    return ((IjVimEditor) editor).getEditor().logicalPositionToOffset(newPos);
  }

  @Override
  public @Range(from = 0, to = Integer.MAX_VALUE) int moveCaretToLineWithStartOfLineOption(@NotNull VimEditor editor,
                                                                                           int logicalLine,
                                                                                           @NotNull VimCaret caret) {
    if (VimPlugin.getOptionService().isSet(new OptionScope.LOCAL(editor), OptionConstants.startoflineName, OptionConstants.startoflineName)) {
      return moveCaretToLineStartSkipLeading(editor, logicalLine);
    }
    else {
      return moveCaretToLineWithSameColumn(editor, logicalLine, caret);
    }
  }

  @Override
  public boolean scrollScreen(final @NotNull VimEditor editor, final @NotNull VimCaret caret, int rawCount, boolean down) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    Caret ijCaret = ((IjVimCaret)caret).getCaret();
    final CaretModel caretModel = ijEditor.getCaretModel();
    final int currentLogicalLine = caretModel.getLogicalPosition().line;

    if ((!down && currentLogicalLine <= 0) || (down && currentLogicalLine >= getLineCount(ijEditor) - 1)) {
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

    int logicalLine = visualLineToLogicalLine(ijEditor, targetCaretVisualLine);
    int caretOffset = moveCaretToLineWithStartOfLineOption(editor, logicalLine, caret);
    moveCaret(ijEditor, ijCaret, caretOffset);

    return true;
  }

  // Scrolls current or [count] line to given screen location
  // In Vim, [count] refers to a file line, so it's a one-based logical line
  private void scrollLineToScreenLocation(@NotNull Editor editor,
                                          @NotNull ScreenLocation screenLocation,
                                          int rawCount,
                                          boolean start) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    int visualLine = rawCount == 0
                     ? editor.getCaretModel().getVisualPosition().line
                     : logicalLineToVisualLine(editor, normalizeLine(editor, rawCount - 1));

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
        scrollVisualLineToBottomOfScreen(editor, normalizeVisualLine(editor, visualLine + scrollOffset));
        break;
    }

    if (visualLine != editor.getCaretModel().getVisualPosition().line || start) {
      int offset;
      if (start) {
        offset = moveCaretToLineStartSkipLeading(new IjVimEditor(editor), visualLineToLogicalLine(editor, visualLine));
      }
      else {
        offset = getVerticalMotionOffset(new IjVimEditor(editor), new IjVimCaret(editor.getCaretModel().getPrimaryCaret()),
                                   visualLineToLogicalLine(editor, visualLine) -
                                   editor.getCaretModel().getLogicalPosition().line);
      }

      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
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
                                                                             @NotNull VimCaret caret,
                                                                             int count) {
    return moveCaretToLineWithStartOfLineOption(editor,
                                                normalizeLine(((IjVimEditor)editor).getEditor(),
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
        ModeHelper.exitVisualMode(editor);
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

    final int maxVisualLine = getVisualLineCount(new IjVimEditor(editor));

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

    final int targetLogicalLine = visualLineToLogicalLine(editor, targetVisualLine);
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
          MotionGroup.scrollCaretIntoView(editor);
        }
      }
    }

    @Override
    public void processLocalValueChange(@Nullable VimDataType oldValue, @NotNull VimEditor editor) {
      Editor ijEditor = ((IjVimEditor)editor).getEditor();

      if (UserDataManager.getVimEditorGroup(ijEditor)) {
        MotionGroup.scrollCaretIntoView(ijEditor);
      }
    }
  }
}
