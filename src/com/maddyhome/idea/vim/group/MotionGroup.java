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
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.Jump;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
import com.maddyhome.idea.vim.handler.MotionActionHandler;
import com.maddyhome.idea.vim.handler.TextObjectActionHandler;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.listener.IdeaSpecifics;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import kotlin.Pair;
import kotlin.ranges.IntProgression;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.EnumSet;

import static com.maddyhome.idea.vim.group.ChangeGroup.*;
import static com.maddyhome.idea.vim.helper.EditorHelper.*;

/**
 * This handles all motion related commands and marks
 */
public class MotionGroup {
  public static final int LAST_F = 1;
  public static final int LAST_f = 2;
  public static final int LAST_T = 3;
  public static final int LAST_t = 4;
  public static final int LAST_COLUMN = 9999;

  /**
   * This helper method calculates the complete range a motion will move over taking into account whether
   * the motion is FLAG_MOT_LINEWISE or FLAG_MOT_CHARACTERWISE (FLAG_MOT_INCLUSIVE or FLAG_MOT_EXCLUSIVE).
   *
   * @param editor     The editor the motion takes place in
   * @param caret      The caret the motion takes place on
   * @param context    The data context
   * @param count      The count applied to the motion
   * @param rawCount   The actual count entered by the user
   * @param argument   Any argument needed by the motion
   * @return The motion's range
   */
  public static @Nullable TextRange getMotionRange(@NotNull Editor editor,
                                         @NotNull Caret caret,
                                         DataContext context,
                                         int count,
                                         int rawCount,
                                         @NotNull Argument argument) {
    int start;
    int end;
    if (argument.getType() == Argument.Type.OFFSETS ) {
      final VimSelection offsets = argument.getOffsets().get(caret);
      if (offsets == null) return null;

      final Pair<Integer, Integer> nativeStartAndEnd = offsets.getNativeStartAndEnd();
      start = nativeStartAndEnd.getFirst();
      end = nativeStartAndEnd.getSecond();
    }
    else {
      final Command cmd = argument.getMotion();
      // Normalize the counts between the command and the motion argument
      int cnt = cmd.getCount() * count;
      int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
      if (cmd.getAction() instanceof MotionActionHandler) {
        MotionActionHandler action = (MotionActionHandler)cmd.getAction();

        // This is where we are now
        start = caret.getOffset();

        // Execute the motion (without moving the cursor) and get where we end
        end = action.getHandlerOffset(editor, caret, context, cnt, raw, cmd.getArgument());

        // Invalid motion
        if (end == -1) return null;

        // If inclusive, add the last character to the range
        if (action.getMotionType() == MotionType.INCLUSIVE && end < EditorHelperRt.getFileSize(editor)) {
          if (start > end) {
            start ++;
          }
          else {
            end++;
          }
        }
      }
      else if (cmd.getAction() instanceof TextObjectActionHandler) {
        TextObjectActionHandler action = (TextObjectActionHandler)cmd.getAction();

        TextRange range = action.getRange(editor, caret, context, cnt, raw, cmd.getArgument());

        if (range == null) return null;

        start = range.getStartOffset();
        end = range.getEndOffset();

        if (cmd.isLinewiseMotion()) end--;
      } else {
        throw new RuntimeException("Commands doesn't take " + cmd.getAction().getClass().getSimpleName() + " as an operator");
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
          end = Math.min(getLineEndForOffset(editor, end) + 1, EditorHelperRt.getFileSize(editor));
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

  private static void moveCaretToView(@NotNull Editor editor) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    final int topVisualLine = getVisualLineAtTopOfScreen(editor);
    final int bottomVisualLine = getVisualLineAtBottomOfScreen(editor);
    final int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
    final int newVisualLine;
    if (caretVisualLine < topVisualLine + scrollOffset) {
      newVisualLine = normalizeVisualLine(editor, topVisualLine + scrollOffset);
    }
    else if (caretVisualLine > bottomVisualLine - scrollOffset) {
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
    if (caretColumn < leftVisualColumn + sideScrollOffset) {
      newColumn = leftVisualColumn + sideScrollOffset;
    }
    else if (caretColumn > rightVisualColumn - sideScrollOffset) {
      newColumn = rightVisualColumn - sideScrollOffset;
    }

    if (newVisualLine == caretVisualLine && newColumn != caretColumn) {
      col = newColumn;
    }

    newColumn = normalizeVisualColumn(editor, newVisualLine, newColumn, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));

    if (newVisualLine != caretVisualLine || newColumn != oldColumn) {
      int offset = visualPositionToOffset(editor, new VisualPosition(newVisualLine, newColumn));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);

      UserDataManager.setVimLastColumn(editor.getCaretModel().getPrimaryCaret(), col);
    }
  }

  public @Nullable TextRange getBlockQuoteRange(@NotNull Editor editor, @NotNull Caret caret, char quote, boolean isOuter) {
    return SearchHelper.findBlockQuoteInLineRange(editor, caret, quote, isOuter);
  }

  public @Nullable TextRange getBlockRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter, char type) {
    return SearchHelper.findBlockRange(editor, caret, type, count, isOuter);
  }

  public @Nullable TextRange getBlockTagRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findBlockTagRange(editor, caret, count, isOuter);
  }

  public @NotNull TextRange getSentenceRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findSentenceRange(editor, caret, count, isOuter);
  }

  public @Nullable TextRange getParagraphRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findParagraphRange(editor, caret, count, isOuter);
  }

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

    return targetCaretVisualLine;
  }

  public int moveCaretToNthCharacter(@NotNull Editor editor, int count) {
    return Math.max(0, Math.min(count, EditorHelperRt.getFileSize(editor) - 1));
  }

  private static int getScrollOption(int rawCount) {
    NumberOption scroll = OptionsManager.INSTANCE.getScroll();
    if (rawCount == 0) {
      return scroll.value();
    }
    // TODO: This needs to be reset whenever the window size changes
    scroll.set(rawCount);
    return rawCount;
  }

  private static int getNormalizedScrollOffset(final @NotNull Editor editor) {
    final int scrollOffset = OptionsManager.INSTANCE.getScrolloff().value();
    return normalizeScrollOffset(editor, scrollOffset);
  }

  private static int getNormalizedSideScrollOffset(final @NotNull Editor editor) {
    final int sideScrollOffset = OptionsManager.INSTANCE.getSidescrolloff().value();
    return normalizeSideScrollOffset(editor, sideScrollOffset);
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

    // Always move the caret. It will be smart enough to not do anything if the offsets are the same, but it will also
    // ensure that it's in the correct location relative to any inline inlays
    final int oldOffset = caret.getOffset();
    InlayHelperKt.moveToInlayAwareOffset(caret, offset);
    if (oldOffset != offset) {
      UserDataManager.setVimLastColumn(caret, InlayHelperKt.getInlayAwareVisualColumn(caret));
      if (caret == editor.getCaretModel().getPrimaryCaret()) {
        scrollCaretIntoView(editor);
      }
    }

    if (CommandStateHelper.inVisualMode(editor) || CommandStateHelper.inSelectMode(editor)) {
      VisualGroupKt.vimMoveSelectionToCaret(caret);
    }
    else {
      ModeHelper.exitVisualMode(editor);
    }

    IdeaSpecifics.AppCodeTemplates.onMovement(editor, caret, oldOffset < offset);
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

  public int moveCaretToMatchingPair(@NotNull Editor editor, @NotNull Caret caret) {
    int pos = SearchHelper.findMatchingPairOnCurrentLine(editor, caret);
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
   * This moves the caret to the start of the next/previous word/WORD.
   *
   * @param editor  The editor to move in
   * @param count   The number of words to skip
   * @param bigWord If true then find WORD, if false then find word
   * @return position
   */
  public int findOffsetOfNextWord(@NotNull Editor editor, int searchFrom, int count, boolean bigWord) {
    final int size = EditorHelperRt.getFileSize(editor);
    if ((searchFrom == 0 && count < 0) || (searchFrom >= size - 1 && count > 0)) {
      return -1;
    }
    return SearchHelper.findNextWord(editor, searchFrom, count, bigWord);
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
  public int moveCaretToNextWordEnd(@NotNull Editor editor, @NotNull Caret caret, int count, boolean bigWord) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }

    // If we are doing this move as part of a change command (e.q. cw), we need to count the current end of
    // word if the cursor happens to be on the end of a word already. If this is a normal move, we don't count
    // the current word.
    int pos = SearchHelper.findNextWordEnd(editor, caret, count, bigWord);
    if (pos == -1) {
      if (count < 0) {
        return moveCaretToLineStart(editor, 0);
      }
      else {
        return moveCaretToLineEnd(editor, getLineCount(editor) - 1, false);
      }
    }
    else {
      return pos;
    }
  }

  /**
   * This moves the caret to the start of the next/previous paragraph.
   *
   * @param editor The editor to move in
   * @param caret  The caret to be moved
   * @param count  The number of paragraphs to skip
   * @return position
   */
  public int moveCaretToNextParagraph(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = SearchHelper.findNextParagraph(editor, caret, count, false);
    if (res >= 0) {
      res = normalizeOffset(editor, res, true);
    }
    else {
      res = -1;
    }

    return res;
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

  public int moveCaretToNextSentenceEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = SearchHelper.findNextSentenceEnd(editor, caret, count, false, true);
    if (res >= 0) {
      res = normalizeOffset(editor, res, false);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToUnmatchedBlock(@NotNull Editor editor, @NotNull Caret caret, int count, char type) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findUnmatchedBlock(editor, caret, type, count);
      if (res != -1) {
        res = normalizeOffset(editor, res, false);
      }

      return res;
    }
  }

  public int moveCaretToSection(@NotNull Editor editor, @NotNull Caret caret, char type, int dir, int count) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelperRt.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findSection(editor, caret, type, dir, count);
      if (res != -1) {
        res = normalizeOffset(editor, res, false);
      }

      return res;
    }
  }

  public int moveCaretToMethodStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return SearchHelper.findMethodStart(editor, caret, count);
  }

  public int moveCaretToMethodEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return SearchHelper.findMethodEnd(editor, caret, count);
  }

  public void setLastFTCmd(int lastFTCmd, char lastChar) {
    this.lastFTCmd = lastFTCmd;
    this.lastFTChar = lastChar;
  }

  public int repeatLastMatchChar(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = -1;
    int startPos = editor.getCaretModel().getOffset();
    switch (lastFTCmd) {
      case LAST_F:
        res = moveCaretToNextCharacterOnLine(editor, caret, -count, lastFTChar);
        break;
      case LAST_f:
        res = moveCaretToNextCharacterOnLine(editor, caret, count, lastFTChar);
        break;
      case LAST_T:
        res = moveCaretToBeforeNextCharacterOnLine(editor, caret, -count, lastFTChar);
        if (res == startPos && Math.abs(count) == 1) {
          res = moveCaretToBeforeNextCharacterOnLine(editor, caret, 2 * count, lastFTChar);
        }
        break;
      case LAST_t:
        res = moveCaretToBeforeNextCharacterOnLine(editor, caret, count, lastFTChar);
        if (res == startPos && Math.abs(count) == 1) {
          res = moveCaretToBeforeNextCharacterOnLine(editor, caret, 2 * count, lastFTChar);
        }
        break;
    }

    return res;
  }

  /**
   * This moves the caret to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  public int moveCaretToNextCharacterOnLine(@NotNull Editor editor, @NotNull Caret caret, int count, char ch) {
    int pos = SearchHelper.findNextCharacterOnLine(editor, caret, count, ch);

    if (pos >= 0) {
      return pos;
    }
    else {
      return -1;
    }
  }

  /**
   * This moves the caret next to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  public int moveCaretToBeforeNextCharacterOnLine(@NotNull Editor editor, @NotNull Caret caret, int count, char ch) {
    int pos = SearchHelper.findNextCharacterOnLine(editor, caret, count, ch);

    if (pos >= 0) {
      int step = count >= 0 ? 1 : -1;
      return pos - step;
    }
    else {
      return -1;
    }
  }

  public boolean scrollLineToFirstScreenLine(@NotNull Editor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(editor, ScreenLocation.TOP, rawCount, start);
    return true;
  }

  public boolean scrollLineToMiddleScreenLine(@NotNull Editor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(editor, ScreenLocation.MIDDLE, rawCount, start);
    return true;
  }

  public boolean scrollLineToLastScreenLine(@NotNull Editor editor, int rawCount, boolean start) {
    scrollLineToScreenLocation(editor, ScreenLocation.BOTTOM, rawCount, start);
    return true;
  }

  public boolean scrollCaretColumnToFirstScreenColumn(@NotNull Editor editor) {
    final VisualPosition caretVisualPosition = editor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(editor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column = Math.max(0, caretVisualPosition.column - scrollOffset);
    scrollColumnToLeftOfScreen(editor, caretVisualPosition.line, column);
    return true;
  }

  public boolean scrollCaretColumnToLastScreenColumn(@NotNull Editor editor) {
    final VisualPosition caretVisualPosition = editor.getCaretModel().getVisualPosition();
    final int scrollOffset = getNormalizedSideScrollOffset(editor);
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    final int column = normalizeVisualColumn(editor, caretVisualPosition.line, caretVisualPosition.column + scrollOffset, false);
    scrollColumnToRightOfScreen(editor, caretVisualPosition.line, column);
    return true;
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

    // We need the non-normalised value here, so we can handle cases such as so=999 to keep the current line centred
    final int scrollOffset = OptionsManager.INSTANCE.getScrolloff().value();
    final int topBound = topLine + scrollOffset;
    final int bottomBound = Math.max(topBound, bottomLine - scrollOffset);

    // If we need to scroll the current line more than half a screen worth of lines then we just centre the new
    // current line. This mimics vim behavior of e.g. 100G in a 300 line file with a screen size of 25 centering line
    // 100. It also handles so=999 keeping the current line centred.
    // Note that block inlays means that the pixel height we are scrolling can be larger than half the screen, even if
    // the number of lines is less. I'm not sure what impact this has.
    final int height = bottomLine - topLine + 1;

    // Scrolljump isn't handled as you might expect. It is the minimal number of lines to scroll, but that doesn't mean
    // newLine = caretLine +/- MAX(sj, so)
    //
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
      scrollVisualLineToMiddleOfScreen(editor, caretLine);
    } else if (caretLine < topBound) {
      // Scrolling up, put the cursor at the top of the window (minus scrolloff)
      // Initial approximation in move.c:update_topline (including same calculation for halfHeight)
      if (topLine + scrollOffset - caretLine >= Math.max(2, (height / 2) - 1)) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine);
      }
      else {
        // New top line must be at least scrolloff above caretLine. If this is above current top line, we must scroll
        // at least scrolljump. If caretLine was already above topLine, this counts as one scroll, and we scroll from
        // here. Otherwise, we scroll from topLine
        final int scrollJumpTopLine = Math.max(0, (caretLine < topLine) ? caretLine - scrollJump + 1 : topLine - scrollJump);
        final int scrollOffsetTopLine = Math.max(0, caretLine - scrollOffset);
        final int newTopLine = Math.min(scrollOffsetTopLine, scrollJumpTopLine);

        // Used is set to the line height of caretLine (1 or how many lines soft wraps take up), and then incremented by
        // the line heights of the lines above and below caretLine (up to scrolloff or end of file).
        // Our implementation ignores soft wrap line heights. Folds already have a line height of 1.
        final int usedAbove = caretLine - newTopLine;
        final int usedBelow = Math.min(scrollOffset, getVisualLineCount(editor) - caretLine);
        final int used = 1 + usedAbove + usedBelow;
        if (used > height) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine);
        }
        else {
          scrollVisualLineToTopOfScreen(editor, newTopLine);
        }
      }
    }
    else if (caretLine > bottomBound) {
      // Scrolling down, put the cursor at the bottom of the window (minus scrolloff)
      // Vim does a quick approximation before going through the full algorithm. It checks the line below the bottom
      // line in the window (bottomLine + 1). See move.c:update_topline
      int lineCount = caretLine - (bottomLine + 1) + 1 + scrollOffset;
      if (lineCount > height) {
        scrollVisualLineToMiddleOfScreen(editor, caretLine);
      } else {
        // Vim expands out from caretLine at least scrolljump lines. It stops expanding above when it hits the
        // current bottom line, or (because it's expanding above and below) when it's scrolled scrolljump/2. It expands
        // above first, and the initial scroll count is 1, so we used (scrolljump+1)/2
        final int scrolledAbove = caretLine - bottomLine;
        final int extra = Math.max(scrollOffset, scrollJump - Math.min(scrolledAbove, Math.round((scrollJump + 1) / 2.0f)));
        final int scrolled = scrolledAbove + extra;

        // "used" is the count of lines expanded above and below. We expand below until we hit EOF (or when we've
        // expanded over a screen full) or until we've scrolled enough and we've expanded at least linesAbove
        // We expand above until usedAbove + usedBelow >= height. Or until we've scrolled enough (scrolled > sj and extra > so)
        // and we've expanded at least linesAbove (and at most, linesAbove - scrolled - scrolledAbove - 1)
        // The minus one is for the current line
        //noinspection UnnecessaryLocalVariable
        final int usedAbove = scrolledAbove;
        final int usedBelow = Math.min(getVisualLineCount(editor) - caretLine, usedAbove - 1);
        final int used = Math.min(height + 1, usedAbove + usedBelow);

        // If we've expanded more than a screen full, redraw with the cursor in the middle of the screen. If we're going
        // scroll more than a screen full or more than scrolloff, redraw with the cursor in the middle of the screen.
        lineCount = used > height ? used : scrolled;
        if (lineCount >= height && lineCount > scrollOffset) {
          scrollVisualLineToMiddleOfScreen(editor, caretLine);
        }
        else {
          scrollVisualLineToBottomOfScreen(editor, caretLine + extra);
        }
      }
    }
  }

  private static int getScrollJump(@NotNull Editor editor, int height) {
    final EnumSet<CommandFlags> flags = CommandState.getInstance(editor).getExecutingCommandFlags();
    final boolean scrollJump = !flags.contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP);

    // Default value is 1. Zero is a valid value, but we normalise to 1 - we always want to scroll at least one line
    // If the value is negative, it's a percentage of the height.
    if (scrollJump) {
      final int scrollJumpSize = OptionsManager.INSTANCE.getScrolljump().value();
      if (scrollJumpSize < 0) {
        return (int) (height * (Math.min(100, -scrollJumpSize) / 100.0));
      }
      else {
        return Math.max(1, scrollJumpSize);
      }
    }
    return 1;
  }

  private static void scrollCaretIntoViewHorizontally(@NotNull Editor editor,
                                                      @NotNull VisualPosition position) {
    final int currentVisualLeftColumn = getVisualColumnAtLeftOfScreen(editor, position.line);
    final int currentVisualRightColumn = getVisualColumnAtRightOfScreen(editor, position.line);
    final int caretColumn = position.column;

    final int halfWidth = getApproximateScreenWidth(editor) / 2;
    final int scrollOffset = getNormalizedSideScrollOffset(editor);

    final EnumSet<CommandFlags> flags = CommandState.getInstance(editor).getExecutingCommandFlags();
    final boolean allowSidescroll = !flags.contains(CommandFlags.FLAG_IGNORE_SIDE_SCROLL_JUMP);
    int sidescroll = OptionsManager.INSTANCE.getSidescroll().value();

    final int offsetLeft = caretColumn - currentVisualLeftColumn - scrollOffset;
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
          scrollColumnToLeftOfScreen(editor, position.line, Math.max(0, currentVisualLeftColumn - diff));
        } else {
          scrollColumnToRightOfScreen(editor, position.line,
            normalizeVisualColumn(editor, position.line, currentVisualRightColumn + diff, false));
        }
      }
    }
  }

  public int moveCaretToFirstScreenLine(@NotNull Editor editor, int count) {
    return moveCaretToScreenLocation(editor, ScreenLocation.TOP, count);
  }

  public int moveCaretToLastScreenLine(@NotNull Editor editor, int count) {
    return moveCaretToScreenLocation(editor, ScreenLocation.BOTTOM, count);
  }

  public int moveCaretToMiddleScreenLine(@NotNull Editor editor) {
    return moveCaretToScreenLocation(editor, ScreenLocation.MIDDLE, 0);
  }

  public boolean scrollLine(@NotNull Editor editor, int lines) {
    assert lines != 0 : "lines cannot be 0";

    if (lines > 0) {
      final int visualLine = getVisualLineAtTopOfScreen(editor);
      scrollVisualLineToTopOfScreen(editor, visualLine + lines);
    }
    else {
      final int visualLine = getVisualLineAtBottomOfScreen(editor);
      scrollVisualLineToBottomOfScreen(editor, visualLine + lines);
    }

    moveCaretToView(editor);

    return true;
  }

  public @NotNull TextRange getWordRange(@NotNull Editor editor,
                                         @NotNull Caret caret,
                                         int count,
                                         boolean isOuter,
                                         boolean isBig) {
    int dir = 1;
    boolean selection = false;
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      if (UserDataManager.getVimSelectionStart(caret) > caret.getOffset()) {
        dir = -1;
      }
      if (UserDataManager.getVimSelectionStart(caret) != caret.getOffset()) {
        selection = true;
      }
    }

    return SearchHelper.findWordUnderCursor(editor, caret, count, dir, isOuter, isBig, selection);
  }

  public int moveCaretToFileMark(@NotNull Editor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getFileMark(editor, ch);
    if (mark == null) return -1;

    final int line = mark.getLogicalLine();
    return toLineStart
           ? moveCaretToLineStartSkipLeading(editor, line)
           : editor.logicalPositionToOffset(new LogicalPosition(line, mark.getCol()));
  }

  public int moveCaretToMark(@NotNull Editor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getMark(editor, ch);
    if (mark == null) return -1;

    final VirtualFile vf = getVirtualFile(editor);
    if (vf == null) return -1;

    final int line = mark.getLogicalLine();
    if (vf.getPath().equals(mark.getFilename())) {
      return toLineStart
             ? moveCaretToLineStartSkipLeading(editor, line)
             : editor.logicalPositionToOffset(new LogicalPosition(line, mark.getCol()));
    }

    final Editor selectedEditor = selectEditor(editor, mark);
    if (selectedEditor != null) {
      for (Caret caret : selectedEditor.getCaretModel().getAllCarets()) {
        moveCaret(selectedEditor, caret, toLineStart
                                         ? moveCaretToLineStartSkipLeading(selectedEditor, line)
                                         : selectedEditor
                                           .logicalPositionToOffset(new LogicalPosition(line, mark.getCol())));
      }
    }
    return -2;
  }

  public int moveCaretToJump(@NotNull Editor editor, int count) {
    final int spot = VimPlugin.getMark().getJumpSpot();
    final Jump jump = VimPlugin.getMark().getJump(count);

    if (jump == null) {
      return -1;
    }

    final VirtualFile vf = getVirtualFile(editor);
    if (vf == null) {
      return -1;
    }

    final LogicalPosition lp = new LogicalPosition(jump.getLogicalLine(), jump.getCol());
    final String fileName = jump.getFilepath();
    if (!vf.getPath().equals(fileName)) {
      final VirtualFile newFile =
        LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'));
      if (newFile == null) {
        return -2;
      }

      final Editor newEditor = selectEditor(editor, newFile);
      if (newEditor != null) {
        if (spot == -1) {
          VimPlugin.getMark().addJump(editor, false);
        }
        moveCaret(newEditor, newEditor.getCaretModel().getCurrentCaret(),
                  normalizeOffset(newEditor, newEditor.logicalPositionToOffset(lp), false));
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

  public int moveCaretToMiddleColumn(@NotNull Editor editor, @NotNull Caret caret) {
    final int width = getApproximateScreenWidth(editor) / 2;
    final int len = getLineLength(editor);

    return moveCaretToColumn(editor, caret, Math.max(0, Math.min(len - 1, width)), false);
  }

  public int moveCaretToColumn(@NotNull Editor editor, @NotNull Caret caret, int count, boolean allowEnd) {
    int line = caret.getLogicalPosition().line;
    int pos = normalizeColumn(editor, line, count, allowEnd);

    return editor.logicalPositionToOffset(new LogicalPosition(line, pos));
  }

  public int moveCaretToLineStartSkipLeading(@NotNull Editor editor, @NotNull Caret caret) {
    int logicalLine = caret.getLogicalPosition().line;
    return moveCaretToLineStartSkipLeading(editor, logicalLine);
  }

  public int moveCaretToLineStartSkipLeading(@NotNull Editor editor, int line) {
    return getLeadingCharacterOffset(editor, line);
  }

  public int moveCaretToLineStartSkipLeadingOffset(@NotNull Editor editor, @NotNull Caret caret, int linesOffset) {
    int line = normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset);
    return moveCaretToLineStartSkipLeading(editor, visualLineToLogicalLine(editor, line));
  }

  public int moveCaretToLineEnd(@NotNull Editor editor, @NotNull Caret caret) {
    final VisualPosition visualPosition = caret.getVisualPosition();
    final int lastVisualLineColumn = EditorUtil.getLastVisualLineColumnNumber(editor, visualPosition.line);
    final VisualPosition visualEndOfLine = new VisualPosition(visualPosition.line, lastVisualLineColumn, true);
    return moveCaretToLineEnd(editor, editor.visualToLogicalPosition(visualEndOfLine).line, true);
  }

  public boolean scrollColumns(@NotNull Editor editor, int columns) {
    final VisualPosition caretVisualPosition = editor.getCaretModel().getVisualPosition();
    if (columns > 0) {
      // TODO: Don't add columns to visual position. This includes inlays and folds
      int visualColumn = normalizeVisualColumn(editor, caretVisualPosition.line,
        getVisualColumnAtLeftOfScreen(editor, caretVisualPosition.line) + columns, false);

      // If the target column has an inlay preceding it, move passed it. This inlay will have been (incorrectly)
      // included in the simple visual position, so it's ok to step over. If we don't do this, scrollColumnToLeftOfScreen
      // can get stuck trying to make sure the inlay is visible.
      // A better solution is to not use VisualPosition everywhere, especially for arithmetic
      final Inlay<?> inlay = editor.getInlayModel().getInlineElementAt(new VisualPosition(caretVisualPosition.line, visualColumn - 1));
      if (inlay != null && !inlay.isRelatedToPrecedingText()) {
        visualColumn++;
      }

      scrollColumnToLeftOfScreen(editor, caretVisualPosition.line, visualColumn);
    }
    else {
      // Don't normalise the rightmost column, or we break virtual space
      final int visualColumn = getVisualColumnAtRightOfScreen(editor, caretVisualPosition.line) + columns;
      scrollColumnToRightOfScreen(editor, caretVisualPosition.line, visualColumn);
    }
    moveCaretToView(editor);
    return true;
  }

  public int moveCaretToLineStart(@NotNull Editor editor, @NotNull Caret caret) {
    int logicalLine = caret.getLogicalPosition().line;
    return moveCaretToLineStart(editor, logicalLine);
  }

  public int moveCaretToLineStart(@NotNull Editor editor, int line) {
    if (line >= getLineCount(editor)) {
      return EditorHelperRt.getFileSize(editor);
    }
    return getLineStartOffset(editor, line);
  }

  public int moveCaretToLineScreenStart(@NotNull Editor editor, @NotNull Caret caret) {
    final int col = getVisualColumnAtLeftOfScreen(editor, caret.getVisualPosition().line);
    return moveCaretToColumn(editor, caret, col, false);
  }

  public int moveCaretToLineScreenStartSkipLeading(@NotNull Editor editor, @NotNull Caret caret) {
    final int col = getVisualColumnAtLeftOfScreen(editor, caret.getVisualPosition().line);
    final int logicalLine = caret.getLogicalPosition().line;
    return getLeadingCharacterOffset(editor, logicalLine, col);
  }

  public int moveCaretToLineScreenEnd(@NotNull Editor editor, @NotNull Caret caret, boolean allowEnd) {
    final int col = getVisualColumnAtRightOfScreen(editor, caret.getVisualPosition().line);
    return moveCaretToColumn(editor, caret, col, allowEnd);
  }

  public int moveCaretHorizontalWrap(@NotNull Editor editor, @NotNull Caret caret, int count) {
    // FIX - allows cursor over newlines
    int oldOffset = caret.getOffset();
    int offset = Math.min(Math.max(0, caret.getOffset() + count), EditorHelperRt.getFileSize(editor));
    if (offset == oldOffset) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public int getOffsetOfHorizontalMotion(@NotNull Editor editor, @NotNull Caret caret, int count, boolean allowPastEnd) {
    int oldOffset = caret.getOffset();
    int diff = 0;
    CharSequence text = editor.getDocument().getCharsSequence();
    int sign = (int)Math.signum(count);
    for (int pointer : new IntProgression(0, count - sign, sign)) {
      int textPointer = oldOffset + pointer;
      if (textPointer < text.length() && textPointer >= 0) {
        // Actual char size can differ from 1 if unicode characters are used (like 🐔)
        diff += Character.charCount(Character.codePointAt(text, textPointer));
      }
      else {
        diff += 1;
      }
    }
    int offset =
      normalizeOffset(editor, caret.getLogicalPosition().line, oldOffset + (sign * diff), allowPastEnd);

    if (offset == oldOffset) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public boolean scrollFullPage(@NotNull Editor editor, int pages) {
    int caretVisualLine = EditorHelper.scrollFullPage(editor, pages);
    if (caretVisualLine != -1) {
      final int scrollOffset = getNormalizedScrollOffset(editor);
      boolean success = true;

      if (pages > 0) {
        // If the caret is ending up passed the end of the file, we need to beep
        if (caretVisualLine > getVisualLineCount(editor) - 1) {
          success = false;
        }

        int topVisualLine = getVisualLineAtTopOfScreen(editor);
        if (caretVisualLine < topVisualLine + scrollOffset) {
          caretVisualLine = normalizeVisualLine(editor, caretVisualLine + scrollOffset);
        }
      }
      else if (pages < 0) {
        int bottomVisualLine = getVisualLineAtBottomOfScreen(editor);
        if (caretVisualLine > bottomVisualLine - scrollOffset) {
          caretVisualLine = normalizeVisualLine(editor, caretVisualLine - scrollOffset);
        }
      }

      int offset =
        moveCaretToLineStartSkipLeading(editor, visualLineToLogicalLine(editor, caretVisualLine));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
      return success;
    }

    return false;
  }

  public int moveCaretToLine(@NotNull Editor editor, int logicalLine, @NotNull Caret caret) {
    int col = UserDataManager.getVimLastColumn(caret);
    int line = logicalLine;
    if (logicalLine < 0) {
      line = 0;
      col = 0;
    }
    else if (logicalLine >= getLineCount(editor)) {
      line = normalizeLine(editor, getLineCount(editor) - 1);
      col = getLineLength(editor, line);
    }

    LogicalPosition newPos = new LogicalPosition(line, normalizeColumn(editor, line, col, false));

    return editor.logicalPositionToOffset(newPos);
  }

  public boolean scrollScreen(final @NotNull Editor editor, int rawCount, boolean down) {
    final CaretModel caretModel = editor.getCaretModel();
    final int currentLogicalLine = caretModel.getLogicalPosition().line;

    if ((!down && currentLogicalLine <= 0) || (down && currentLogicalLine >= getLineCount(editor) - 1)) {
      return false;
    }

    final Rectangle visibleArea = getVisibleArea(editor);

    int targetCaretVisualLine = getScrollScreenTargetCaretVisualLine(editor, rawCount, down);

    // Scroll at most one screen height
    final int yInitialCaret = editor.visualLineToY(caretModel.getVisualPosition().line);
    final int yTargetVisualLine = editor.visualLineToY(targetCaretVisualLine);
    if (Math.abs(yTargetVisualLine - yInitialCaret) > visibleArea.height) {

      final int yPrevious = visibleArea.y;
      boolean moved;
      if (down) {
        targetCaretVisualLine = getVisualLineAtBottomOfScreen(editor) + 1;
        moved = scrollVisualLineToTopOfScreen(editor, targetCaretVisualLine);
      }
      else {
        targetCaretVisualLine = getVisualLineAtTopOfScreen(editor) - 1;
        moved = scrollVisualLineToBottomOfScreen(editor, targetCaretVisualLine);
      }
      if (moved) {
        // We'll keep the caret at the same position, although that might not be the same line offset as previously
        targetCaretVisualLine = editor.yToVisualLine(yInitialCaret + getVisibleArea(editor).y - yPrevious);
      }
    }
    else {

      scrollVisualLineToCaretLocation(editor, targetCaretVisualLine);

      final int scrollOffset = getNormalizedScrollOffset(editor);
      final int visualTop = getVisualLineAtTopOfScreen(editor) + scrollOffset;
      final int visualBottom = getVisualLineAtBottomOfScreen(editor) - scrollOffset;

      targetCaretVisualLine = Math.max(visualTop, Math.min(visualBottom, targetCaretVisualLine));
    }

    int logicalLine = visualLineToLogicalLine(editor, targetCaretVisualLine);
    int caretOffset = moveCaretToLineStartSkipLeading(editor, logicalLine);
    moveCaret(editor, caretModel.getPrimaryCaret(), caretOffset);

    return true;
  }

  public int moveCaretToLineEndSkipLeadingOffset(@NotNull Editor editor, @NotNull Caret caret, int linesOffset) {
    int line = visualLineToLogicalLine(editor, normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset));
    int start = getLineStartOffset(editor, line);
    int end = getLineEndOffset(editor, line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = start;
    for (int offset = end; offset > start; offset--) {
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

  public int moveCaretToLineEnd(@NotNull Editor editor, int line, boolean allowPastEnd) {
    return normalizeOffset(editor, line, getLineEndOffset(editor, line, allowPastEnd), allowPastEnd);
  }

  public int moveCaretGotoLineFirst(@NotNull Editor editor, int line) {
    return moveCaretToLineStartSkipLeading(editor, line);
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
        scrollVisualLineToMiddleOfScreen(editor, visualLine);
        break;
      case BOTTOM:
        scrollVisualLineToBottomOfScreen(editor, visualLine + scrollOffset);
        break;
    }

    if (visualLine != editor.getCaretModel().getVisualPosition().line || start) {
      int offset;
      if (start) {
        offset = moveCaretToLineStartSkipLeading(editor, visualLineToLogicalLine(editor, visualLine));
      }
      else {
        offset = moveCaretVertical(editor, editor.getCaretModel().getPrimaryCaret(),
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

  public int moveCaretGotoPreviousTab(@NotNull Editor editor, @NotNull DataContext context, int rawCount) {
    switchEditorTab(EditorWindow.DATA_KEY.getData(context), rawCount >= 1 ? -rawCount : -1, false);
    return editor.getCaretModel().getOffset();
  }

  public int moveCaretGotoNextTab(@NotNull Editor editor, @NotNull DataContext context, int rawCount) {
    final boolean absolute = rawCount >= 1;
    switchEditorTab(EditorWindow.DATA_KEY.getData(context), absolute ? rawCount - 1 : 1, absolute);
    return editor.getCaretModel().getOffset();
  }

  public int moveCaretVertical(@NotNull Editor editor, @NotNull Caret caret, int count) {
    VisualPosition pos = caret.getVisualPosition();
    if ((pos.line == 0 && count < 0) || (pos.line >= getVisualLineCount(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int col = UserDataManager.getVimLastColumn(caret);
      int line = normalizeVisualLine(editor, pos.line + count);

      if (col == LAST_COLUMN) {
        col = normalizeVisualColumn(editor, line, col, CommandStateHelper.isEndAllowedIgnoringOnemore(CommandStateHelper.getMode(editor)));
      }
      else {
        int newInlineElements = InlayHelperKt.amountOfInlaysBeforeVisualPosition(editor, new VisualPosition(line, col));

        col = normalizeVisualColumn(editor, line, col, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));
        col += newInlineElements;
      }

      VisualPosition newPos = new VisualPosition(line, col);
      return visualPositionToOffset(editor, newPos);
    }
  }

  public int moveCaretToLinePercent(@NotNull Editor editor, int count) {
    if (count > 100) count = 100;

    return moveCaretToLineStartSkipLeading(editor, normalizeLine(editor, (getLineCount(editor) * count + 99) / 100 - 1));
  }

  public int moveCaretGotoLineLast(@NotNull Editor editor, int rawCount) {
    final int line =
      rawCount == 0 ? normalizeLine(editor, getLineCount(editor) - 1) : rawCount - 1;

    return moveCaretToLineStartSkipLeading(editor, line);
  }

  public int moveCaretGotoLineLastEnd(@NotNull Editor editor, int rawCount, int line, boolean pastEnd) {
    return moveCaretToLineEnd(editor, rawCount == 0
                                      ? normalizeLine(editor, getLineCount(editor) - 1)
                                      : line, pastEnd);
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
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        ModeHelper.exitVisualMode(editor);
        KeyHandler.getInstance().reset(editor);
      }
    }
  }

  public int getLastFTCmd() {
    return lastFTCmd;
  }

  public char getLastFTChar() {
    return lastFTChar;
  }

  public int selectNextSearch(@NotNull Editor editor, int count, boolean forwards) {
    final Caret caret = editor.getCaretModel().getPrimaryCaret();
    final TextRange range = VimPlugin.getSearch().getNextSearchRange(editor, count, forwards);
    if (range == null) return -1;
    final int adj = VimPlugin.getVisualMotion().getSelectionAdj();
    if (!CommandStateHelper.inVisualMode(editor)) {
      final int startOffset = forwards ? range.getStartOffset() : Math.max(range.getEndOffset() - adj, 0);
      MotionGroup.moveCaret(editor, caret, startOffset);
      VimPlugin.getVisualMotion().enterVisualMode(editor, CommandState.SubMode.VISUAL_CHARACTER);
    }
    return forwards ? Math.max(range.getEndOffset() - adj, 0) : range.getStartOffset();
  }

  private int lastFTCmd = 0;
  private char lastFTChar;

  // [count] is a visual line offset, which means it's 1 based. The value is ignored for ScreenLocation.MIDDLE
  private int moveCaretToScreenLocation(@NotNull Editor editor,
                                        @NotNull ScreenLocation screenLocation,
                                        int visualLineOffset) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    int topVisualLine = getVisualLineAtTopOfScreen(editor);
    int bottomVisualLine = getVisualLineAtBottomOfScreen(editor);

    // Don't apply scrolloff if we're at the top or bottom of the file
    int offsetTopVisualLine = topVisualLine > 0 ? topVisualLine + scrollOffset : topVisualLine;
    int offsetBottomVisualLine =
      bottomVisualLine < getVisualLineCount(editor) ? bottomVisualLine - scrollOffset : bottomVisualLine;

    // [count]H/[count]L moves caret to that screen line, bounded by top/bottom scroll offsets
    int targetVisualLine = 0;
    switch (screenLocation) {
      case TOP:
        targetVisualLine = Math.max(offsetTopVisualLine, topVisualLine + visualLineOffset - 1);
        targetVisualLine = Math.min(targetVisualLine, offsetBottomVisualLine);
        break;
      case MIDDLE:
        targetVisualLine = getVisualLineAtMiddleOfScreen(editor);
        break;
      case BOTTOM:
        targetVisualLine = Math.min(offsetBottomVisualLine, bottomVisualLine - visualLineOffset + 1);
        targetVisualLine = Math.max(targetVisualLine, offsetTopVisualLine);
        break;
    }

    return moveCaretToLineStartSkipLeading(editor, visualLineToLogicalLine(editor, targetVisualLine));
  }

  public int moveCaretToLineEndOffset(@NotNull Editor editor,
                                      @NotNull Caret caret,
                                      int cntForward,
                                      boolean allowPastEnd) {
    int line = normalizeVisualLine(editor, caret.getVisualPosition().line + cntForward);

    if (line < 0) {
      return 0;
    }
    else {
      return moveCaretToLineEnd(editor, visualLineToLogicalLine(editor, line), allowPastEnd);
    }
  }

  public static class ScrollOptionsChangeListener implements OptionChangeListener<String> {
    public static ScrollOptionsChangeListener INSTANCE = new ScrollOptionsChangeListener();

    @Contract(pure = true)
    private ScrollOptionsChangeListener() {
    }

    @Override
    public void valueChange(String oldValue, String newValue) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        if (UserDataManager.getVimEditorGroup(editor)) {
          MotionGroup.scrollCaretIntoView(editor);
        }
      }
    }
  }
}
