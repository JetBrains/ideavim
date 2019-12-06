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
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
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
import com.maddyhome.idea.vim.listener.VimListenerManager;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import kotlin.Pair;
import kotlin.ranges.IntProgression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.EnumSet;

import static com.maddyhome.idea.vim.group.ChangeGroup.*;

/**
 * This handles all motion related commands and marks
 */
public class MotionGroup {
  public static final int LAST_F = 1;
  public static final int LAST_f = 2;
  public static final int LAST_T = 3;
  public static final int LAST_t = 4;
  public static final int LAST_COLUMN = 9999;

  public void editorCreated(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    // This ridiculous code ensures that a lot of events are processed BEFORE we finally start listening
    // to visible area changes. The primary reason for this change is to fix the cursor position bug
    // using the gd and gD commands (Goto Declaration). This bug has been around since Idea 6.0.4?
    // Prior to this change the visible area code was moving the cursor around during file load and messing
    // with the cursor position of the Goto Declaration processing.
    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication()
      .invokeLater(() -> ApplicationManager.getApplication().invokeLater(() -> {
        VimListenerManager.EditorListeners.add(editor);
        UserDataManager.setVimMotionGroup(editor, true);
      })));
  }

  public void editorReleased(@NotNull EditorFactoryEvent event) {
    Editor editor = event.getEditor();
    if (UserDataManager.getVimMotionGroup(editor)) {
      VimListenerManager.EditorListeners.remove(editor);
      UserDataManager.setVimMotionGroup(editor, false);
    }
  }

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
  @Nullable
  public static TextRange getMotionRange(@NotNull Editor editor,
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
        if (action.getMotionType() == MotionType.INCLUSIVE &&
            !cmd.getFlags().contains(CommandFlags.FLAG_MOT_LINEWISE)) {
          end++;
        }
      }
      else if (cmd.getAction() instanceof TextObjectActionHandler) {
        TextObjectActionHandler action = (TextObjectActionHandler)cmd.getAction();

        TextRange range = action.getRange(editor, caret, context, cnt, raw, cmd.getArgument());

        if (range == null) return null;

        start = range.getStartOffset();
        end = range.getEndOffset();

        if (cmd.getFlags().contains(CommandFlags.FLAG_MOT_LINEWISE)) end--;
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
      EnumSet<CommandFlags> flags = cmd.getFlags();
      if (flags.contains(CommandFlags.FLAG_MOT_LINEWISE)) {
        start = EditorHelper.getLineStartForOffset(editor, start);
        end = Math.min(EditorHelper.getLineEndForOffset(editor, end) + 1, EditorHelper.getFileSize(editor, true));
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

    int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
    int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
    int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
    int newline = caretVisualLine;
    if (caretVisualLine < topVisualLine + scrollOffset) {
      newline = EditorHelper.normalizeVisualLine(editor, topVisualLine + scrollOffset);
    }
    else if (caretVisualLine >= bottomVisualLine - scrollOffset) {
      newline = EditorHelper.normalizeVisualLine(editor, bottomVisualLine - scrollOffset);
    }

    int sideScrollOffset = OptionsManager.INSTANCE.getSidescrolloff().value();
    int width = EditorHelper.getScreenWidth(editor);
    if (sideScrollOffset > width / 2) {
      sideScrollOffset = width / 2;
    }

    int col = editor.getCaretModel().getVisualPosition().column;
    int oldColumn = col;
    if (col >= EditorHelper.getLineLength(editor) - 1) {
      col = UserDataManager.getVimLastColumn(editor.getCaretModel().getPrimaryCaret());
    }
    int visualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    int caretColumn = col;
    int newColumn = caretColumn;
    if (caretColumn < visualColumn + sideScrollOffset) {
      newColumn = visualColumn + sideScrollOffset;
    }
    else if (caretColumn >= visualColumn + width - sideScrollOffset) {
      newColumn = visualColumn + width - sideScrollOffset - 1;
    }

    if (newline == caretVisualLine && newColumn != caretColumn) {
      col = newColumn;
    }

    newColumn = EditorHelper.normalizeVisualColumn(editor, newline, newColumn, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));

    if (newline != caretVisualLine || newColumn != oldColumn) {
      int offset = EditorHelper.visualPositionToOffset(editor, new VisualPosition(newline, newColumn));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);

      UserDataManager.setVimLastColumn(editor.getCaretModel().getPrimaryCaret(), col);
    }
  }

  @Nullable
  public TextRange getBlockQuoteRange(@NotNull Editor editor, @NotNull Caret caret, char quote, boolean isOuter) {
    return SearchHelper.findBlockQuoteInLineRange(editor, caret, quote, isOuter);
  }

  @Nullable
  public TextRange getBlockRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter, char type) {
    return SearchHelper.findBlockRange(editor, caret, type, count, isOuter);
  }

  @Nullable
  public TextRange getBlockTagRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findBlockTagRange(editor, caret, count, isOuter);
  }

  @NotNull
  public TextRange getSentenceRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findSentenceRange(editor, caret, count, isOuter);
  }

  @Nullable
  public TextRange getParagraphRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter) {
    return SearchHelper.findParagraphRange(editor, caret, count, isOuter);
  }

  private static int getScrollScreenTargetCaretVisualLine(@NotNull final Editor editor, int rawCount, boolean down) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
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
    return Math.max(0, Math.min(count, EditorHelper.getFileSize(editor) - 1));
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

  private static int getNormalizedScrollOffset(@NotNull final Editor editor) {
    int scrollOffset = OptionsManager.INSTANCE.getScrolloff().value();
    return EditorHelper.normalizeScrollOffset(editor, scrollOffset);
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

    if (caret.getOffset() != offset) {
      caret.moveToOffset(offset);
      UserDataManager.setVimLastColumn(caret, caret.getVisualPosition().column);
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
  }

  @Nullable
  private Editor selectEditor(@NotNull Editor editor, @NotNull Mark mark) {
    final VirtualFile virtualFile = markToVirtualFile(mark);
    if (virtualFile != null) {
      return selectEditor(editor, virtualFile);
    }
    else {
      return null;
    }
  }

  @Nullable
  private VirtualFile markToVirtualFile(@NotNull Mark mark) {
    String protocol = mark.getProtocol();
    VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
    return fileSystem.findFileByPath(mark.getFilename());
  }

  @Nullable
  private Editor selectEditor(@NotNull Editor editor, @NotNull VirtualFile file) {
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
        (caret.getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
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
        (caret.getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
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
    final int size = EditorHelper.getFileSize(editor);
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
        (caret.getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
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
        return moveCaretToLineEnd(editor, EditorHelper.getLineCount(editor) - 1, false);
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
      res = EditorHelper.normalizeOffset(editor, res, true);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToNextSentenceStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = SearchHelper.findNextSentenceStart(editor, caret, count, false, true);
    if (res >= 0) {
      res = EditorHelper.normalizeOffset(editor, res, true);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToNextSentenceEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int res = SearchHelper.findNextSentenceEnd(editor, caret, count, false, true);
    if (res >= 0) {
      res = EditorHelper.normalizeOffset(editor, res, false);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToUnmatchedBlock(@NotNull Editor editor, @NotNull Caret caret, int count, char type) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findUnmatchedBlock(editor, caret, type, count);
      if (res != -1) {
        res = EditorHelper.normalizeOffset(editor, res, false);
      }

      return res;
    }
  }

  public int moveCaretToSection(@NotNull Editor editor, @NotNull Caret caret, char type, int dir, int count) {
    if ((caret.getOffset() == 0 && count < 0) ||
        (caret.getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findSection(editor, caret, type, dir, count);
      if (res != -1) {
        res = EditorHelper.normalizeOffset(editor, res, false);
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

  public boolean scrollColumnToFirstScreenColumn(@NotNull Editor editor) {
    scrollColumnToScreenColumn(editor, 0);

    return true;
  }

  public boolean scrollColumnToLastScreenColumn(@NotNull Editor editor) {
    scrollColumnToScreenColumn(editor, EditorHelper.getScreenWidth(editor));

    return true;
  }

  public static void scrollCaretIntoView(@NotNull Editor editor) {
    final boolean scrollJump =
      !CommandState.getInstance(editor).getFlags().contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP);
    scrollPositionIntoView(editor, editor.getCaretModel().getVisualPosition(), scrollJump);
  }

  public static void scrollPositionIntoView(@NotNull Editor editor,
                                            @NotNull VisualPosition position,
                                            boolean scrollJump) {
    final int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
    final int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
    final int visualLine = position.line;
    final int column = position.column;

    // We need the non-normalised value here, so we can handle cases such as so=999 to keep the current line centred
    int scrollOffset = OptionsManager.INSTANCE.getScrolloff().value();

    int scrollJumpSize = 0;
    if (scrollJump) {
      scrollJumpSize = Math.max(0, OptionsManager.INSTANCE.getScrolljump().value() - 1);
    }

    int visualTop = topVisualLine + scrollOffset;
    int visualBottom = bottomVisualLine - scrollOffset + 1;
    if (visualTop == visualBottom) {
      visualBottom++;
    }

    int diff;
    if (visualLine < visualTop) {
      diff = visualLine - visualTop;
      scrollJumpSize = -scrollJumpSize;
    }
    else {
      diff = Math.max(0, visualLine - visualBottom + 1);
    }

    if (diff != 0) {

      // If we need to scroll the current line more than half a screen worth of lines then we just centre the new
      // current line. This mimics vim behavior of e.g. 100G in a 300 line file with a screen size of 25 centering line
      // 100. It also handles so=999 keeping the current line centred.
      // It doesn't handle keeping the line centred when scroll offset is less than a full page height, as the new line
      // might be within e.g. top + scroll offset, so we test for that separately.
      // Note that block inlays means that the pixel height we are scrolling can be larger than half the screen, even if
      // the number of lines is less. I'm not sure what impact this has.
      int height = bottomVisualLine - topVisualLine + 1;
      if (Math.abs(diff) > height / 2 || scrollOffset > height / 2) {
        EditorHelper.scrollVisualLineToMiddleOfScreen(editor, visualLine);
      }
      else {
        // Put the new cursor line "scrolljump" lines from the top/bottom. Ensure that the line is fully visible,
        // including block inlays above/below the line
        if (diff > 0) {
          int resLine = bottomVisualLine + diff + scrollJumpSize;
          EditorHelper.scrollVisualLineToBottomOfScreen(editor, resLine);
        }
        else {
          int resLine = topVisualLine + diff + scrollJumpSize;
          resLine = Math.min(resLine, EditorHelper.getVisualLineCount(editor) - height);
          resLine = Math.max(0, resLine);
          EditorHelper.scrollVisualLineToTopOfScreen(editor, resLine);
        }
      }
    }

    int visualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    int width = EditorHelper.getScreenWidth(editor);
    scrollJump = !CommandState.getInstance(editor).getFlags().contains(CommandFlags.FLAG_IGNORE_SIDE_SCROLL_JUMP);
    scrollOffset = OptionsManager.INSTANCE.getScrolloff().value();
    scrollJumpSize = 0;
    if (scrollJump) {
      scrollJumpSize = Math.max(0, OptionsManager.INSTANCE.getSidescroll().value() - 1);
      if (scrollJumpSize == 0) {
        scrollJumpSize = width / 2;
      }
    }

    int visualLeft = visualColumn + scrollOffset;
    int visualRight = visualColumn + width - scrollOffset;
    if (scrollOffset >= width / 2) {
      scrollOffset = width / 2;
      visualLeft = visualColumn + scrollOffset;
      visualRight = visualColumn + width - scrollOffset;
      if (visualLeft == visualRight) {
        visualRight++;
      }
    }

    scrollJumpSize = Math.min(scrollJumpSize, width / 2 - scrollOffset);

    if (column < visualLeft) {
      diff = column - visualLeft + 1;
      scrollJumpSize = -scrollJumpSize;
    }
    else {
      diff = column - visualRight + 1;
      if (diff < 0) {
        diff = 0;
      }
    }

    if (diff != 0) {
      int col;
      if (Math.abs(diff) > width / 2) {
        col = column - width / 2 - 1;
      }
      else {
        col = visualColumn + diff + scrollJumpSize;
      }

      col = Math.max(0, col);
      scrollColumnToLeftOfScreen(editor, col);
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
      int visualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
      visualLine = EditorHelper.normalizeVisualLine(editor, visualLine + lines);
      EditorHelper.scrollVisualLineToTopOfScreen(editor, visualLine);
    }
    else {
      int visualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
      visualLine = EditorHelper.normalizeVisualLine(editor, visualLine + lines);
      EditorHelper.scrollVisualLineToBottomOfScreen(editor, visualLine);
    }

    moveCaretToView(editor);

    return true;
  }

  @NotNull
  public TextRange getWordRange(@NotNull Editor editor,
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

    final VirtualFile vf = EditorHelper.getVirtualFile(editor);
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

    final VirtualFile vf = EditorHelper.getVirtualFile(editor);
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
                  EditorHelper.normalizeOffset(newEditor, newEditor.logicalPositionToOffset(lp), false));
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

  private void scrollColumnToScreenColumn(@NotNull Editor editor, int column) {
    int scrollOffset = OptionsManager.INSTANCE.getSidescrolloff().value();
    int width = EditorHelper.getScreenWidth(editor);
    if (scrollOffset > width / 2) {
      scrollOffset = width / 2;
    }
    if (column <= width / 2) {
      if (column < scrollOffset + 1) {
        column = scrollOffset + 1;
      }
    }
    else {
      if (column > width - scrollOffset) {
        column = width - scrollOffset;
      }
    }

    int visualColumn = editor.getCaretModel().getVisualPosition().column;
    scrollColumnToLeftOfScreen(editor, EditorHelper
      .normalizeVisualColumn(editor, editor.getCaretModel().getVisualPosition().line, visualColumn - column + 1,
                             false));
  }

  private static void scrollColumnToLeftOfScreen(@NotNull Editor editor, int column) {
    editor.getScrollingModel().scrollHorizontally(column * EditorHelper.getColumnWidth(editor));
  }

  public int moveCaretToMiddleColumn(@NotNull Editor editor, @NotNull Caret caret) {
    final int width = EditorHelper.getScreenWidth(editor) / 2;
    final int len = EditorHelper.getLineLength(editor);

    return moveCaretToColumn(editor, caret, Math.max(0, Math.min(len - 1, width)), false);
  }

  public int moveCaretToColumn(@NotNull Editor editor, @NotNull Caret caret, int count, boolean allowEnd) {
    int line = caret.getLogicalPosition().line;
    int pos = EditorHelper.normalizeColumn(editor, line, count, allowEnd);

    return editor.logicalPositionToOffset(new LogicalPosition(line, pos));
  }

  /**
   * @deprecated To move the caret, use {@link #moveCaretToColumn(Editor, Caret, int, boolean)}
   */
  public int moveCaretToColumn(@NotNull Editor editor, int count, boolean allowEnd) {
    return moveCaretToColumn(editor, editor.getCaretModel().getPrimaryCaret(), count, allowEnd);
  }

  public int moveCaretToLineStartSkipLeading(@NotNull Editor editor, @NotNull Caret caret) {
    int logicalLine = caret.getLogicalPosition().line;
    return moveCaretToLineStartSkipLeading(editor, logicalLine);
  }

  public int moveCaretToLineStartSkipLeading(@NotNull Editor editor, int line) {
    return EditorHelper.getLeadingCharacterOffset(editor, line);
  }

  /**
   * @deprecated To move the caret, use {@link #moveCaretToLineStartSkipLeading(Editor, Caret)}
   */
  public int moveCaretToLineStartSkipLeading(@NotNull Editor editor) {
    return moveCaretToLineStartSkipLeading(editor, editor.getCaretModel().getPrimaryCaret());
  }

  public int moveCaretToLineStartSkipLeadingOffset(@NotNull Editor editor, @NotNull Caret caret, int linesOffset) {
    int line = EditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset);
    return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, line));
  }

  /**
   * @deprecated To move the caret, use {@link #moveCaretToLineStartSkipLeadingOffset(Editor, Caret, int)}
   */
  public int moveCaretToLineStartSkipLeadingOffset(@NotNull Editor editor, int linesOffset) {
    return moveCaretToLineStartSkipLeadingOffset(editor, editor.getCaretModel().getPrimaryCaret(), linesOffset);
  }

  /**
   * @deprecated Use {@link #moveCaretToLineEnd(Editor, Caret)}
   */
  public int moveCaretToLineEnd(@NotNull Editor editor) {
    return moveCaretToLineEnd(editor, editor.getCaretModel().getPrimaryCaret());
  }

  public int moveCaretToLineEnd(@NotNull Editor editor, @NotNull Caret caret) {
    final VisualPosition visualPosition = caret.getVisualPosition();
    final int lastVisualLineColumn = EditorUtil.getLastVisualLineColumnNumber(editor, visualPosition.line);
    final VisualPosition visualEndOfLine = new VisualPosition(visualPosition.line, lastVisualLineColumn, true);
    return moveCaretToLineEnd(editor, editor.visualToLogicalPosition(visualEndOfLine).line, true);
  }

  public boolean scrollColumn(@NotNull Editor editor, int columns) {
    int visualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    visualColumn = EditorHelper
      .normalizeVisualColumn(editor, editor.getCaretModel().getVisualPosition().line, visualColumn + columns, false);

    scrollColumnToLeftOfScreen(editor, visualColumn);

    moveCaretToView(editor);

    return true;
  }

  /**
   * @deprecated To move the caret, use {@link #moveCaretToLineEndOffset(Editor, Caret, int, boolean)}
   */
  public int moveCaretToLineEndOffset(@NotNull Editor editor, int cntForward, boolean allowPastEnd) {
    return moveCaretToLineEndOffset(editor, editor.getCaretModel().getPrimaryCaret(), cntForward, allowPastEnd);
  }

  public int moveCaretToLineStart(@NotNull Editor editor, @NotNull Caret caret) {
    int logicalLine = caret.getLogicalPosition().line;
    return moveCaretToLineStart(editor, logicalLine);
  }

  /**
   * @deprecated To move the caret, use {@link #moveCaretToLineStart(Editor, Caret)}
   */
  public int moveCaretToLineStart(@NotNull Editor editor) {
    return moveCaretToLineStart(editor, editor.getCaretModel().getPrimaryCaret());
  }

  public int moveCaretToLineStart(@NotNull Editor editor, int line) {
    if (line >= EditorHelper.getLineCount(editor)) {
      return EditorHelper.getFileSize(editor);
    }
    return EditorHelper.getLineStartOffset(editor, line);
  }

  public int moveCaretToLineScreenStart(@NotNull Editor editor, @NotNull Caret caret) {
    final int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    return moveCaretToColumn(editor, caret, col, false);
  }

  public int moveCaretToLineScreenStartSkipLeading(@NotNull Editor editor, @NotNull Caret caret) {
    final int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    final int logicalLine = caret.getLogicalPosition().line;
    return EditorHelper.getLeadingCharacterOffset(editor, logicalLine, col);
  }

  public int moveCaretToLineScreenEnd(@NotNull Editor editor, @NotNull Caret caret, boolean allowEnd) {
    final int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor) + EditorHelper.getScreenWidth(editor) - 1;
    return moveCaretToColumn(editor, caret, col, allowEnd);
  }

  public int moveCaretHorizontalWrap(@NotNull Editor editor, @NotNull Caret caret, int count) {
    // FIX - allows cursor over newlines
    int oldOffset = caret.getOffset();
    int offset = Math.min(Math.max(0, caret.getOffset() + count), EditorHelper.getFileSize(editor));
    if (offset == oldOffset) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public int moveCaretHorizontal(@NotNull Editor editor, @NotNull Caret caret, int count, boolean allowPastEnd) {
    int oldOffset = caret.getOffset();
    int diff = 0;
    String text = editor.getDocument().getText();
    int sign = (int)Math.signum(count);
    for (Integer pointer : new IntProgression(0, count - sign, sign)) {
      int textPointer = oldOffset + pointer;
      if (textPointer < text.length() && textPointer >= 0) {
        // Actual char size can differ from 1 if unicode characters are used (like 🐔)
        diff += Character.charCount(text.codePointAt(textPointer));
      }
      else {
        diff += 1;
      }
    }
    int offset =
      EditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, oldOffset + (sign * diff), allowPastEnd);

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
        if (caretVisualLine > EditorHelper.getVisualLineCount(editor) - 1) {
          success = false;
        }

        int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
        if (caretVisualLine < topVisualLine + scrollOffset) {
          caretVisualLine = EditorHelper.normalizeVisualLine(editor, caretVisualLine + scrollOffset);
        }
      }
      else if (pages < 0) {
        int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
        if (caretVisualLine > bottomVisualLine - scrollOffset) {
          caretVisualLine = EditorHelper.normalizeVisualLine(editor, caretVisualLine - scrollOffset);
        }
      }

      int offset =
        moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, caretVisualLine));
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
    else if (logicalLine >= EditorHelper.getLineCount(editor)) {
      line = EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1);
      col = EditorHelper.getLineLength(editor, line);
    }

    LogicalPosition newPos = new LogicalPosition(line, EditorHelper.normalizeColumn(editor, line, col, false));

    return editor.logicalPositionToOffset(newPos);
  }

  public boolean scrollScreen(@NotNull final Editor editor, int rawCount, boolean down) {
    final CaretModel caretModel = editor.getCaretModel();
    final int currentLogicalLine = caretModel.getLogicalPosition().line;

    if ((!down && currentLogicalLine <= 0) || (down && currentLogicalLine >= EditorHelper.getLineCount(editor) - 1)) {
      return false;
    }

    final ScrollingModel scrollingModel = editor.getScrollingModel();
    final Rectangle visibleArea = scrollingModel.getVisibleArea();

    int targetCaretVisualLine = getScrollScreenTargetCaretVisualLine(editor, rawCount, down);

    // Scroll at most one screen height
    final int yInitialCaret = editor.visualLineToY(caretModel.getVisualPosition().line);
    final int yTargetVisualLine = editor.visualLineToY(targetCaretVisualLine);
    if (Math.abs(yTargetVisualLine - yInitialCaret) > visibleArea.height) {

      final int yPrevious = visibleArea.y;
      boolean moved;
      if (down) {
        targetCaretVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor) + 1;
        moved = EditorHelper.scrollVisualLineToTopOfScreen(editor, targetCaretVisualLine);
      }
      else {
        targetCaretVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor) - 1;
        moved = EditorHelper.scrollVisualLineToBottomOfScreen(editor, targetCaretVisualLine);
      }
      if (moved) {
        // We'll keep the caret at the same position, although that might not be the same line offset as previously
        targetCaretVisualLine = editor.yToVisualLine(yInitialCaret + scrollingModel.getVisibleArea().y - yPrevious);
      }
    }
    else {

      EditorHelper.scrollVisualLineToCaretLocation(editor, targetCaretVisualLine);

      final int scrollOffset = getNormalizedScrollOffset(editor);
      final int visualTop = EditorHelper.getVisualLineAtTopOfScreen(editor) + scrollOffset;
      final int visualBottom = EditorHelper.getVisualLineAtBottomOfScreen(editor) - scrollOffset;

      targetCaretVisualLine = Math.max(visualTop, Math.min(visualBottom, targetCaretVisualLine));
    }

    int logicalLine = EditorHelper.visualLineToLogicalLine(editor, targetCaretVisualLine);
    int caretOffset = moveCaretToLineStartSkipLeading(editor, logicalLine);
    moveCaret(editor, caretModel.getPrimaryCaret(), caretOffset);

    return true;
  }

  public int moveCaretToLineEndSkipLeadingOffset(@NotNull Editor editor, @NotNull Caret caret, int linesOffset) {
    int line = EditorHelper.visualLineToLogicalLine(editor, EditorHelper
      .normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset));
    int start = EditorHelper.getLineStartOffset(editor, line);
    int end = EditorHelper.getLineEndOffset(editor, line, true);
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
    return EditorHelper
      .normalizeOffset(editor, line, EditorHelper.getLineEndOffset(editor, line, allowPastEnd), allowPastEnd);
  }

  public int moveCaretGotoLineFirst(@NotNull Editor editor, int line) {
    return moveCaretToLineStartSkipLeading(editor, line);
  }

  // Scrolls current or [count] line to given screen location
  // In Vim, [count] refers to a file line, so it's a logical line
  private void scrollLineToScreenLocation(@NotNull Editor editor,
                                          @NotNull ScreenLocation screenLocation,
                                          int line,
                                          boolean start) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    line = EditorHelper.normalizeLine(editor, line);
    int visualLine = line == 0
                     ? editor.getCaretModel().getVisualPosition().line
                     : EditorHelper.logicalLineToVisualLine(editor, line - 1);

    // This method moves the current (or [count]) line to the specified screen location
    // Scroll offset is applicable, but scroll jump isn't. Offset is applied to screen lines (visual lines)
    switch (screenLocation) {
      case TOP:
        EditorHelper.scrollVisualLineToTopOfScreen(editor, visualLine - scrollOffset);
        break;
      case MIDDLE:
        EditorHelper.scrollVisualLineToMiddleOfScreen(editor, visualLine);
        break;
      case BOTTOM:
        EditorHelper.scrollVisualLineToBottomOfScreen(editor, visualLine + scrollOffset);
        break;
    }
    if (visualLine != editor.getCaretModel().getVisualPosition().line || start) {
      int offset;
      if (start) {
        offset = moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, visualLine));
      }
      else {
        offset = moveCaretVertical(editor, editor.getCaretModel().getPrimaryCaret(),
                                   EditorHelper.visualLineToLogicalLine(editor, visualLine) -
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
    final LogicalPosition logicalPosition = caret.getLogicalPosition();
    if ((pos.line == 0 && count < 0) || (pos.line >= EditorHelper.getVisualLineCount(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      boolean savedColumn = true;
      int col = UserDataManager.getVimLastColumn(caret);
      int line = EditorHelper.normalizeVisualLine(editor, pos.line + count);
      final CommandState.Mode mode = CommandStateHelper.getMode(editor);
      final int lastColumnCurrentLine = EditorHelper.lastColumnForLine(editor, logicalPosition.line, CommandStateHelper.isEndAllowed(mode));

      if (lastColumnCurrentLine != pos.column) {
        col = pos.column;
        savedColumn = false;
      }

      // Inline hints are counted as 1 in visual position. So, to keep the correct column
      //   we decrease the column by the number of inline hints
      int curLineStartOffset = editor.getDocument().getLineStartOffset(logicalPosition.line);
      int curInlineElements = editor.getInlayModel().getInlineElementsInRange(curLineStartOffset, caret.getOffset()).size();

      VisualPosition newVisualPos = new VisualPosition(line, col);
      int newOffset = EditorHelper.visualPositionToOffset(editor, newVisualPos);
      int lineStartNewOffset = editor.getDocument().getLineStartOffset(editor.visualToLogicalPosition(newVisualPos).line);
      int newInlineElements = editor.getInlayModel().getInlineElementsInRange(lineStartNewOffset, newOffset).size();

      if (!savedColumn) col -= curInlineElements;

      col = EditorHelper
        .normalizeVisualColumn(editor, line, col, CommandStateHelper.isEndAllowed(CommandStateHelper.getMode(editor)));
      col += newInlineElements;
      VisualPosition newPos = new VisualPosition(line, col);

      return EditorHelper.visualPositionToOffset(editor, newPos);
    }
  }

  public int moveCaretToLinePercent(@NotNull Editor editor, int count) {
    if (count > 100) count = 100;

    return moveCaretToLineStartSkipLeading(editor, EditorHelper
      .normalizeLine(editor, (EditorHelper.getLineCount(editor) * count + 99) / 100 - 1));
  }

  public int moveCaretGotoLineLast(@NotNull Editor editor, int rawCount) {
    final int line =
      rawCount == 0 ? EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) : rawCount - 1;

    return moveCaretToLineStartSkipLeading(editor, line);
  }

  public int moveCaretGotoLineLastEnd(@NotNull Editor editor, int rawCount, int line, boolean pastEnd) {
    return moveCaretToLineEnd(editor, rawCount == 0
                                      ? EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1)
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

    int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
    int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);

    // Don't apply scrolloff if we're at the top or bottom of the file
    int offsetTopVisualLine = topVisualLine > 0 ? topVisualLine + scrollOffset : topVisualLine;
    int offsetBottomVisualLine =
      bottomVisualLine < EditorHelper.getVisualLineCount(editor) ? bottomVisualLine - scrollOffset : bottomVisualLine;

    // [count]H/[count]L moves caret to that screen line, bounded by top/bottom scroll offsets
    int targetVisualLine = 0;
    switch (screenLocation) {
      case TOP:
        targetVisualLine = Math.max(offsetTopVisualLine, topVisualLine + visualLineOffset - 1);
        targetVisualLine = Math.min(targetVisualLine, offsetBottomVisualLine);
        break;
      case MIDDLE:
        targetVisualLine = EditorHelper.getVisualLineAtMiddleOfScreen(editor);
        break;
      case BOTTOM:
        targetVisualLine = Math.min(offsetBottomVisualLine, bottomVisualLine - visualLineOffset + 1);
        targetVisualLine = Math.max(targetVisualLine, offsetTopVisualLine);
        break;
    }

    return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, targetVisualLine));
  }

  public int moveCaretToLineEndOffset(@NotNull Editor editor,
                                      @NotNull Caret caret,
                                      int cntForward,
                                      boolean allowPastEnd) {
    int line = EditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + cntForward);

    if (line < 0) {
      return 0;
    }
    else {
      return moveCaretToLineEnd(editor, EditorHelper.visualLineToLogicalLine(editor, line), allowPastEnd);
    }
  }

}
