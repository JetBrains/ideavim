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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.action.motion.TextObjectAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.Jump;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.handler.ExecuteMethodNotOverriddenException;
import com.maddyhome.idea.vim.helper.CaretData;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.option.BoundStringOption;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EnumSet;

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
   * Create the group
   */
  public MotionGroup() {
    EventFacade.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        // This ridiculous code ensures that a lot of events are processed BEFORE we finally start listening
        // to visible area changes. The primary reason for this change is to fix the cursor position bug
        // using the gd and gD commands (Goto Declaration). This bug has been around since Idea 6.0.4?
        // Prior to this change the visible area code was moving the cursor around during file load and messing
        // with the cursor position of the Goto Declaration processing.
        ApplicationManager.getApplication().invokeLater(
            () -> ApplicationManager.getApplication().invokeLater(
                () -> ApplicationManager.getApplication().invokeLater(
                    () -> {
                      addEditorListener(editor);
                      EditorData.setMotionGroup(editor, true);
                    })
            )
        );
      }

      public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (EditorData.getMotionGroup(editor)) {
          removeEditorListener(editor);
          EditorData.setMotionGroup(editor, false);
        }
      }
    }, ApplicationManager.getApplication());
  }

  public void turnOn() {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      if (!EditorData.getMotionGroup(editor)) {
        addEditorListener(editor);
        EditorData.setMotionGroup(editor, true);
      }
    }
  }

  public void turnOff() {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      if (EditorData.getMotionGroup(editor)) {
        removeEditorListener(editor);
        EditorData.setMotionGroup(editor, false);
      }
    }
  }

  private void addEditorListener(@NotNull Editor editor) {
    final EventFacade eventFacade = EventFacade.getInstance();
    eventFacade.addEditorMouseListener(editor, mouseHandler);
    eventFacade.addEditorMouseMotionListener(editor, mouseHandler);
    eventFacade.addEditorSelectionListener(editor, selectionHandler);
  }

  private void removeEditorListener(@NotNull Editor editor) {
    final EventFacade eventFacade = EventFacade.getInstance();
    eventFacade.removeEditorMouseListener(editor, mouseHandler);
    eventFacade.removeEditorMouseMotionListener(editor, mouseHandler);
    eventFacade.removeEditorSelectionListener(editor, selectionHandler);
  }

  /**
   * Process mouse clicks by setting/resetting visual mode. There are some strange scenarios to handle.
   *
   * @param editor The editor
   * @param event  The mouse event
   */
  private void processMouseClick(@NotNull Editor editor, @NotNull MouseEvent event) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    ExOutputModel.getInstance(editor).clear();

    CommandState.SubMode visualMode = CommandState.SubMode.NONE;
    switch (event.getClickCount()) {
      case 2:
        visualMode = CommandState.SubMode.VISUAL_CHARACTER;
        break;
      case 3:
        visualMode = CommandState.SubMode.VISUAL_LINE;
        // Pop state of being in Visual Char mode
        if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
          CommandState.getInstance(editor).popState();
        }

        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        editor.getSelectionModel().setSelection(start, Math.max(start, end - 1));

        break;
    }

    setVisualMode(editor, visualMode);

    final CaretModel caretModel = editor.getCaretModel();
    if (CommandState.getInstance(editor).getSubMode() != CommandState.SubMode.NONE) {
      caretModel.removeSecondaryCarets();
    }

    switch (CommandState.getInstance(editor).getSubMode()) {
      case NONE:
        VisualPosition vp = caretModel.getVisualPosition();
        int col = EditorHelper.normalizeVisualColumn(editor, vp.line, vp.column,
            CommandState.getInstance(editor).getMode() ==
                CommandState.Mode.INSERT ||
                CommandState.getInstance(editor).getMode() ==
                    CommandState.Mode.REPLACE);
        if (col != vp.column) {
          caretModel.moveToVisualPosition(new VisualPosition(vp.line, col));
        }
        MotionGroup.scrollCaretIntoView(editor);
        break;
      case VISUAL_CHARACTER:
        caretModel.moveToOffset(CaretData.getVisualEnd(caretModel.getPrimaryCaret()));
        break;
      case VISUAL_LINE:
        caretModel.moveToLogicalPosition(editor.xyToLogicalPosition(event.getPoint()));
        break;
    }

    CaretData.setVisualOffset(caretModel.getPrimaryCaret(), caretModel.getOffset());

    CaretData.setLastColumn(editor, caretModel.getPrimaryCaret(), caretModel.getVisualPosition().column);
  }

  /**
   * Handles mouse drags by properly setting up visual mode based on the new selection.
   *
   * @param editor The editor the mouse drag occurred in.
   * @param update True if update, false if not.
   */
  private void processLineSelection(@NotNull Editor editor, boolean update) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    ExOutputModel.getInstance(editor).clear();

    if (update) {
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        for (@NotNull Caret caret : editor.getCaretModel().getAllCarets()) {
          updateSelection(editor, caret, caret.getOffset());
        }
      }
    }
    else {
      if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
        CommandState.getInstance(editor).popState();
      }

      int start = editor.getSelectionModel().getSelectionStart();
      int end = editor.getSelectionModel().getSelectionEnd();
      editor.getSelectionModel().setSelection(start, Math.max(start, end - 1));

      setVisualMode(editor, CommandState.SubMode.VISUAL_LINE);

      final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
      VisualChange range = getVisualOperatorRange(editor, primaryCaret, EnumSet.of(CommandFlags.FLAG_MOT_LINEWISE));
      if (range.getLines() > 1) {
        MotionGroup.moveCaret(editor, primaryCaret,
                              moveCaretVertical(editor, primaryCaret, -1));
      }
    }
  }

  private void processMouseReleased(@NotNull Editor editor, @NotNull CommandState.SubMode mode, int startOff,
                                    int endOff) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    ExOutputModel.getInstance(editor).clear();

    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      CommandState.getInstance(editor).popState();
    }

    int start = editor.getSelectionModel().getSelectionStart();
    int end = editor.getSelectionModel().getSelectionEnd();
    if (start == end) return;

    if (mode == CommandState.SubMode.VISUAL_LINE) {
      end--;
      endOff--;
    }

    if (end == startOff || end == endOff) {
      int t = start;
      start = end;
      end = t;

      if (mode == CommandState.SubMode.VISUAL_CHARACTER) {
        start--;
      }
    }

    MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), start);
    toggleVisual(editor, 1, 0, mode);
    MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), end);
    KeyHandler.getInstance().reset(editor);
  }

  @NotNull
  public TextRange getWordRange(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isOuter,
                                boolean isBig) {
    int dir = 1;
    boolean selection = false;
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      if (CaretData.getVisualEnd(caret) < CaretData.getVisualStart(caret)) {
        dir = -1;
      }
      if (CaretData.getVisualStart(caret) != CaretData.getVisualEnd(caret)) {
        selection = true;
      }
    }

    return SearchHelper.findWordUnderCursor(editor, caret, count, dir, isOuter, isBig, selection);
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
   * @param incNewline True if to include newline
   * @return The motion's range
   */
  @Nullable
  public static TextRange getMotionRange(@NotNull Editor editor, @NotNull Caret caret, DataContext context, int count,
                                         int rawCount, @NotNull Argument argument, boolean incNewline) {
    final Command cmd = argument.getMotion();
    if (cmd == null) {
      return null;
    }
    // Normalize the counts between the command and the motion argument
    int cnt = cmd.getCount() * count;
    int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
    int start = 0;
    int end = 0;
    if (cmd.getAction() instanceof MotionEditorAction) {
      MotionEditorAction action = (MotionEditorAction) cmd.getAction();

      // This is where we are now
      start = caret.getOffset();

      // Execute the motion (without moving the cursor) and get where we end
      try {
        end = action.getOffset(editor, caret, context, cnt, raw, cmd.getArgument());
      } catch (ExecuteMethodNotOverriddenException e) {
        // This actually should have fallen even earlier.
        end = -1;
        VimPlugin.indicateError();
      }

      // Invalid motion
      if (end == -1) {
        return null;
      }
    }
    else if (cmd.getAction() instanceof TextObjectAction) {
      TextObjectAction action = (TextObjectAction) cmd.getAction();

      TextRange range = action.getRange(editor, caret, context, cnt, raw, cmd.getArgument());

      if (range == null) {
        return null;
      }

      start = range.getStartOffset();
      end = range.getEndOffset();
    }

    // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
    // of the line and move the end to the end of the line.
    EnumSet<CommandFlags> flags = cmd.getFlags();
    if (flags.contains(CommandFlags.FLAG_MOT_LINEWISE)) {
      if (start > end) {
        int t = start;
        start = end;
        end = t;
      }

      start = EditorHelper.getLineStartForOffset(editor, start);
      end = Math
          .min(EditorHelper.getLineEndForOffset(editor, end) + (incNewline ? 1 : 0), EditorHelper.getFileSize(editor));
    }
    // If characterwise and inclusive, add the last character to the range
    else if (flags.contains(CommandFlags.FLAG_MOT_INCLUSIVE)) {
      end++;
    }

    // Normalize the range
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    return new TextRange(start, end);
  }

  public int moveCaretToNthCharacter(@NotNull Editor editor, int count) {
    return Math.max(0, Math.min(count, EditorHelper.getFileSize(editor) - 1));
  }

  public int moveCaretToFileMark(@NotNull Editor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getFileMark(editor, ch);
    if (mark == null) return -1;

    final int line = mark.getLogicalLine();
    return toLineStart ? moveCaretToLineStartSkipLeading(editor, line)
                       : editor.logicalPositionToOffset(new LogicalPosition(line, mark.getCol()));
  }

  public int moveCaretToMark(@NotNull Editor editor, char ch, boolean toLineStart) {
    final Mark mark = VimPlugin.getMark().getMark(editor, ch);
    if (mark == null) return -1;

    final VirtualFile vf = EditorData.getVirtualFile(editor);
    if (vf == null) return -1;

    final int line = mark.getLogicalLine();
    if (vf.getPath().equals(mark.getFilename())) {
      return toLineStart ? moveCaretToLineStartSkipLeading(editor, line)
                         : editor.logicalPositionToOffset(new LogicalPosition(line, mark.getCol()));
    }

    final Editor selectedEditor = selectEditor(editor, mark);
    if (selectedEditor != null) {
      for (Caret caret : selectedEditor.getCaretModel().getAllCarets()) {
        moveCaret(selectedEditor, caret, toLineStart ? moveCaretToLineStartSkipLeading(selectedEditor, line)
                                                     : selectedEditor.logicalPositionToOffset(
                                                         new LogicalPosition(line, mark.getCol())));
      }
    }
    return -2;
  }

  public int moveCaretToJump(@NotNull Editor editor, @NotNull Caret caret, int count) {
    final int spot = VimPlugin.getMark().getJumpSpot();
    final Jump jump = VimPlugin.getMark().getJump(count);

    if (jump == null) {
      return -1;
    }

    final VirtualFile vf = EditorData.getVirtualFile(editor);
    if (vf == null) {
      return -1;
    }

    final LogicalPosition lp = new LogicalPosition(jump.getLogicalLine(), jump.getCol());
    final String fileName = jump.getFilename();
    if (!vf.getPath().equals(fileName) && fileName != null) {
      final VirtualFile newFile = LocalFileSystem.getInstance().findFileByPath(fileName.replace(File.separatorChar, '/'));
      if (newFile == null) {
        return -2;
      }

      final Editor newEditor = selectEditor(editor, newFile);
      if (newEditor != null) {
        if (spot == -1) {
          VimPlugin.getMark().addJump(editor, false);
        }
        moveCaret(newEditor, caret, EditorHelper.normalizeOffset(newEditor, newEditor.logicalPositionToOffset(lp), false));
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
    if (mark.getFilename() == null) {
      return null;
    }
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
   * @param caret   The caret to be moved
   * @param count   The number of words to skip
   * @param bigWord If true then find WORD, if false then find word
   * @return position
   */
  public int moveCaretToNextWord(@NotNull Editor editor, @NotNull Caret caret, int count, boolean bigWord) {
    final int offset = caret.getOffset();
    final int size = EditorHelper.getFileSize(editor);
    if ((offset == 0 && count < 0) || (offset >= size - 1 && count > 0)) {
      return -1;
    }
    return SearchHelper.findNextWord(editor, caret, count, bigWord);
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

  private void scrollColumnToScreenColumn(@NotNull Editor editor, int column) {
    int scrollOffset = ((NumberOption) Options.getInstance().getOption("sidescrolloff")).value();
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

  // Scrolls current or [count] line to given screen location
  // In Vim, [count] refers to a file line, so it's a logical line
  private void scrollLineToScreenLocation(@NotNull Editor editor, @NotNull ScreenLocation screenLocation, int line,
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

  public int moveCaretToFirstScreenLine(@NotNull Editor editor, int count) {
    return moveCaretToScreenLocation(editor, ScreenLocation.TOP, count);
  }

  public int moveCaretToLastScreenLine(@NotNull Editor editor, int count) {
    return moveCaretToScreenLocation(editor, ScreenLocation.BOTTOM, count);
  }

  public int moveCaretToMiddleScreenLine(@NotNull Editor editor) {
    return moveCaretToScreenLocation(editor, ScreenLocation.MIDDLE, 0);
  }

  // [count] is a visual line offset, which means it's 1 based. The value is ignored for ScreenLocation.MIDDLE
  private int moveCaretToScreenLocation(@NotNull Editor editor, @NotNull ScreenLocation screenLocation,
                                        int visualLineOffset) {
    final int scrollOffset = getNormalizedScrollOffset(editor);

    int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
    int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);

    // Don't apply scrolloff if we're at the top or bottom of the file
    int offsetTopVisualLine = topVisualLine > 0 ? topVisualLine + scrollOffset : topVisualLine;
    int offsetBottomVisualLine = bottomVisualLine < EditorHelper.getVisualLineCount(editor) ? bottomVisualLine - scrollOffset : bottomVisualLine;

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

  public boolean scrollColumn(@NotNull Editor editor, int columns) {
    int visualColumn = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    visualColumn = EditorHelper
        .normalizeVisualColumn(editor, editor.getCaretModel().getVisualPosition().line, visualColumn + columns, false);

    scrollColumnToLeftOfScreen(editor, visualColumn);

    moveCaretToView(editor);

    return true;
  }

  public boolean scrollLine(@NotNull Editor editor, int lines) {
    assert lines != 0 : "lines cannot be 0";

    if (lines > 0) {
      int visualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
      visualLine = EditorHelper.normalizeVisualLine(editor, visualLine + lines);
      EditorHelper.scrollVisualLineToTopOfScreen(editor, visualLine);
    }
    else if (lines < 0) {
      int visualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
      visualLine = EditorHelper.normalizeVisualLine(editor, visualLine + lines);
      EditorHelper.scrollVisualLineToBottomOfScreen(editor, visualLine);
    }

    moveCaretToView(editor);

    return true;
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

    int sideScrollOffset = ((NumberOption) Options.getInstance().getOption("sidescrolloff")).value();
    int width = EditorHelper.getScreenWidth(editor);
    if (sideScrollOffset > width / 2) {
      sideScrollOffset = width / 2;
    }

    int col = editor.getCaretModel().getVisualPosition().column;
    int oldColumn = col;
    if (col >= EditorHelper.getLineLength(editor) - 1) {
      col = CaretData.getLastColumn(editor.getCaretModel().getPrimaryCaret());
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

    newColumn = EditorHelper.normalizeVisualColumn(editor, newline, newColumn, CommandState.inInsertMode(editor));

    if (newline != caretVisualLine || newColumn != oldColumn) {
      int offset = EditorHelper.visualPositionToOffset(editor, new VisualPosition(newline, newColumn));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);

      CaretData.setLastColumn(editor, editor.getCaretModel().getPrimaryCaret(), col);
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
        int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen( editor);
        if (caretVisualLine > bottomVisualLine - scrollOffset) {
          caretVisualLine = EditorHelper.normalizeVisualLine(editor, caretVisualLine - scrollOffset);
        }
      }

      int offset = moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, caretVisualLine));
      moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
      return success;
    }

    return false;
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
      } else {
        targetCaretVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor) - 1;
        moved = EditorHelper.scrollVisualLineToBottomOfScreen(editor, targetCaretVisualLine);
      }
      if (moved) {
        // We'll keep the caret at the same position, although that might not be the same line offset as previously
        targetCaretVisualLine = editor.yToVisualLine(yInitialCaret + scrollingModel.getVisibleArea().y - yPrevious);
      }
    } else {

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

  private static int getScrollScreenTargetCaretVisualLine(@NotNull final Editor editor, int rawCount, boolean down) {
    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    final int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
    final int scrollOption = getScrollOption(rawCount);

    int targetCaretVisualLine;
    if (scrollOption == 0) {
      // Scroll up/down half window size by default. We can't use line count here because of block inlays
      final int offset = down ? (visibleArea.height / 2) : editor.getLineHeight() - (visibleArea.height / 2);
      targetCaretVisualLine = editor.yToVisualLine(editor.visualLineToY(caretVisualLine) + offset);
    } else {
      targetCaretVisualLine = down ? caretVisualLine + scrollOption : caretVisualLine - scrollOption;
    }

    return targetCaretVisualLine;
  }

  private static int getScrollOption(int rawCount) {
    NumberOption scroll = (NumberOption) Options.getInstance().getOption("scroll");
    if (rawCount == 0) {
      return scroll.value();
    }
    // TODO: This needs to be reset whenever the window size changes
    scroll.set(rawCount);
    return rawCount;
  }

  private static int getNormalizedScrollOffset(@NotNull final Editor editor) {
    int scrollOffset = ((NumberOption) Options.getInstance().getOption("scrolloff")).value();
    return EditorHelper.normalizeScrollOffset(editor, scrollOffset);
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

  public int moveCaretToLineEndSkipLeadingOffset(@NotNull Editor editor, @NotNull Caret caret, int linesOffset) {
    int line = EditorHelper.visualLineToLogicalLine(editor,
                                                    EditorHelper.normalizeVisualLine(editor,
                                                                                     caret.getVisualPosition().line + linesOffset));
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

  public int moveCaretToLineEnd(@NotNull Editor editor, int line, boolean allowPastEnd) {
    return EditorHelper
        .normalizeOffset(editor, line, EditorHelper.getLineEndOffset(editor, line, allowPastEnd), allowPastEnd);
  }

  public int moveCaretToLineEndOffset(@NotNull Editor editor, @NotNull Caret caret, int cntForward,
                                      boolean allowPastEnd) {
    int line = EditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + cntForward);

    if (line < 0) {
      return 0;
    }
    else {
      return moveCaretToLineEnd(editor, EditorHelper.visualLineToLogicalLine(editor, line), allowPastEnd);
    }
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
    VisualPosition visualPosition = caret.getVisualPosition();
    int newOffset = EditorHelper.visualPositionToOffset(editor, new VisualPosition(visualPosition.line, Math
      .max(0, visualPosition.column + count)));
    int offset = EditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, newOffset, allowPastEnd);

    if (offset == caret.getOffset()) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public int moveCaretVertical(@NotNull Editor editor, @NotNull Caret caret, int count) {
    VisualPosition pos = caret.getVisualPosition();
    if ((pos.line == 0 && count < 0) || (pos.line >= EditorHelper.getVisualLineCount(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int col = CaretData.getLastColumn(caret);
      int line = EditorHelper.normalizeVisualLine(editor, pos.line + count);
      VisualPosition newPos = new VisualPosition(line, EditorHelper
          .normalizeVisualColumn(editor, line, col, CommandState.inInsertMode(editor)));

      return EditorHelper.visualPositionToOffset(editor, newPos);
    }
  }

  public int moveCaretToLine(@NotNull Editor editor, int logicalLine) {
    int col = CaretData.getLastColumn(editor.getCaretModel().getPrimaryCaret());
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

  public int moveCaretToLinePercent(@NotNull Editor editor, int count) {
    if (count > 100) count = 100;

    return moveCaretToLineStartSkipLeading(editor, EditorHelper
        .normalizeLine(editor, (EditorHelper.getLineCount(editor) * count + 99) / 100 - 1));
  }

  public int moveCaretGotoLineLast(@NotNull Editor editor, int rawCount) {
    final int line = rawCount == 0 ?
        EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) :
        rawCount - 1;

    return moveCaretToLineStartSkipLeading(editor, line);
  }

  public int moveCaretGotoLineLastEnd(@NotNull Editor editor, int rawCount, int line, boolean pastEnd) {
    return moveCaretToLineEnd(editor, rawCount == 0
        ? EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1)
        : line, pastEnd);
  }

  public int moveCaretGotoLineFirst(@NotNull Editor editor, int line) {
    return moveCaretToLineStartSkipLeading(editor, line);
  }

  public static void moveCaret(@NotNull Editor editor, @NotNull Caret caret, int offset) {
    moveCaret(editor, caret, offset, false);
  }

  public static void moveCaret(@NotNull Editor editor, @NotNull Caret caret, int offset, boolean forceKeepVisual) {
    if (offset >= 0 && offset <= editor.getDocument().getTextLength()) {
      final boolean keepVisual = forceKeepVisual || keepVisual(editor);
      if (caret.getOffset() != offset) {
        caret.moveToOffset(offset);
        CaretData.setLastColumn(editor, caret, caret.getVisualPosition().column);
        if (caret == editor.getCaretModel().getPrimaryCaret()) {
          scrollCaretIntoView(editor);
        }
      }

      if (keepVisual) {
        VimPlugin.getMotion().updateSelection(editor, caret, offset);
      }
      else {
        editor.getSelectionModel().removeSelection();
      }
    }
  }

  private static boolean keepVisual(Editor editor) {
    final CommandState commandState = CommandState.getInstance(editor);
    if (commandState.getMode() == CommandState.Mode.VISUAL) {
      final Command command = commandState.getCommand();
      return command == null || !command.getFlags().contains(CommandFlags.FLAG_EXIT_VISUAL);
    }
    return false;
  }

  /**
   * If 'absolute' is true, then set tab index to 'value', otherwise add 'value' to tab index with wraparound.
   */
  private void switchEditorTab(@Nullable EditorWindow editorWindow, int value, boolean absolute) {
    if (editorWindow != null) {
      final EditorTabbedContainer tabbedPane = editorWindow.getTabbedPane();
      if (tabbedPane != null) {
        if (absolute) {
          tabbedPane.setSelectedIndex(value);
        }
        else {
          int tabIndex = (value + tabbedPane.getSelectedIndex()) % tabbedPane.getTabCount();
          tabbedPane.setSelectedIndex(tabIndex < 0 ? tabIndex + tabbedPane.getTabCount() : tabIndex);
        }
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

  private static void scrollCaretIntoView(@NotNull Editor editor) {
    final boolean scrollJump = !CommandState.getInstance(editor).getFlags().contains(CommandFlags.FLAG_IGNORE_SCROLL_JUMP);
    scrollPositionIntoView(editor, editor.getCaretModel().getVisualPosition(), scrollJump);
  }

  public static void scrollPositionIntoView(@NotNull Editor editor, @NotNull VisualPosition position,
                                            boolean scrollJump) {
    final int topVisualLine = EditorHelper.getVisualLineAtTopOfScreen(editor);
    final int bottomVisualLine = EditorHelper.getVisualLineAtBottomOfScreen(editor);
    final int visualLine = position.line;
    final int column = position.column;

    // We need the non-normalised value here, so we can handle cases such as so=999 to keep the current line centred
    int scrollOffset = ((NumberOption) Options.getInstance().getOption("scrolloff")).value();

    int scrollJumpSize = 0;
    if (scrollJump) {
      scrollJumpSize = Math.max(0, ((NumberOption) Options.getInstance().getOption("scrolljump")).value() - 1);
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
    } else {
      diff = Math.max(0, visualLine - visualBottom + 1);
    }

    if (diff != 0) {

      // If we need to scroll the current line more than half a screen worth of lines then we just centre the new
      // current line. This mimics vim behaviour of e.g. 100G in a 300 line file with a screen size of 25 centering line
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
    scrollOffset = ((NumberOption) Options.getInstance().getOption("sidescrolloff")).value();
    scrollJumpSize = 0;
    if (scrollJump) {
      scrollJumpSize = Math.max(0, ((NumberOption) Options.getInstance().getOption("sidescroll")).value() - 1);
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

  public boolean selectPreviousVisualMode(@NotNull Editor editor) {
    final SelectionType lastSelectionType = EditorData.getLastSelectionType(editor);
    if (lastSelectionType == null) {
      return false;
    }

    final TextRange visualMarks = VimPlugin.getMark().getVisualSelectionMarks(editor);
    if (visualMarks == null) {
      return false;
    }

    editor.getCaretModel().removeSecondaryCarets();

    CommandState.getInstance(editor)
        .pushState(CommandState.Mode.VISUAL, lastSelectionType.toSubMode(), MappingMode.VISUAL);

    Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();

    CaretData.setVisualStart(primaryCaret, visualMarks.getStartOffset());
    CaretData.setVisualEnd(primaryCaret, visualMarks.getEndOffset());
    CaretData.setVisualOffset(primaryCaret, visualMarks.getEndOffset());

    updateSelection(editor, primaryCaret, visualMarks.getEndOffset());

    primaryCaret.moveToOffset(visualMarks.getEndOffset());
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

    return true;
  }

  public boolean swapVisualSelections(@NotNull Editor editor) {
    final SelectionType lastSelectionType = EditorData.getLastSelectionType(editor);
    final TextRange lastVisualRange = EditorData.getLastVisualRange(editor);
    if (lastSelectionType == null || lastVisualRange == null) {
      return false;
    }

    final SelectionType selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).getSubMode());
    EditorData.setLastSelectionType(editor, selectionType);

    editor.getCaretModel().removeSecondaryCarets();

    Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
    CaretData.setVisualStart(primaryCaret, lastVisualRange.getStartOffset());
    CaretData.setVisualEnd(primaryCaret, lastVisualRange.getEndOffset());
    CaretData.setVisualOffset(primaryCaret, lastVisualRange.getEndOffset());

    CommandState.getInstance(editor).setSubMode(lastSelectionType.toSubMode());

    updateSelection(editor, primaryCaret, lastVisualRange.getEndOffset());

    primaryCaret.moveToOffset(lastVisualRange.getEndOffset());
    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

    return true;
  }

  public void setVisualMode(@NotNull Editor editor, @NotNull CommandState.SubMode mode) {
    CommandState.SubMode oldMode = CommandState.getInstance(editor).getSubMode();
    if (mode == CommandState.SubMode.NONE) {
      int start = editor.getSelectionModel().getSelectionStart();
      int end = editor.getSelectionModel().getSelectionEnd();
      if (start != end) {
        int line = editor.offsetToLogicalPosition(start).line;
        int logicalStart = EditorHelper.getLineStartOffset(editor, line);
        int lend = EditorHelper.getLineEndOffset(editor, line, true);
        if (logicalStart == start && lend + 1 == end) {
          mode = CommandState.SubMode.VISUAL_LINE;
        }
        else {
          mode = CommandState.SubMode.VISUAL_CHARACTER;
        }
      }
    }

    if (oldMode == CommandState.SubMode.NONE && mode == CommandState.SubMode.NONE) {
      editor.getSelectionModel().removeSelection();
      return;
    }

    if (mode == CommandState.SubMode.NONE) {
      exitVisual(editor);
    }
    else {
      CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, mode, MappingMode.VISUAL);
    }

    KeyHandler.getInstance().reset(editor);

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      CaretData.setVisualStart(caret, caret.getSelectionStart());
      int visualEnd = caret.getSelectionEnd();
      if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_CHARACTER) {
        BoundStringOption opt = (BoundStringOption) Options.getInstance().getOption("selection");
        int adj = 1;
        if (opt.getValue().equals("exclusive")) {
          adj = 0;
        }
        visualEnd -= adj;
      }
      CaretData.setVisualEnd(caret, visualEnd);
      CaretData.setVisualOffset(caret, caret.getOffset());
    }

    VimPlugin.getMark().setVisualSelectionMarks(editor, getRawVisualRange(editor.getCaretModel().getPrimaryCaret()));
  }

  public boolean toggleVisual(@NotNull Editor editor, int count, int rawCount, @NotNull CommandState.SubMode mode) {
    CommandState.SubMode currentMode = CommandState.getInstance(editor).getSubMode();
    if (CommandState.getInstance(editor).getMode() != CommandState.Mode.VISUAL) {
      if (rawCount > 0) {
        if (editor.getCaretModel().getCaretCount() > 1) {
          return false;
        }
        VisualChange range = CaretData.getLastVisualOperatorRange(editor.getCaretModel().getPrimaryCaret());
        if (range == null) {
          return false;
        }
        mode = range.getType().toSubMode();
        int start = editor.getCaretModel().getOffset();
        int end = calculateVisualRange(editor, range, count);
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, mode, MappingMode.VISUAL);
        CaretData.setVisualStart(primaryCaret, start);
        updateSelection(editor, primaryCaret, end);
        MotionGroup.moveCaret(editor, primaryCaret, CaretData.getVisualEnd(primaryCaret), true);
      }
      else {
        CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, mode, MappingMode.VISUAL);
        if (mode == CommandState.SubMode.VISUAL_BLOCK) {
          EditorData.setVisualBlockStart(editor, editor.getSelectionModel().getSelectionStart());
          updateBlockSelection(editor, editor.getSelectionModel().getSelectionEnd());
          MotionGroup
              .moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), EditorData.getVisualBlockEnd(editor), true);
        }
        else {
          for (Caret caret : editor.getCaretModel().getAllCarets()) {
            CaretData.setVisualStart(caret, caret.getSelectionStart());
            updateSelection(editor, caret, caret.getSelectionEnd());
            MotionGroup.moveCaret(editor, caret, CaretData.getVisualEnd(caret), true);
          }
        }
      }
    }
    else if (mode == currentMode) {
      exitVisual(editor);
    }
    else if (mode == CommandState.SubMode.VISUAL_BLOCK) {
      CommandState.getInstance(editor).setSubMode(mode);
      updateBlockSelection(editor, EditorData.getVisualBlockEnd(editor));
    }
    else {
      CommandState.getInstance(editor).setSubMode(mode);
      for (Caret caret : editor.getCaretModel().getAllCarets()) {
        updateSelection(editor, caret, CaretData.getVisualEnd(caret));
      }
    }

    return true;
  }

  private int calculateVisualRange(@NotNull Editor editor, @NotNull VisualChange range, int count) {
    int lines = range.getLines();
    int chars = range.getColumns();
    if (range.getType() == SelectionType.LINE_WISE || range.getType() == SelectionType.BLOCK_WISE || lines > 1) {
      lines *= count;
    }
    if ((range.getType() == SelectionType.CHARACTER_WISE && lines == 1) ||
        range.getType() == SelectionType.BLOCK_WISE) {
      chars *= count;
    }
    int start = editor.getCaretModel().getOffset();
    LogicalPosition sp = editor.offsetToLogicalPosition(start);
    int endLine = sp.line + lines - 1;
    int res;
    if (range.getType() == SelectionType.LINE_WISE) {
      res = moveCaretToLine(editor, endLine);
    }
    else if (range.getType() == SelectionType.CHARACTER_WISE) {
      if (lines > 1) {
        res = moveCaretToLineStart(editor, endLine) + Math.min(EditorHelper.getLineLength(editor, endLine), chars);
      }
      else {
        res = EditorHelper.normalizeOffset(editor, sp.line, start + chars - 1, false);
      }
    }
    else {
      int endColumn = Math.min(EditorHelper.getLineLength(editor, endLine), sp.column + chars - 1);
      res = editor.logicalPositionToOffset(new LogicalPosition(endLine, endColumn));
    }

    return res;
  }

  public void exitVisual(@NotNull final Editor editor) {
    resetVisual(editor, true);
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      CommandState.getInstance(editor).popState();
    }
  }

  public void resetVisual(@NotNull final Editor editor, final boolean removeSelection) {
    final boolean wasVisualBlock = CommandState.inVisualBlockMode(editor);
    final SelectionType selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).getSubMode());
    EditorData.setLastSelectionType(editor, selectionType);
    final TextRange visualMarks = VimPlugin.getMark().getVisualSelectionMarks(editor);
    if (visualMarks != null) {
      EditorData.setLastVisualRange(editor, visualMarks);
    }
    if (removeSelection) {
      if (!EditorData.isKeepingVisualOperatorAction(editor)) {
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
          caret.removeSelection();
        }
      }
      if (wasVisualBlock) {
        editor.getCaretModel().removeSecondaryCarets();
      }
    }
    CommandState.getInstance(editor).setSubMode(CommandState.SubMode.NONE);
  }

  @NotNull
  public VisualChange getVisualOperatorRange(@NotNull Editor editor, @NotNull Caret caret, EnumSet<CommandFlags> cmdFlags) {
    int start = CaretData.getVisualStart(caret);
    int end = CaretData.getVisualEnd(caret);

    if (CommandState.inVisualBlockMode(editor)) {
      start = EditorData.getVisualBlockStart(editor);
      end = EditorData.getVisualBlockEnd(editor);
    }

    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    start = EditorHelper.normalizeOffset(editor, start, false);
    end = EditorHelper.normalizeOffset(editor, end, false);
    LogicalPosition sp = editor.offsetToLogicalPosition(start);
    LogicalPosition ep = editor.offsetToLogicalPosition(end);
    int lines = ep.line - sp.line + 1;
    int chars;
    SelectionType type;
    if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_LINE ||
        cmdFlags.contains(CommandFlags.FLAG_MOT_LINEWISE)) {
      chars = ep.column;
      type = SelectionType.LINE_WISE;
    }
    else if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_CHARACTER) {
      type = SelectionType.CHARACTER_WISE;
      if (lines > 1) {
        chars = ep.column;
      }
      else {
        chars = ep.column - sp.column + 1;
      }
    }
    else {
      chars = ep.column - sp.column + 1;
      if (CaretData.getLastColumn(editor.getCaretModel().getPrimaryCaret()) == MotionGroup.LAST_COLUMN) {
        chars = MotionGroup.LAST_COLUMN;
      }
      type = SelectionType.BLOCK_WISE;
    }

    return new VisualChange(lines, chars, type);
  }

  @NotNull
  public TextRange getVisualRange(@NotNull Editor editor) {
    return new TextRange(editor.getSelectionModel().getBlockSelectionStarts(),
        editor.getSelectionModel().getBlockSelectionEnds());
  }

  @NotNull
  public TextRange getVisualRange(@NotNull Caret caret) {
    return new TextRange(caret.getSelectionStart(), caret.getSelectionEnd());
  }

  @NotNull
  public TextRange getRawVisualRange(@NotNull Caret caret) {
    return new TextRange(CaretData.getVisualStart(caret), CaretData.getVisualEnd(caret));
  }

  public void updateBlockSelection(@NotNull Editor editor) {
    updateBlockSelection(editor, EditorData.getVisualBlockEnd(editor));
  }

  private void updateBlockSelection(@NotNull Editor editor, int offset) {
    EditorData.setVisualBlockEnd(editor, offset);
    EditorData.setVisualBlockOffset(editor, offset);
    int start = EditorData.getVisualBlockStart(editor);
    int end = EditorData.getVisualBlockEnd(editor);

    LogicalPosition blockStart = editor.offsetToLogicalPosition(start);
    LogicalPosition blockEnd = editor.offsetToLogicalPosition(end);
    if (blockStart.column < blockEnd.column) {
      blockEnd = new LogicalPosition(blockEnd.line, blockEnd.column + 1);
    }
    else {
      blockStart = new LogicalPosition(blockStart.line, blockStart.column + 1);
    }
    editor.getSelectionModel().setBlockSelection(blockStart, blockEnd);

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      int line = caret.getLogicalPosition().line;
      int lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true);

      if (CaretData.getLastColumn(editor.getCaretModel().getPrimaryCaret()) >= MotionGroup.LAST_COLUMN) {
        caret.setSelection(caret.getSelectionStart(), lineEndOffset);
      }
      if (!EditorHelper.isLineEmpty(editor, line, false)) {
        caret.moveToOffset(caret.getSelectionEnd() - 1);
      }
    }
    editor.getCaretModel().getPrimaryCaret().moveToOffset(end);

    VimPlugin.getMark().setVisualSelectionMarks(editor, new TextRange(start, end));
  }

  public void updateSelection(@NotNull Editor editor, @NotNull Caret caret, int offset) {
    if (CommandState.getInstance(editor).getSubMode() == CommandState.SubMode.VISUAL_BLOCK) {
      updateBlockSelection(editor, offset);
    }
    else {
      CaretData.setVisualEnd(caret, offset);
      CaretData.setVisualOffset(caret, offset);
      int start = CaretData.getVisualStart(caret);
      int end = offset;
      final CommandState.SubMode subMode = CommandState.getInstance(editor).getSubMode();

      if (subMode == CommandState.SubMode.VISUAL_CHARACTER) {
        if (start > end) {
          int t = start;
          start = end;
          end = t;
        }
        final BoundStringOption opt = (BoundStringOption) Options.getInstance().getOption("selection");
        int lineEnd = EditorHelper.getLineEndForOffset(editor, end);
        final int adj = opt.getValue().equals("exclusive") || end == lineEnd ? 0 : 1;
        final int adjEnd = Math.min(EditorHelper.getFileSize(editor), end + adj);
        caret.setSelection(start, adjEnd);
      }
      else if (subMode == CommandState.SubMode.VISUAL_LINE) {
        if (start > end) {
          int t = start;
          start = end;
          end = t;
        }
        start = EditorHelper.getLineStartForOffset(editor, start);
        end = EditorHelper.getLineEndForOffset(editor, end);
        caret.setSelection(start, end);
      }

      VimPlugin.getMark().setVisualSelectionMarks(editor, new TextRange(start, end));
    }
  }

  public boolean swapVisualBlockEnds(@NotNull Editor editor) {
    if (!CommandState.inVisualBlockMode(editor)) return false;
    int t = EditorData.getVisualBlockEnd(editor);
    EditorData.setVisualBlockEnd(editor, EditorData.getVisualBlockStart(editor));
    EditorData.setVisualBlockStart(editor, t);

    moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), EditorData.getVisualBlockEnd(editor));

    return true;
  }

  public boolean swapVisualEnds(@NotNull Editor editor, @NotNull Caret caret) {
    int t = CaretData.getVisualEnd(caret);
    CaretData.setVisualEnd(caret, CaretData.getVisualStart(caret));
    CaretData.setVisualStart(caret, t);

    moveCaret(editor, caret, CaretData.getVisualEnd(caret));

    return true;
  }

  public void moveVisualStart(@NotNull Caret caret, int startOffset) {
    CaretData.setVisualStart(caret, startOffset);
  }

  public void processEscape(@NotNull Editor editor) {
    exitVisual(editor);
  }

  public static class MotionEditorChange implements FileEditorManagerListener {
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
      if (ExEntryPanel.getInstance().isActive()) {
        ExEntryPanel.getInstance().deactivate(false);
      }
      final FileEditor fileEditor = event.getOldEditor();
      if (fileEditor instanceof TextEditor) {
        final Editor editor = ((TextEditor) fileEditor).getEditor();
        ExOutputModel.getInstance(editor).clear();
        if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
          VimPlugin.getMotion().exitVisual(editor);
        }
      }
    }
  }

  private static class EditorSelectionHandler implements SelectionListener {
    private boolean myMakingChanges = false;

    public void selectionChanged(@NotNull SelectionEvent selectionEvent) {
      final Editor editor = selectionEvent.getEditor();
      final Document document = editor.getDocument();
      if (myMakingChanges || (document instanceof DocumentEx && ((DocumentEx) document).isInEventsHandling())) {
        return;
      }

      myMakingChanges = true;
      try {
        final com.intellij.openapi.util.TextRange newRange = selectionEvent.getNewRange();
        for (Editor e : EditorFactory.getInstance().getEditors(document)) {
          if (!e.equals(editor)) {
            e.getSelectionModel().setSelection(newRange.getStartOffset(), newRange.getEndOffset());
          }
        }
      } finally {
        myMakingChanges = false;
      }
    }
  }

  private static class EditorMouseHandler implements EditorMouseListener, EditorMouseMotionListener {
    public void mouseMoved(@NotNull EditorMouseEvent event) {
    }

    public void mouseDragged(@NotNull EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getArea() == EditorMouseEventArea.EDITING_AREA ||
          event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA) {
        if (dragEditor == null) {
          if (event.getArea() == EditorMouseEventArea.EDITING_AREA) {
            mode = CommandState.SubMode.VISUAL_CHARACTER;
          }
          else if (event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA) {
            mode = CommandState.SubMode.VISUAL_LINE;
          }
          startOff = event.getEditor().getSelectionModel().getSelectionStart();
          endOff = event.getEditor().getSelectionModel().getSelectionEnd();
        }

        dragEditor = event.getEditor();
      }
    }

    public void mousePressed(@NotNull EditorMouseEvent event) {
    }

    public void mouseClicked(@NotNull EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getArea() == EditorMouseEventArea.EDITING_AREA) {
        VimPlugin.getMotion().processMouseClick(event.getEditor(), event.getMouseEvent());
      }
      else if (event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA &&
          event.getArea() != EditorMouseEventArea.FOLDING_OUTLINE_AREA) {
        VimPlugin.getMotion()
            .processLineSelection(event.getEditor(), event.getMouseEvent().getButton() == MouseEvent.BUTTON3);
      }
    }

    public void mouseReleased(@NotNull EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getEditor().equals(dragEditor)) {
        VimPlugin.getMotion().processMouseReleased(event.getEditor(), mode, startOff, endOff);

        dragEditor = null;
      }
    }

    public void mouseEntered(@NotNull EditorMouseEvent event) {
    }

    public void mouseExited(@NotNull EditorMouseEvent event) {
    }

    @Nullable
    private Editor dragEditor = null;
    @NotNull
    private CommandState.SubMode mode = CommandState.SubMode.NONE;
    private int startOff;
    private int endOff;
  }

  private enum ScreenLocation {
    TOP,
    MIDDLE,
    BOTTOM
  }

  public int getLastFTCmd() {
    return lastFTCmd;
  }

  public char getLastFTChar() {
    return lastFTChar;
  }

  private int lastFTCmd = 0;
  private char lastFTChar;
  @NotNull
  private final EditorMouseHandler mouseHandler = new EditorMouseHandler();
  @NotNull
  private final EditorSelectionHandler selectionHandler = new EditorSelectionHandler();
}
