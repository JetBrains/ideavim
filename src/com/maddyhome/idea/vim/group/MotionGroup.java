package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2008 Rick Maddy
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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.action.motion.TextObjectAction;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.Jump;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.ApiHelper;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.option.BoundStringOption;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import com.maddyhome.idea.vim.ui.MorePanel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * This handles all motion related commands and marks
 */
public class MotionGroup extends AbstractActionGroup {
  public static final int LAST_F = 1;
  public static final int LAST_f = 2;
  public static final int LAST_T = 3;
  public static final int LAST_t = 4;
  public static final int LAST_COLUMN = 9999;

  /**
   * Create the group
   */
  public MotionGroup() {
    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        // This ridiculous code ensures that a lot of events are processed BEFORE we finally start listening
        // to visible area changes. The primary reason for this change is to fix the cursor position bug
        // using the gd and gD commands (Goto Declaration). This bug has been around since Idea 6.0.4?
        // Prior to this change the visible area code was moving the cursor around during file load and messing
        // with the cursor position of the Goto Declaration processing.
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                  public void run() {
                    addEditorListener(editor);
                    EditorData.setMotionGroup(editor, true);
                  }
                });
              }
            });
          }
        });
      }

      public void editorReleased(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (EditorData.getMotionGroup(editor)) {
          removeEditorListener(editor);
          EditorData.setMotionGroup(editor, false);
        }
      }
    });
  }

  public void turnOn() {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      addEditorListener(editor);
    }
  }

  public void turnOff() {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      removeEditorListener(editor);
    }
  }

  private void addEditorListener(Editor editor) {
    editor.addEditorMouseListener(mouseHandler);
    editor.addEditorMouseMotionListener(mouseHandler);

    editor.getSelectionModel().addSelectionListener(selectionHandler);
    editor.getScrollingModel().addVisibleAreaListener(scrollHandler);
  }

  private void removeEditorListener(Editor editor) {
    editor.removeEditorMouseListener(mouseHandler);
    editor.removeEditorMouseMotionListener(mouseHandler);

    editor.getSelectionModel().removeSelectionListener(selectionHandler);
    editor.getScrollingModel().removeVisibleAreaListener(scrollHandler);
  }

  /**
   * Process mouse clicks by setting/resetting visual mode. There are some strange scenerios to handle.
   *
   * @param editor The editor
   * @param event  The mouse event
   */
  private void processMouseClick(Editor editor, MouseEvent event) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    if (MorePanel.getInstance().isActive()) {
      MorePanel.getInstance().deactivate(false);
    }

    int visualMode = 0;
    switch (event.getClickCount() % 3) {
      case 1: // Single click or quad click
        visualMode = 0;
        break;
      case 2: // Double click
        visualMode = Command.FLAG_MOT_CHARACTERWISE;
        break;
      case 0: // Triple click
        visualMode = Command.FLAG_MOT_LINEWISE;
        // Pop state of being in Visual Char mode
        if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
          CommandState.getInstance(editor).popState();
        }

        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        editor.getSelectionModel().setSelection(start, end - 1);

        break;
    }

    setVisualMode(editor, null, visualMode);

    switch (CommandState.getInstance(editor).getSubMode()) {
      case 0:
        VisualPosition vp = editor.getCaretModel().getVisualPosition();
        int col = EditorHelper.normalizeVisualColumn(editor, vp.line, vp.column,
                                                     CommandState.getInstance(editor).getMode() == CommandState.MODE_INSERT ||
                                                     CommandState.getInstance(editor).getMode() == CommandState.MODE_REPLACE);
        if (col != vp.column) {
          editor.getCaretModel().moveToVisualPosition(new VisualPosition(vp.line, col));
        }
        MotionGroup.scrollCaretIntoView(editor);
        break;
      case Command.FLAG_MOT_CHARACTERWISE:
        /*
        BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
        int adj = 1;
        if (opt.getValue().equals("exclusive"))
        {
            adj = 0;
        }
        */
        editor.getCaretModel().moveToOffset(visualEnd);
        break;
      case Command.FLAG_MOT_LINEWISE:
        editor.getCaretModel().moveToLogicalPosition(editor.xyToLogicalPosition(event.getPoint()));
        break;
    }

    visualOffset = editor.getCaretModel().getOffset();

    EditorData.setLastColumn(editor, EditorHelper.getCurrentVisualColumn(editor));
    if (logger.isDebugEnabled()) {
      logger.debug("Mouse click: vp=" + editor.getCaretModel().getVisualPosition() +
                   "lp=" + editor.getCaretModel().getLogicalPosition() +
                   "offset=" + editor.getCaretModel().getOffset());
    }
  }

  /**
   * Handles mouse drags by properly setting up visual mode based on the new selection.
   *
   * @param editor The editor the mouse drag occured in.
   * @param update True if update, false if not.
   */
  private void processLineSelection(Editor editor, boolean update) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    if (MorePanel.getInstance().isActive()) {
      MorePanel.getInstance().deactivate(false);
    }

    if (update) {
      if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
        updateSelection(editor, null, editor.getCaretModel().getOffset());
      }
    }
    else {
      if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
        CommandState.getInstance(editor).popState();
      }

      int start = editor.getSelectionModel().getSelectionStart();
      int end = editor.getSelectionModel().getSelectionEnd();
      if (logger.isDebugEnabled()) {
        logger.debug("start=" + start);
        logger.debug("end=" + end);
      }
      editor.getSelectionModel().setSelection(start, end - 1);

      setVisualMode(editor, null, Command.FLAG_MOT_LINEWISE);

      VisualChange range = getVisualOperatorRange(editor, Command.FLAG_MOT_LINEWISE);
      if (logger.isDebugEnabled()) logger.debug("range=" + range);
      if (range.getLines() > 1) {
        MotionGroup.moveCaret(editor, null, moveCaretVertical(editor, -1));
      }
    }
  }

  private void processMouseReleased(Editor editor, int mode, int startOff, int endOff) {
    if (ExEntryPanel.getInstance().isActive()) {
      ExEntryPanel.getInstance().deactivate(false);
    }

    if (MorePanel.getInstance().isActive()) {
      MorePanel.getInstance().deactivate(false);
    }

    logger.debug("mouse released");
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
      CommandState.getInstance(editor).popState();
    }

    int start = editor.getSelectionModel().getSelectionStart();
    int end = editor.getSelectionModel().getSelectionEnd();
    if (logger.isDebugEnabled()) {
      logger.debug("startOff=" + startOff);
      logger.debug("endOff=" + endOff);
      logger.debug("start=" + start);
      logger.debug("end=" + end);
    }

    if (mode == Command.FLAG_MOT_LINEWISE) {
      end--;
      endOff--;
    }

    if (end == startOff || end == endOff) {
      int t = start;
      start = end;
      end = t;

      if (mode == Command.FLAG_MOT_CHARACTERWISE) {
        start--;
      }
    }

    MotionGroup.moveCaret(editor, null, start);
    toggleVisual(editor, null, 1, 0, mode);
    MotionGroup.moveCaret(editor, null, end);
    KeyHandler.getInstance().reset(editor);
  }

  public static int moveCaretToMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument) {
    Command cmd = argument.getMotion();
    // Normalize the counts between the command and the motion argument
    int cnt = cmd.getCount() * count;
    int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
    MotionEditorAction action = (MotionEditorAction)cmd.getAction();

    // Execute the motion (without moving the cursor) and get where we end
    int offset = action.getOffset(editor, context, cnt, raw, cmd.getArgument());

    moveCaret(editor, context, offset);

    return offset;
  }

  public TextRange getWordRange(Editor editor, DataContext context, int count, boolean isOuter, boolean isBig) {
    int dir = 1;
    boolean selection = false;
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
      if (visualEnd < visualStart) {
        dir = -1;
      }
      if (visualStart != visualEnd) {
        selection = true;
      }
    }

    return SearchHelper.findWordUnderCursor(editor, count, dir, isOuter, isBig, selection);
  }

  public TextRange getBlockRange(Editor editor, DataContext context, int count, boolean isOuter, char type) {
    return SearchHelper.findBlockRange(editor, type, count, isOuter);
  }

  public TextRange getSentenceRange(Editor editor, DataContext context, int count, boolean isOuter) {
    return SearchHelper.findSentenceRange(editor, count, isOuter);
  }

  public TextRange getParagraphRange(Editor editor, DataContext context, int count, boolean isOuter) {
    return SearchHelper.findParagraphRange(editor, count, isOuter);
  }

  /**
   * This helper method calculates the complete range a motion will move over taking into account whether
   * the motion is FLAG_MOT_LINEWISE or FLAG_MOT_CHARACTERWISE (FLAG_MOT_INCLUSIVE or FLAG_MOT_EXCLUSIVE).
   *
   * @param editor     The editor the motion takes place in
   * @param context    The data context
   * @param count      The count applied to the motion
   * @param rawCount   The actual count entered by the user
   * @param argument   Any argument needed by the motion
   * @param incNewline True if to include newline
   * @param moveCursor True if cursor should be moved just as if motion command were executed by user, false if not
   * @return The motion's range
   */
  public static TextRange getMotionRange(Editor editor, DataContext context, int count, int rawCount,
                                         Argument argument, boolean incNewline, boolean moveCursor) {
    Command cmd = argument.getMotion();
    // Normalize the counts between the command and the motion argument
    int cnt = cmd.getCount() * count;
    int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
    int start = 0;
    int end = 0;
    if (cmd.getAction() instanceof MotionEditorAction) {
      MotionEditorAction action = (MotionEditorAction)cmd.getAction();

      // This is where we are now
      start = editor.getCaretModel().getOffset();

      // Execute the motion (without moving the cursor) and get where we end
      end = action.getOffset(editor, context, cnt, raw, cmd.getArgument());

      // Invalid motion
      if (end == -1) {
        return null;
      }

      if (moveCursor) {
        moveCaret(editor, context, end);
      }
    }
    else if (cmd.getAction() instanceof TextObjectAction) {
      TextObjectAction action = (TextObjectAction)cmd.getAction();

      TextRange range = action.getRange(editor, context, cnt, raw, cmd.getArgument());

      if (range == null) {
        return null;
      }

      start = range.getStartOffset();
      end = range.getEndOffset();

      if (moveCursor) {
        moveCaret(editor, context, start);
      }
    }

    // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
    // of the line and move the end to the end of the line.
    int flags = cmd.getFlags();
    if ((flags & Command.FLAG_MOT_LINEWISE) != 0) {
      if (start > end) {
        int t = start;
        start = end;
        end = t;
      }

      start = EditorHelper.getLineStartForOffset(editor, start);
      end = Math.min(EditorHelper.getLineEndForOffset(editor, end) + (incNewline ? 1 : 0),
                     EditorHelper.getFileSize(editor));
    }
    // If characterwise and inclusive, add the last character to the range
    else if ((flags & Command.FLAG_MOT_INCLUSIVE) != 0) {
      end = end + (start <= end ? 1 : -1);
    }

    // Normalize the range
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    return new TextRange(start, end);
  }

  public int moveCaretToNthCharacter(Editor editor, int count) {
    return Math.max(0, Math.min(count, EditorHelper.getFileSize(editor) - 1));
  }

  public int moveCaretToMarkLine(Editor editor, DataContext context, char ch) {
    Mark mark = CommandGroups.getInstance().getMark().getMark(editor, ch);
    if (mark != null) {
      VirtualFile vf = EditorData.getVirtualFile(editor);
      if (vf == null) return -1;

      int line = mark.getLogicalLine();
      if (!mark.getFilename().equals(vf.getPath())) {
        editor = selectEditor(editor, EditorData.getVirtualFile(editor));
        if (editor != null) {
          moveCaret(editor, context, moveCaretToLineStartSkipLeading(editor, line));
        }

        return -2;
      }
      else {
        return moveCaretToLineStartSkipLeading(editor, line);
      }
    }
    else {
      return -1;
    }
  }

  public int moveCaretToFileMarkLine(Editor editor, DataContext context, char ch) {
    Mark mark = CommandGroups.getInstance().getMark().getFileMark(editor, ch);
    if (mark != null) {
      int line = mark.getLogicalLine();
      return moveCaretToLineStartSkipLeading(editor, line);
    }
    else {
      return -1;
    }
  }

  public int moveCaretToMark(Editor editor, DataContext context, char ch) {
    Mark mark = CommandGroups.getInstance().getMark().getMark(editor, ch);
    if (mark != null) {
      VirtualFile vf = EditorData.getVirtualFile(editor);
      if (vf == null) return -1;

      LogicalPosition lp = new LogicalPosition(mark.getLogicalLine(), mark.getCol());
      if (!vf.getPath().equals(mark.getFilename())) {
        editor = selectEditor(editor, EditorData.getVirtualFile(editor));
        if (editor != null) {
          moveCaret(editor, context, editor.logicalPositionToOffset(lp));
        }

        return -2;
      }
      else {
        return editor.logicalPositionToOffset(lp);
      }
    }
    else {
      return -1;
    }
  }

  public int moveCaretToJump(Editor editor, DataContext context, int count) {
    int spot = CommandGroups.getInstance().getMark().getJumpSpot();
    Jump jump = CommandGroups.getInstance().getMark().getJump(count);
    if (jump != null) {
      VirtualFile vf = EditorData.getVirtualFile(editor);
      if (vf == null) return -1;

      LogicalPosition lp = new LogicalPosition(jump.getLogicalLine(), jump.getCol());
      if (!vf.getPath().equals(jump.getFilename())) {
        VirtualFile newFile = LocalFileSystem.getInstance().findFileByPath(jump.getFilename().replace(File.separatorChar, '/'));
        if (newFile == null) return -2;

        Editor newEditor = selectEditor(editor, newFile);
        if (newEditor != null) {
          if (spot == -1) {
            CommandGroups.getInstance().getMark().addJump(editor, context, false);
          }
          moveCaret(newEditor, context, EditorHelper.normalizeOffset(newEditor, newEditor.logicalPositionToOffset(lp), false));
        }

        return -2;
      }
      else {
        if (spot == -1) {
          CommandGroups.getInstance().getMark().addJump(editor, context, false);
        }

        return editor.logicalPositionToOffset(lp);
      }
    }
    else {
      return -1;
    }
  }

  public int moveCaretToFileMark(Editor editor, DataContext context, char ch) {
    Mark mark = CommandGroups.getInstance().getMark().getFileMark(editor, ch);
    if (mark != null) {
      LogicalPosition lp = new LogicalPosition(mark.getLogicalLine(), mark.getCol());

      return editor.logicalPositionToOffset(lp);
    }
    else {
      return -1;
    }
  }

  private Editor selectEditor(Editor editor, VirtualFile file) {
    Project proj = EditorData.getProject(editor);

    return CommandGroups.getInstance().getFile().selectEditor(proj, file);
  }

  public int moveCaretToMatchingPair(Editor editor, DataContext context) {
    int pos = SearchHelper.findMatchingPairOnCurrentLine(editor);
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
   * @param count  The number of words to skip
   * @return position
   */
  public int moveCaretToNextCamel(Editor editor, int count) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      return SearchHelper.findNextCamelStart(editor, count);
    }
  }

  /**
   * This moves the caret to the start of the next/previous camel word.
   *
   * @param editor The editor to move in
   * @param count  The number of words to skip
   * @return position
   */
  public int moveCaretToNextCamelEnd(Editor editor, int count) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      return SearchHelper.findNextCamelEnd(editor, count);
    }
  }

  /**
   * This moves the caret to the start of the next/previous word/WORD.
   *
   * @param editor   The editor to move in
   * @param count    The number of words to skip
   * @param skipPunc If true then find WORD, if false then find word
   * @return position
   */
  public int moveCaretToNextWord(Editor editor, int count, boolean skipPunc) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      return SearchHelper.findNextWord(editor, count, skipPunc);
    }
  }

  /**
   * This moves the caret to the end of the next/previous word/WORD.
   *
   * @param editor   The editor to move in
   * @param count    The number of words to skip
   * @param skipPunc If true then find WORD, if false then find word
   * @return position
   */
  public int moveCaretToNextWordEnd(Editor editor, int count, boolean skipPunc) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }

    // If we are doing this move as part of a change command (e.q. cw), we need to count the current end of
    // word if the cursor happens to be on the end of a word already. If this is a normal move, we don't count
    // the current word.
    boolean stay = CommandState.getInstance(editor).getCommand().getType() == Command.CHANGE;
    int pos = SearchHelper.findNextWordEnd(editor, count, skipPunc, stay);
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
   * @param count  The number of paragraphs to skip
   * @return position
   */
  public int moveCaretToNextParagraph(Editor editor, int count) {
    int res = SearchHelper.findNextParagraph(editor, count, false);
    if (res >= 0) {
      res = EditorHelper.normalizeOffset(editor, res, false);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToNextSentenceStart(Editor editor, int count) {
    int res = SearchHelper.findNextSentenceStart(editor, count, false, true);
    if (res >= 0) {
      res = EditorHelper.normalizeOffset(editor, res, false);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToNextSentenceEnd(Editor editor, int count) {
    int res = SearchHelper.findNextSentenceEnd(editor, count, false, true);
    if (res >= 0) {
      res = EditorHelper.normalizeOffset(editor, res, false);
    }
    else {
      res = -1;
    }

    return res;
  }

  public int moveCaretToUnmatchedBlock(Editor editor, int count, char type) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findUnmatchedBlock(editor, type, count);
      if (res != -1) {
        res = EditorHelper.normalizeOffset(editor, res, false);
      }

      return res;
    }
  }

  public int moveCaretToSection(Editor editor, char type, int dir, int count) {
    if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
        (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int res = SearchHelper.findSection(editor, type, dir, count);
      if (res != -1) {
        res = EditorHelper.normalizeOffset(editor, res, false);
      }

      return res;
    }
  }

  public int moveCaretToMethodStart(Editor editor, int count) {
    return SearchHelper.findMethodStart(editor, count);
  }

  public int moveCaretToMethodEnd(Editor editor, int count) {
    return SearchHelper.findMethodEnd(editor, count);
  }

  public void setLastFTCmd(int lastFTCmd, char lastChar) {
    this.lastFTCmd = lastFTCmd;
    this.lastFTChar = lastChar;
  }

  public int repeatLastMatchChar(Editor editor, int count) {
    int res = -1;
    switch (lastFTCmd) {
      case LAST_F:
        res = moveCaretToNextCharacterOnLine(editor, -count, lastFTChar);
        break;
      case LAST_f:
        res = moveCaretToNextCharacterOnLine(editor, count, lastFTChar);
        break;
      case LAST_T:
        res = moveCaretToBeforeNextCharacterOnLine(editor, -count, lastFTChar);
        break;
      case LAST_t:
        res = moveCaretToBeforeNextCharacterOnLine(editor, count, lastFTChar);
        break;
    }

    return res;
  }

  /**
   * This moves the caret to the next/previous matching character on the current line
   *
   * @param count  The number of occurences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  public int moveCaretToNextCharacterOnLine(Editor editor, int count, char ch) {
    int pos = SearchHelper.findNextCharacterOnLine(editor, count, ch);

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
   * @param count  The number of occurences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  public int moveCaretToBeforeNextCharacterOnLine(Editor editor, int count, char ch) {
    int pos = SearchHelper.findNextCharacterOnLine(editor, count, ch);

    if (pos >= 0) {
      int step = count >= 0 ? 1 : -1;
      return pos - step;
    }
    else {
      return -1;
    }
  }

  public boolean scrollLineToFirstScreenLine(Editor editor, DataContext context, int rawCount, int count,
                                             boolean start) {
    scrollLineToScreenLine(editor, context, 1, rawCount, count, start);

    return true;
  }

  public boolean scrollLineToMiddleScreenLine(Editor editor, DataContext context, int rawCount, int count,
                                              boolean start) {
    scrollLineToScreenLine(editor, context, EditorHelper.getScreenHeight(editor) / 2 + 1, rawCount, count, start);

    return true;
  }

  public boolean scrollLineToLastScreenLine(Editor editor, DataContext context, int rawCount, int count,
                                            boolean start) {
    scrollLineToScreenLine(editor, context, EditorHelper.getScreenHeight(editor), rawCount, count, start);

    return true;
  }

  public boolean scrollColumnToFirstScreenColumn(Editor editor, DataContext context) {
    scrollColumnToScreenColumn(editor, 0);

    return true;
  }

  public boolean scrollColumnToLastScreenColumn(Editor editor, DataContext context) {
    scrollColumnToScreenColumn(editor, EditorHelper.getScreenWidth(editor));

    return true;
  }

  private void scrollColumnToScreenColumn(Editor editor, int scol) {
    int scrolloff = ((NumberOption)Options.getInstance().getOption("sidescrolloff")).value();
    int width = EditorHelper.getScreenWidth(editor);
    if (scrolloff > width / 2) {
      scrolloff = width / 2;
    }
    if (scol <= width / 2) {
      if (scol < scrolloff + 1) {
        scol = scrolloff + 1;
      }
    }
    else {
      if (scol > width - scrolloff) {
        scol = width - scrolloff;
      }
    }

    int vcol = EditorHelper.getCurrentVisualColumn(editor);
    scrollColumnToLeftOfScreen(editor, EditorHelper.normalizeVisualColumn(editor,
                                                                          EditorHelper.getCurrentVisualLine(editor), vcol - scol + 1,
                                                                          false));
  }

  private void scrollLineToScreenLine(Editor editor, DataContext context, int sline, int rawCount, int count,
                                      boolean start) {
    int scrolloff = ((NumberOption)Options.getInstance().getOption("scrolloff")).value();
    int height = EditorHelper.getScreenHeight(editor);
    if (scrolloff > height / 2) {
      scrolloff = height / 2;
    }
    if (sline <= height / 2) {
      if (sline < scrolloff + 1) {
        sline = scrolloff + 1;
      }
    }
    else {
      if (sline > height - scrolloff) {
        sline = height - scrolloff;
      }
    }

    int vline = rawCount == 0 ?
                EditorHelper.getCurrentVisualLine(editor) : EditorHelper.logicalLineToVisualLine(editor, count - 1);
    scrollLineToTopOfScreen(editor, EditorHelper.normalizeVisualLine(editor, vline - sline + 1));
    if (vline != EditorHelper.getCurrentVisualLine(editor) || start) {
      int offset;
      if (start) {
        offset = moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, vline));
      }
      else {
        offset = moveCaretVertical(editor,
                                   EditorHelper.visualLineToLogicalLine(editor, vline) - EditorHelper.getCurrentLogicalLine(editor));
      }

      moveCaret(editor, context, offset);
    }
  }

  public int moveCaretToFirstScreenLine(Editor editor, DataContext context, int count) {
    return moveCaretToScreenLine(editor, count);
  }

  public int moveCaretToLastScreenLine(Editor editor, DataContext context, int count) {
    return moveCaretToScreenLine(editor, EditorHelper.getScreenHeight(editor) - count + 1);
  }

  public int moveCaretToLastScreenLineEnd(Editor editor, DataContext context, int count) {
    int offset = moveCaretToLastScreenLine(editor, context, count);
    LogicalPosition lline = editor.offsetToLogicalPosition(offset);

    return moveCaretToLineEnd(editor, lline.line, false);
  }

  public int moveCaretToMiddleScreenLine(Editor editor, DataContext context) {
    return moveCaretToScreenLine(editor, EditorHelper.getScreenHeight(editor) / 2 + 1);
  }

  private int moveCaretToScreenLine(Editor editor, int line) {
    //saveJumpLocation(editor, context);
    int scrolloff = ((NumberOption)Options.getInstance().getOption("scrolloff")).value();
    int height = EditorHelper.getScreenHeight(editor);
    if (scrolloff > height / 2) {
      scrolloff = height / 2;
    }

    int top = EditorHelper.getVisualLineAtTopOfScreen(editor);

    if (line > height - scrolloff && top < EditorHelper.getLineCount(editor) - height) {
      line = height - scrolloff;
    }
    else if (line <= scrolloff && top > 0) {
      line = scrolloff + 1;
    }

    return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, top + line - 1));
  }

  public boolean scrollHalfPage(Editor editor, DataContext context, int dir, int count) {
    NumberOption scroll = (NumberOption)Options.getInstance().getOption("scroll");
    int height = EditorHelper.getScreenHeight(editor) / 2;
    if (count == 0) {
      count = scroll.value();
      if (count == 0) {
        count = height;
      }
    }
    else {
      scroll.set(count);
    }

    return scrollPage(editor, context, dir, count, EditorHelper.getCurrentVisualScreenLine(editor), true);
  }

  public boolean scrollColumn(Editor editor, DataContext context, int columns) {
    int vcol = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    vcol = EditorHelper.normalizeVisualColumn(editor, EditorHelper.getCurrentVisualLine(editor), vcol + columns,
                                              false);

    scrollColumnToLeftOfScreen(editor, vcol);

    moveCaretToView(editor, context);

    return true;
  }

  public boolean scrollLine(Editor editor, DataContext context, int lines) {
    if (logger.isDebugEnabled()) logger.debug("lines=" + lines);
    int vline = EditorHelper.getVisualLineAtTopOfScreen(editor);

    vline = EditorHelper.normalizeVisualLine(editor, vline + lines);
    scrollLineToTopOfScreen(editor, vline);

    moveCaretToView(editor, context);

    return true;
  }

  public static void moveCaretToView(Editor editor, DataContext context) {
    if (logger.isDebugEnabled()) logger.debug("editor=" + editor);
    int scrolloff = ((NumberOption)Options.getInstance().getOption("scrolloff")).value();
    int sidescrolloff = ((NumberOption)Options.getInstance().getOption("sidescrolloff")).value();
    int height = EditorHelper.getScreenHeight(editor);
    int width = EditorHelper.getScreenWidth(editor);
    if (scrolloff > height / 2) {
      scrolloff = height / 2;
    }
    if (sidescrolloff > width / 2) {
      sidescrolloff = width / 2;
    }

    int vline = EditorHelper.getVisualLineAtTopOfScreen(editor);
    int cline = EditorHelper.getCurrentVisualLine(editor);
    int newline = cline;
    if (cline < vline + scrolloff) {
      newline = EditorHelper.normalizeVisualLine(editor, vline + scrolloff);
    }
    else if (cline >= vline + height - scrolloff) {
      newline = EditorHelper.normalizeVisualLine(editor, vline + height - scrolloff - 1);
    }
    if (logger.isDebugEnabled()) logger.debug("vline=" + vline + ", cline=" + cline + ", newline=" + newline);

    int col = EditorHelper.getCurrentVisualColumn(editor);
    int ocol = col;
    if (col >= EditorHelper.getLineLength(editor) - 1) {
      col = EditorData.getLastColumn(editor);
    }
    int vcol = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    int ccol = col;
    int newcol = ccol;
    if (ccol < vcol + sidescrolloff) {
      newcol = vcol + sidescrolloff;
    }
    else if (ccol >= vcol + width - sidescrolloff) {
      newcol = vcol + width - sidescrolloff - 1;
    }
    if (logger.isDebugEnabled()) logger.debug("col=" + col + ", vcol=" + vcol + ", ccol=" + ccol + ", newcol=" + newcol);

    if (newline == cline && newcol != ccol) {
      col = newcol;
    }

    newcol = EditorHelper.normalizeVisualColumn(editor, newline, newcol, CommandState.inInsertMode(editor));

    if (newline != cline || newcol != ocol) {
      int offset = EditorHelper.visualPostionToOffset(editor, new VisualPosition(newline, newcol));
      moveCaret(editor, context, offset);

      EditorData.setLastColumn(editor, col);
    }
  }

  public boolean scrollFullPage(Editor editor, DataContext context, int pages) {
    int height = EditorHelper.getScreenHeight(editor);
    int line = pages > 0 ? 1 : height;

    return scrollPage(editor, context, pages, height - 2, line, false);
  }

  public boolean scrollPage(Editor editor, DataContext context, int pages, int height, int line, boolean partial) {
    if (logger.isDebugEnabled()) logger.debug("scrollPage(" + pages + ")");
    int tline = EditorHelper.getVisualLineAtTopOfScreen(editor);
    /*
    if ((tline == 0 && pages < 0) || (tline == EditorHelper.getVisualLineCount(editor) - 1 && pages > 0))
    {
        return false;
    }
    */

    int newline = tline + pages * height;
    int topline = EditorHelper.normalizeVisualLine(editor, newline);

    boolean moved = scrollLineToTopOfScreen(editor, topline);
    tline = EditorHelper.getVisualLineAtTopOfScreen(editor);

    if (moved && topline == newline && topline == tline) {
      moveCaret(editor, context, moveCaretToScreenLine(editor, line));

      return true;
    }
    else if (moved && !partial) {
      int vline = Math.abs(tline - newline) % height + 1;
      if (pages < 0) {
        vline = height - vline + 3;
      }
      moveCaret(editor, context, moveCaretToScreenLine(editor, vline));

      return true;
    }
    else if (partial) {
      int cline = EditorHelper.getCurrentVisualLine(editor);
      int vline = cline + pages * height;
      vline = EditorHelper.normalizeVisualLine(editor, vline);
      if (cline == vline) {
        return false;
      }

      int lline = editor.visualToLogicalPosition(new VisualPosition(vline, 0)).line;
      moveCaret(editor, context, moveCaretToLineStartSkipLeading(editor, lline));

      return true;
    }
    else {
      moveCaret(editor, context, moveCaretToLineStartSkipLeading(editor));
      return false;
    }
  }

  private static boolean scrollLineToTopOfScreen(Editor editor, int vline) {
    EditorScrollHandler.ignoreChanges(true);
    int pos = vline * editor.getLineHeight();
    int vpos = editor.getScrollingModel().getVerticalScrollOffset();
    editor.getScrollingModel().scrollVertically(pos);
    EditorScrollHandler.ignoreChanges(false);

    return vpos != editor.getScrollingModel().getVerticalScrollOffset();
  }

  private static void scrollColumnToLeftOfScreen(Editor editor, int vcol) {
    EditorScrollHandler.ignoreChanges(true);
    editor.getScrollingModel().scrollHorizontally(vcol * EditorHelper.getColumnWidth(editor));
    EditorScrollHandler.ignoreChanges(false);
  }

  public int moveCaretToMiddleColumn(Editor editor) {
    int width = EditorHelper.getScreenWidth(editor) / 2;
    int len = EditorHelper.getLineLength(editor);

    return moveCaretToColumn(editor, Math.max(0, Math.min(len - 1, width)), false);
  }

  public int moveCaretToColumn(Editor editor, int count, boolean allowEnd) {
    int line = EditorHelper.getCurrentLogicalLine(editor);
    int pos = EditorHelper.normalizeColumn(editor, line, count, allowEnd);

    return editor.logicalPositionToOffset(new LogicalPosition(line, pos));
  }

  public int moveCaretToLineStartSkipLeading(Editor editor) {
    int lline = EditorHelper.getCurrentLogicalLine(editor);
    return moveCaretToLineStartSkipLeading(editor, lline);
  }

  public int moveCaretToLineStartSkipLeadingOffset(Editor editor, int offset) {
    int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
    return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, line));
  }

  public int moveCaretToLineStartSkipLeading(Editor editor, int lline) {
    return EditorHelper.getLeadingCharacterOffset(editor, lline);
  }

  public int moveCaretToLineEndSkipLeadingOffset(Editor editor, int offset) {
    int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
    return moveCaretToLineEndSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, line));
  }

  public int moveCaretToLineEndSkipLeading(Editor editor, int lline) {
    int start = EditorHelper.getLineStartOffset(editor, lline);
    int end = EditorHelper.getLineEndOffset(editor, lline, true);
    CharSequence chars = EditorHelper.getDocumentChars(editor);
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

  public int moveCaretToLineEnd(Editor editor, boolean allowPastEnd) {
    return moveCaretToLineEnd(editor, EditorHelper.getCurrentLogicalLine(editor), allowPastEnd);
  }

  public int moveCaretToLineEnd(Editor editor, int lline, boolean allowPastEnd) {

    return EditorHelper.normalizeOffset(editor, lline, EditorHelper.getLineEndOffset(editor, lline, allowPastEnd),
                                        allowPastEnd);
  }

  public int moveCaretToLineEndOffset(Editor editor, int cntForward, boolean allowPastEnd) {
    int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + cntForward);

    if (line < 0) {
      return 0;
    }
    else {
      return moveCaretToLineEnd(editor, EditorHelper.visualLineToLogicalLine(editor, line), allowPastEnd);
    }
  }

  public int moveCaretToLineStart(Editor editor) {
    int lline = EditorHelper.getCurrentLogicalLine(editor);
    return moveCaretToLineStart(editor, lline);
  }

  public int moveCaretToLineStart(Editor editor, int lline) {
    if (lline >= EditorHelper.getLineCount(editor)) {
      return EditorHelper.getFileSize(editor);
    }

    return EditorHelper.getLineStartOffset(editor, lline);
  }

  public int moveCaretToLineStartOffset(Editor editor, int offset) {
    int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
    return moveCaretToLineStart(editor, EditorHelper.visualLineToLogicalLine(editor, line));
  }

  public int moveCaretToLineScreenStart(Editor editor) {
    int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    return moveCaretToColumn(editor, col, false);
  }

  public int moveCaretToLineScreenStartSkipLeading(Editor editor) {
    int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    int lline = EditorHelper.getCurrentLogicalLine(editor);
    return EditorHelper.getLeadingCharacterOffset(editor, lline, col);
  }

  public int moveCaretToLineScreenEnd(Editor editor, boolean allowEnd) {
    int col = EditorHelper.getVisualColumnAtLeftOfScreen(editor) + EditorHelper.getScreenWidth(editor) - 1;
    return moveCaretToColumn(editor, col, allowEnd);
  }

  public int moveCaretHorizontalWrap(Editor editor, int count) {
    // FIX - allows cursor over newlines
    int oldoffset = editor.getCaretModel().getOffset();
    int offset = Math.min(Math.max(0, editor.getCaretModel().getOffset() + count), EditorHelper.getFileSize(editor));
    if (offset == oldoffset) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public int moveCaretHorizontal(Editor editor, int count, boolean allowPastEnd) {
    int oldoffset = editor.getCaretModel().getOffset();
    int offset = EditorHelper.normalizeOffset(editor, EditorHelper.getCurrentLogicalLine(editor), oldoffset + count,
                                              allowPastEnd);
    if (offset == oldoffset) {
      return -1;
    }
    else {
      return offset;
    }
  }

  public int moveCaretVertical(Editor editor, int count) {
    VisualPosition pos = editor.getCaretModel().getVisualPosition();
    if ((pos.line == 0 && count < 0) || (pos.line >= EditorHelper.getVisualLineCount(editor) - 1 && count > 0)) {
      return -1;
    }
    else {
      int col = EditorData.getLastColumn(editor);
      int line = EditorHelper.normalizeVisualLine(editor, pos.line + count);
      VisualPosition newPos = new VisualPosition(line, EditorHelper.normalizeVisualColumn(editor, line, col, CommandState.inInsertMode(editor)));

      return EditorHelper.visualPostionToOffset(editor, newPos);
    }
  }

  public int moveCaretToLine(Editor editor, DataContext context, int lline) {
    int col = EditorData.getLastColumn(editor);
    int line = lline;
    if (lline < 0) {
      line = 0;
      col = 0;
    }
    else if (lline >= EditorHelper.getLineCount(editor)) {
      line = EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1);
      col = EditorHelper.getLineLength(editor, line);
    }

    LogicalPosition newPos = new LogicalPosition(line, EditorHelper.normalizeColumn(editor, line, col, false));

    return editor.logicalPositionToOffset(newPos);
  }

  public int moveCaretToLinePercent(Editor editor, DataContext context, int count) {
    if (count > 100) count = 100;

    return moveCaretToLineStartSkipLeading(editor, EditorHelper.normalizeLine(
      editor, (EditorHelper.getLineCount(editor) * count + 99) / 100 - 1));
  }

  public int moveCaretGotoLineLast(Editor editor, DataContext context, int rawCount, int lline) {
    return moveCaretToLineStartSkipLeading(editor, rawCount == 0 ?
                                                   EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) : lline);
  }

  public int moveCaretGotoLineLastEnd(Editor editor, DataContext context, int rawCount, int lline, boolean pastEnd) {
    return moveCaretToLineEnd(editor, rawCount == 0 ?
                                      EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) : lline, pastEnd);
  }

  public int moveCaretGotoLineFirst(Editor editor, DataContext context, int lline) {
    return moveCaretToLineStartSkipLeading(editor, lline);
  }

  public static void moveCaret(Editor editor, DataContext context, int offset) {
    if (offset >= 0 && offset <= editor.getDocument().getTextLength()) {
      editor.getCaretModel().moveToOffset(offset);
      EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
      scrollCaretIntoView(editor);

      if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
        CommandGroups.getInstance().getMotion().updateSelection(editor, context, offset);
      }
      else {
        editor.getSelectionModel().removeSelection();
      }
    }
  }

  public int moveCaretGotoPreviousTab(final DataContext context) {
    final AnAction previousTab = ActionManager.getInstance().getAction("PreviousTab");
    previousTab.actionPerformed(new AnActionEvent(
      null,
      context,
      "",
      new Presentation(),
      ActionManager.getInstance(),
      0));
    return 0;
  }

  public int moveCaretGotoNextTab(final DataContext context) {
    final AnAction previousTab = ActionManager.getInstance().getAction("NextTab");
    previousTab.actionPerformed(new AnActionEvent(
      null,
      context,
      "",
      new Presentation(),
      ActionManager.getInstance(),
      0));
    return 0;
  }


  public static void scrollCaretIntoView(Editor editor) {
    int cline = EditorHelper.getCurrentVisualLine(editor);
    int vline = EditorHelper.getVisualLineAtTopOfScreen(editor);
    boolean scrolljump = (CommandState.getInstance(editor).getFlags() & Command.FLAG_IGNORE_SCROLL_JUMP) == 0;
    int scrolloff = ((NumberOption)Options.getInstance().getOption("scrolloff")).value();
    int sjSize = 0;
    if (scrolljump) {
      sjSize = Math.max(0, ((NumberOption)Options.getInstance().getOption("scrolljump")).value() - 1);
    }

    int height = EditorHelper.getScreenHeight(editor);
    int vtop = vline + scrolloff;
    int vbot = vline + height - scrolloff;
    if (scrolloff >= height / 2) {
      scrolloff = height / 2;
      vtop = vline + scrolloff;
      vbot = vline + height - scrolloff;
      if (vtop == vbot) {
        vbot++;
      }
    }

    int diff;
    if (cline < vtop) {
      diff = cline - vtop;
      sjSize = -sjSize;
    }
    else {
      diff = cline - vbot + 1;
      if (diff < 0) {
        diff = 0;
      }
    }

    if (diff != 0) {
      int line;
      // If we need to move the top line more than a half screen worth then we just center the cursor line
      if (Math.abs(diff) > height / 2) {
        line = cline - height / 2 - 1;
      }
      // Otherwise put the new cursor line "scrolljump" lines from the top/bottom
      else {
        line = vline + diff + sjSize;
      }

      line = Math.min(line, EditorHelper.getVisualLineCount(editor) - height);
      line = Math.max(0, line);
      scrollLineToTopOfScreen(editor, line);
    }

    int ccol = EditorHelper.getCurrentVisualColumn(editor);
    int vcol = EditorHelper.getVisualColumnAtLeftOfScreen(editor);
    int width = EditorHelper.getScreenWidth(editor);
    scrolljump = (CommandState.getInstance(editor).getFlags() & Command.FLAG_IGNORE_SIDE_SCROLL_JUMP) == 0;
    scrolloff = ((NumberOption)Options.getInstance().getOption("sidescrolloff")).value();
    sjSize = 0;
    if (scrolljump) {
      sjSize = Math.max(0, ((NumberOption)Options.getInstance().getOption("sidescroll")).value() - 1);
      if (sjSize == 0) {
        sjSize = width / 2;
      }
    }

    int vleft = vcol + scrolloff;
    int vright = vcol + width - scrolloff;
    if (scrolloff >= width / 2) {
      scrolloff = width / 2;
      vleft = vcol + scrolloff;
      vright = vcol + width - scrolloff;
      if (vleft == vright) {
        vright++;
      }
    }

    sjSize = Math.min(sjSize, width / 2 - scrolloff);

    if (ccol < vleft) {
      diff = ccol - vleft + 1;
      sjSize = -sjSize;
    }
    else {
      diff = ccol - vright + 1;
      if (diff < 0) {
        diff = 0;
      }
    }

    if (diff != 0) {
      int col;
      if (Math.abs(diff) > width / 2) {
        col = ccol - width / 2 - 1;
      }
      else {
        col = vcol + diff + sjSize;
      }

      //col = Math.min(col, EditorHelper.getMaximumColumnWidth());
      col = Math.max(0, col);
      scrollColumnToLeftOfScreen(editor, col);
    }
  }

  public boolean selectPreviousVisualMode(Editor editor, DataContext context) {
    logger.debug("selectPreviousVisualMode");
    VisualRange vr = EditorData.getLastVisualRange(editor);
    if (vr == null) {
      return false;
    }

    if (logger.isDebugEnabled()) logger.debug("vr=" + vr);
    CommandState.getInstance(editor).pushState(CommandState.MODE_VISUAL, vr.getType(), KeyParser.MAPPING_VISUAL);

    visualStart = vr.getStart();
    visualEnd = vr.getEnd();
    visualOffset = vr.getOffset();

    updateSelection(editor, context, visualEnd);

    editor.getCaretModel().moveToOffset(visualOffset);
    //EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    //MotionGroup.moveCaret(editor, context, vr.getOffset());

    return true;
  }

  public boolean swapVisualSelections(Editor editor, DataContext context) {
    VisualRange vr = EditorData.getLastVisualRange(editor);
    if (vr == null) {
      return false;
    }

    EditorData.setLastVisualRange(editor, new VisualRange(visualStart, visualEnd,
                                                          CommandState.getInstance(editor).getSubMode(), visualOffset));

    visualStart = vr.getStart();
    visualEnd = vr.getEnd();
    visualOffset = vr.getOffset();

    CommandState.getInstance(editor).setSubMode(vr.getType());

    updateSelection(editor, context, visualEnd);

    editor.getCaretModel().moveToOffset(visualOffset);
    //EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    //MotionGroup.moveCaret(editor, context, vr.getOffset());

    return true;
  }

  public void setVisualMode(Editor editor, DataContext context, int mode) {
    logger.debug("setVisualMode");
    int oldMode = CommandState.getInstance(editor).getSubMode();
    if (mode == 0) {
      int start = editor.getSelectionModel().getSelectionStart();
      int end = editor.getSelectionModel().getSelectionEnd();
      if (start != end) {
        int line = editor.offsetToLogicalPosition(start).line;
        int lstart = EditorHelper.getLineStartOffset(editor, line);
        int lend = EditorHelper.getLineEndOffset(editor, line, true);
        if (logger.isDebugEnabled()) logger.debug("start=" + start + ", end=" + end + ", lstart=" + lstart + ", lend=" + lend);
        if (lstart == start && lend + 1 == end) {
          mode = Command.FLAG_MOT_LINEWISE;
        }
        else {
          mode = Command.FLAG_MOT_CHARACTERWISE;
        }
      }
    }

    if (oldMode == 0 && mode == 0) {
      editor.getSelectionModel().removeSelection();
      return;
    }

    if (mode == 0) {
      exitVisual(editor);
    }
    else {
      CommandState.getInstance(editor).pushState(CommandState.MODE_VISUAL, mode, KeyParser.MAPPING_VISUAL);
    }

    KeyHandler.getInstance().reset(editor);

    visualStart = editor.getSelectionModel().getSelectionStart();
    visualEnd = editor.getSelectionModel().getSelectionEnd();
    if (CommandState.getInstance(editor).getSubMode() == Command.FLAG_MOT_CHARACTERWISE) {
      BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
      int adj = 1;
      if (opt.getValue().equals("exclusive")) {
        adj = 0;
      }
      visualEnd -= adj;
    }
    visualOffset = editor.getCaretModel().getOffset();
    if (logger.isDebugEnabled()) logger.debug("visualStart=" + visualStart + ", visualEnd=" + visualEnd);

    CommandGroups.getInstance().getMark().setMark(editor, context, '<', visualStart);
    CommandGroups.getInstance().getMark().setMark(editor, context, '>', visualEnd);
  }

  public boolean toggleVisual(Editor editor, DataContext context, int count, int rawCount, int mode) {
    if (logger.isDebugEnabled()) logger.debug("toggleVisual: mode=" + mode);
    int currentMode = CommandState.getInstance(editor).getSubMode();
    if (CommandState.getInstance(editor).getMode() != CommandState.MODE_VISUAL) {
      int start;
      int end;
      if (rawCount > 0) {
        VisualChange range = EditorData.getLastVisualOperatorRange(editor);
        if (range == null) {
          logger.debug("no prior visual range");
          return false;
        }
        else {
          if (logger.isDebugEnabled()) logger.debug("last visual change: " + range);
        }
        mode = range.getType();
        start = editor.getCaretModel().getOffset();
        end = calculateVisualRange(editor, context, range, count);
      }
      else {
        start = end = editor.getSelectionModel().getSelectionStart();
      }
      CommandState.getInstance(editor).pushState(CommandState.MODE_VISUAL, mode, KeyParser.MAPPING_VISUAL);
      visualStart = start;
      updateSelection(editor, context, end);
      MotionGroup.moveCaret(editor, context, visualEnd);
    }
    else if (mode == currentMode) {
      exitVisual(editor);
    }
    else {
      CommandState.getInstance(editor).setSubMode(mode);
      updateSelection(editor, context, visualEnd);
    }

    return true;
  }

  private int calculateVisualRange(Editor editor, DataContext context, VisualChange range, int count) {
    int lines = range.getLines();
    int chars = range.getColumns();
    if (range.getType() == Command.FLAG_MOT_LINEWISE || range.getType() == Command.FLAG_MOT_BLOCKWISE || lines > 1) {
      lines *= count;
    }
    if ((range.getType() == Command.FLAG_MOT_CHARACTERWISE && lines == 1) || range.getType() == Command.FLAG_MOT_BLOCKWISE) {
      chars *= count;
    }
    int start = editor.getCaretModel().getOffset();
    LogicalPosition sp = editor.offsetToLogicalPosition(start);
    int endLine = sp.line + lines - 1;
    int res;
    if (range.getType() == Command.FLAG_MOT_LINEWISE) {
      res = moveCaretToLine(editor, context, endLine);
    }
    else if (range.getType() == Command.FLAG_MOT_CHARACTERWISE) {
      if (lines > 1) {
        res = moveCaretToLineStart(editor, endLine) +
              Math.min(EditorHelper.getLineLength(editor, endLine), chars);
      }
      else {
        res = EditorHelper.normalizeOffset(editor, sp.line, start + chars - 1, false);
      }
    }
    else {
      int endcol = Math.min(EditorHelper.getLineLength(editor, endLine), sp.column + chars - 1);
      res = editor.logicalPositionToOffset(new LogicalPosition(endLine, endcol));
    }

    return res;
  }

  public void exitVisual(Editor editor) {
    resetVisual(editor);
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
      CommandState.getInstance(editor).popState();
    }
  }

  public void resetVisual(Editor editor) {
    logger.debug("resetVisual");
    EditorData.setLastVisualRange(editor, new VisualRange(visualStart,
                                                          visualEnd, CommandState.getInstance(editor).getSubMode(), visualOffset));
    if (logger.isDebugEnabled()) logger.debug("visualStart=" + visualStart + ", visualEnd=" + visualEnd);

    editor.getSelectionModel().removeSelection();

    CommandState.getInstance(editor).setSubMode(0);
  }

  public VisualChange getVisualOperatorRange(Editor editor, int cmdFlags) {
    logger.debug("vis op range");
    int start = visualStart;
    int end = visualEnd;
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    start = EditorHelper.normalizeOffset(editor, start, false);
    end = EditorHelper.normalizeOffset(editor, end, false);
    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("end=" + end);
    }
    LogicalPosition sp = editor.offsetToLogicalPosition(start);
    LogicalPosition ep = editor.offsetToLogicalPosition(end);
    int lines = ep.line - sp.line + 1;
    int chars;
    int type;
    if (CommandState.getInstance(editor).getSubMode() == Command.FLAG_MOT_LINEWISE ||
        (cmdFlags & Command.FLAG_MOT_LINEWISE) != 0) {
      chars = ep.column;
      type = Command.FLAG_MOT_LINEWISE;
    }
    else if (CommandState.getInstance(editor).getSubMode() == Command.FLAG_MOT_CHARACTERWISE) {
      type = Command.FLAG_MOT_CHARACTERWISE;
      if (lines > 1) {
        chars = ep.column;
      }
      else {
        chars = ep.column - sp.column + 1;
      }
    }
    else {
      chars = ep.column - sp.column + 1;
      if (EditorData.getLastColumn(editor) == MotionGroup.LAST_COLUMN) {
        chars = MotionGroup.LAST_COLUMN;
      }
      type = Command.FLAG_MOT_BLOCKWISE;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("lines=" + lines);
      logger.debug("chars=" + chars);
      logger.debug("type=" + type);
    }
    return new VisualChange(lines, chars, type);
  }

  public TextRange getVisualRange(Editor editor) {
    if (ApiHelper.supportsBlockSelection() && editor.getSelectionModel().hasBlockSelection()) {
      TextRange res = new TextRange(editor.getSelectionModel().getBlockSelectionStarts(),
                                    editor.getSelectionModel().getBlockSelectionEnds());
      // If the last left/right motion was the $ command, simulate each line being selected to end-of-line
      if (EditorData.getLastColumn(editor) >= MotionGroup.LAST_COLUMN) {
        int[] starts = res.getStartOffsets();
        int[] ends = res.getEndOffsets();

        for (int i = 0; i < starts.length; i++) {
          if (ends[i] > starts[i]) {
            ends[i] = EditorHelper.getLineEndForOffset(editor, starts[i]);
          }
        }

        res = new TextRange(starts, ends);
      }

      return res;
    }
    else {
      return new TextRange(editor.getSelectionModel().getSelectionStart(),
                           editor.getSelectionModel().getSelectionEnd());
    }
  }

  public TextRange getRawVisualRange() {
    return new TextRange(visualStart, visualEnd);
  }

  private void updateSelection(Editor editor, DataContext context, int offset) {
    logger.debug("updateSelection");
    visualEnd = offset;
    visualOffset = offset;
    int start = visualStart;
    int end = visualEnd;
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    if (CommandState.getInstance(editor).getSubMode() == Command.FLAG_MOT_CHARACTERWISE) {
      BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
      int lineend = EditorHelper.getLineEndForOffset(editor, end);
      if (logger.isDebugEnabled()) {
        logger.debug("lineend=" + lineend);
        logger.debug("end=" + end);
      }
      int adj = 1;
      if (opt.getValue().equals("exclusive") || end == lineend) {
        adj = 0;
      }
      end = Math.min(EditorHelper.getFileSize(editor), end + adj);
      if (logger.isDebugEnabled()) logger.debug("start=" + start + ", end=" + end);
      editor.getSelectionModel().setSelection(start, end);
    }
    else if (CommandState.getInstance(editor).getSubMode() == Command.FLAG_MOT_LINEWISE) {
      start = EditorHelper.getLineStartForOffset(editor, start);
      end = EditorHelper.getLineEndForOffset(editor, end);
      if (logger.isDebugEnabled()) logger.debug("start=" + start + ", end=" + end);
      editor.getSelectionModel().setSelection(start, end);
    }
    else if (ApiHelper.supportsBlockSelection()) {
      LogicalPosition lstart = editor.offsetToLogicalPosition(start);
      LogicalPosition lend = editor.offsetToLogicalPosition(end);
      if (logger.isDebugEnabled()) logger.debug("lstart=" + lstart + ", lend=" + lend);
      editor.getSelectionModel().setBlockSelection(lstart, lend);
    }

    CommandGroups.getInstance().getMark().setMark(editor, context, '<', start);
    CommandGroups.getInstance().getMark().setMark(editor, context, '>', end);
  }

  public boolean swapVisualEnds(Editor editor, DataContext context) {
    int t = visualEnd;
    visualEnd = visualStart;
    visualStart = t;

    moveCaret(editor, context, visualEnd);

    return true;
  }

  public boolean swapVisualEndsBlock(Editor editor, DataContext context) {
    if (CommandState.getInstance(editor).getSubMode() != Command.FLAG_MOT_BLOCKWISE) {
      return swapVisualEnds(editor, context);
    }

    LogicalPosition lstart = editor.getSelectionModel().getBlockStart();
    LogicalPosition lend = editor.getSelectionModel().getBlockEnd();
    if (lstart == null || lend == null) {
      return false;
    }

    if (visualStart > visualEnd) {
      LogicalPosition t = lend;
      lend = lstart;
      lstart = t;
    }

    LogicalPosition nstart = new LogicalPosition(lstart.line, lend.column);
    LogicalPosition nend = new LogicalPosition(lend.line, lstart.column);

    visualStart = editor.logicalPositionToOffset(nstart);
    visualEnd = editor.logicalPositionToOffset(nend);

    moveCaret(editor, context, visualEnd);

    return true;
  }

  public void moveVisualStart(Editor editor, int startOffset) {
    visualStart = startOffset;
  }

  public void processEscape(Editor editor, DataContext context) {
    exitVisual(editor);
  }

  public static class MotionEditorChange extends FileEditorManagerAdapter {
    public void selectionChanged(FileEditorManagerEvent event) {
      if (ExEntryPanel.getInstance().isActive()) {
        ExEntryPanel.getInstance().deactivate(false);
      }

      if (MorePanel.getInstance().isActive()) {
        MorePanel.getInstance().deactivate(false);
      }

      FileEditor fe = event.getOldEditor();
      if (fe instanceof TextEditor) {
        Editor editor = ((TextEditor)fe).getEditor();
        if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
          CommandGroups.getInstance().getMotion().exitVisual(
            EditorHelper.getEditor(event.getManager(), event.getOldFile()));
        }
      }
    }
  }

  private static class EditorSelectionHandler implements SelectionListener {
    public void selectionChanged(SelectionEvent selectionEvent) {
      if (makingChanges) return;

      makingChanges = true;

      Editor editor = selectionEvent.getEditor();
      TextRange range = new TextRange(selectionEvent.getNewRange().getStartOffset(), selectionEvent.getNewRange().getEndOffset());

      Editor[] editors = EditorFactory.getInstance().getEditors(editor.getDocument());
      for (Editor ed : editors) {
        if (ed.equals(editor)) {
          continue;
        }

        ed.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        ed.getCaretModel().moveToOffset(editor.getCaretModel().getOffset());
      }

      makingChanges = false;
    }

    private boolean makingChanges = false;
  }

  private static class EditorScrollHandler implements VisibleAreaListener {
    public static void ignoreChanges(boolean ignore) {
      EditorScrollHandler.ignore = ignore;
    }

    public void visibleAreaChanged(VisibleAreaEvent visibleAreaEvent) {
      if (ignore) return;

      Editor editor = visibleAreaEvent.getEditor();
      if (CommandState.inInsertMode(editor)) {
        return;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("old=" + visibleAreaEvent.getOldRectangle());
        logger.debug("new=" + visibleAreaEvent.getNewRectangle());
      }

      if (!visibleAreaEvent.getNewRectangle().equals(visibleAreaEvent.getOldRectangle())) {
        if (!EditorData.isConsoleOutput(editor) && !isTabSwitchEvent(visibleAreaEvent)) {
          MotionGroup.moveCaretToView(editor, null);
        }
      }
    }

    private static boolean isTabSwitchEvent(final VisibleAreaEvent visibleAreaEvent) {
      final Rectangle newRectangle = visibleAreaEvent.getNewRectangle();
      return newRectangle.width == 0 || newRectangle.height == 0;
    }

    private static boolean ignore = false;
  }

  private static class EditorMouseHandler implements EditorMouseListener, EditorMouseMotionListener {
    public void mouseMoved(EditorMouseEvent event) {
      // no-op
    }

    public void mouseDragged(EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getArea() == EditorMouseEventArea.EDITING_AREA ||
          event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA) {
        if (dragEditor == null) {
          if (event.getArea() == EditorMouseEventArea.EDITING_AREA) {
            mode = Command.FLAG_MOT_CHARACTERWISE;
          }
          else if (event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA) {
            mode = Command.FLAG_MOT_LINEWISE;
          }
          startOff = event.getEditor().getSelectionModel().getSelectionStart();
          endOff = event.getEditor().getSelectionModel().getSelectionEnd();
          if (logger.isDebugEnabled()) logger.debug("startOff=" + startOff);
        }

        dragEditor = event.getEditor();
      }
    }

    public void mousePressed(EditorMouseEvent event) {
      // no-op
    }

    public void mouseClicked(EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getArea() == EditorMouseEventArea.EDITING_AREA) {
        CommandGroups.getInstance().getMotion().processMouseClick(event.getEditor(), event.getMouseEvent());
        //event.consume();
      }
      else if (event.getArea() != EditorMouseEventArea.ANNOTATIONS_AREA && event.getArea() != EditorMouseEventArea.FOLDING_OUTLINE_AREA) {
        CommandGroups.getInstance().getMotion().processLineSelection(
          event.getEditor(), event.getMouseEvent().getButton() == MouseEvent.BUTTON3);
        //event.consume();
      }
    }

    public void mouseReleased(EditorMouseEvent event) {
      if (!VimPlugin.isEnabled()) return;

      if (event.getEditor().equals(dragEditor)) {
        CommandGroups.getInstance().getMotion().processMouseReleased(event.getEditor(), mode, startOff, endOff);

        //event.consume();
        dragEditor = null;
      }
    }

    public void mouseEntered(EditorMouseEvent event) {
      // no-op
    }

    public void mouseExited(EditorMouseEvent event) {
      // no-op
    }

    private Editor dragEditor = null;
    private int mode;
    private int startOff;
    private int endOff;
  }

  private int lastFTCmd = 0;
  private char lastFTChar;
  private int visualStart;
  private int visualEnd;
  private int visualOffset;
  private EditorMouseHandler mouseHandler = new EditorMouseHandler();
  private EditorSelectionHandler selectionHandler = new EditorSelectionHandler();
  private EditorScrollHandler scrollHandler = new EditorScrollHandler();

  private static Logger logger = Logger.getInstance(MotionGroup.class.getName());
}
