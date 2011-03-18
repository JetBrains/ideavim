package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.option.BoundListOption;
import com.maddyhome.idea.vim.option.Options;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

/**
 * Provides all the insert/replace related functionality
 * TODO - change cursor for the different modes
 */
public class ChangeGroup extends AbstractActionGroup {
  /**
   * Creates the group
   */
  public ChangeGroup() {
    // We want to know when a user clicks the mouse somewhere in the editor so we can clear any
    // saved text for the current insert mode.
    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        editor.addEditorMouseListener(listener);
        EditorData.setChangeGroup(editor, true);
      }

      public void editorReleased(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (EditorData.getChangeGroup(editor)) {
          editor.removeEditorMouseListener(listener);
          EditorData.setChangeGroup(editor, false);
        }
      }

      private EditorMouseAdapter listener = new EditorMouseAdapter() {
        public void mouseClicked(EditorMouseEvent event) {
          Editor editor = event.getEditor();
          if (!VimPlugin.isEnabled()) {
            return;
          }

          if (CommandState.inInsertMode(editor)) {
            clearStrokes(editor);
          }
        }
      };
    });
  }

  public void setInsertRepeat(int lines, int column, boolean append) {
    repeatLines = lines;
    repeatColumn = column;
    repeatAppend = append;
  }

  /**
   * Begin insert before the cursor position
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertBeforeCursor(Editor editor, DataContext context) {
    initInsert(editor, context, CommandState.MODE_INSERT);
  }

  /**
   * Begin insert before the first non-blank on the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertBeforeFirstNonBlank(Editor editor, DataContext context) {
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor));
    initInsert(editor, context, CommandState.MODE_INSERT);
  }

  /**
   * Begin insert before the start of the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertLineStart(Editor editor, DataContext context) {
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor));
    initInsert(editor, context, CommandState.MODE_INSERT);
  }

  /**
   * Begin insert after the cursor position
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertAfterCursor(Editor editor, DataContext context) {
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, 1, true));
    initInsert(editor, context, CommandState.MODE_INSERT);
  }

  /**
   * Begin insert after the end of the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertAfterLineEnd(Editor editor, DataContext context) {
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, true));
    initInsert(editor, context, CommandState.MODE_INSERT);
  }

  /**
   * Begin insert before the current line by creating a new blank line above the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertNewLineAbove(final Editor editor, final DataContext context) {
    if (EditorHelper.getCurrentVisualLine(editor) == 0) {
      MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor));
      initInsert(editor, context, CommandState.MODE_INSERT);

      if (!editor.isOneLineMode()) {
        CommandState state = CommandState.getInstance(editor);
        if (state.getMode() != CommandState.MODE_REPEAT) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), context);
              MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretVertical(editor, -1));
            }
          });
        }
        else {
          //KeyHandler.executeAction("VimEditorEnter", context);
          MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretVertical(editor, -1));
        }
      }
    }
    else {
      MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretVertical(editor, -1));
      insertNewLineBelow(editor, context);
    }
  }

  /**
   * Begin insert after the current line by creating a new blank line below the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertNewLineBelow(final Editor editor, final DataContext context) {
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, true));
    initInsert(editor, context, CommandState.MODE_INSERT);

    CommandState state = CommandState.getInstance(editor);
    if (state.getMode() != CommandState.MODE_REPEAT) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), context);
        }
      });
    }
    //KeyHandler.executeAction("VimEditorEnter", context);
  }

  /**
   * Begin insert at the location of the previous insert
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertAtPreviousInsert(Editor editor, DataContext context) {
    int offset = CommandGroups.getInstance().getMotion().moveCaretToFileMarkLine(editor, context, '^');
    if (offset != -1) {
      MotionGroup.moveCaret(editor, context, offset);
    }

    insertBeforeCursor(editor, context);
  }

  /**
   * Inserts the previously inserted text
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param exit    true if insert mode should be exited after the insert, false should stay in insert mode
   */
  public void insertPreviousInsert(Editor editor, DataContext context, boolean exit) {
    repeatInsertText(editor, context, 1);
    if (exit) {
      processEscape(editor, context);
    }
  }

  /**
   * Exits insert mode and brings up the help system
   *
   * @param editor  The editor to exit insert mode in
   * @param context The data context
   */
  public void insertHelp(Editor editor, DataContext context) {
    processEscape(editor, context);
    KeyHandler.executeAction("HelpTopics", context);
  }

  /**
   * Inserts the contents of the specified register
   *
   * @param editor  The editor to insert the text into
   * @param context The data context
   * @param key     The register name
   * @return true if able to insert the register contents, false if not
   */
  public boolean insertRegister(Editor editor, DataContext context, char key) {
    Register register = CommandGroups.getInstance().getRegister().getRegister(key);
    if (register != null) {
      String text = register.getText();
      for (int i = 0; i < text.length(); i++) {
        processKey(editor, context, KeyStroke.getKeyStroke(text.charAt(i)));
      }

      return true;
    }

    return false;
  }

  /**
   * Inserts the character above/below the cursor at the cursor location
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param dir     1 for getting from line below cursor, -1 for getting from line aboe cursor
   * @return true if able to get the character and insert it, false if not
   */
  public boolean insertCharacterAroundCursor(Editor editor, DataContext context, int dir) {
    boolean res = false;

    VisualPosition vp = editor.getCaretModel().getVisualPosition();
    vp = new VisualPosition(vp.line + dir, vp.column);
    int len = EditorHelper.getLineLength(editor, EditorHelper.visualLineToLogicalLine(editor, vp.line));
    if (vp.column < len) {
      int offset = EditorHelper.visualPostionToOffset(editor, vp);
      char ch = EditorHelper.getDocumentChars(editor).charAt(offset);
      processKey(editor, context, KeyStroke.getKeyStroke(ch));
      res = true;
    }

    return res;
  }

  /**
   * If the cursor is currently after the start of the current insert this deletes all the newly inserted text.
   * Otherwise it deletes all text from the cursor back to the first non-blank in the line.
   *
   * @param editor  The editor to delete the text from
   * @param context The data context
   * @return true if able to delete the text, false if not
   */
  public boolean insertDeleteInsertedText(Editor editor, DataContext context) {
    int deleteTo = insertStart;
    int offset = editor.getCaretModel().getOffset();
    if (offset == insertStart) {
      deleteTo = CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor);
    }

    if (deleteTo != -1) {
      deleteRange(editor, context, new TextRange(deleteTo, offset), Command.FLAG_MOT_EXCLUSIVE, false);

      return true;
    }

    return false;
  }

  /**
   * Deletes the text from the cursor to the start of the previous word
   *
   * @param editor  The editor to delete the text from
   * @param context The data context
   * @return true if able to delete text, false if not
   */
  public boolean insertDeletePreviousWord(Editor editor, DataContext context) {
    int deleteTo = insertStart;
    int offset = editor.getCaretModel().getOffset();
    if (offset == insertStart) {
      deleteTo = CommandGroups.getInstance().getMotion().moveCaretToNextWord(editor, -1, false);
    }

    if (deleteTo != -1) {
      deleteRange(editor, context, new TextRange(deleteTo, offset), Command.FLAG_MOT_EXCLUSIVE, false);

      return true;
    }

    return false;
  }

  /**
   * Begin insert/replace mode
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param mode    The mode - inidicate insert or replace
   */
  private void initInsert(Editor editor, DataContext context, int mode) {
    CommandState state = CommandState.getInstance(editor);

    insertStart = editor.getCaretModel().getOffset();
    CommandGroups.getInstance().getMark().setMark(editor, context, '[', insertStart);

    // If we are repeating the last insert/replace
    if (state.getMode() == CommandState.MODE_REPEAT) {
      if (mode == CommandState.MODE_REPLACE) {
        processInsert(editor, context);
      }
      // If this command doesn't allow repeating, set the count to 1
      if ((state.getCommand().getFlags() & Command.FLAG_NO_REPEAT) != 0) {
        repeatInsert(editor, context, 1, false);
      }
      else {
        repeatInsert(editor, context, state.getCommand().getCount(), false);
      }
      if (mode == CommandState.MODE_REPLACE) {
        processInsert(editor, context);
      }
    }
    // Here we begin insert/replace mode
    else {
      lastInsert = state.getCommand();
      strokes.clear();
      inInsert = true;
      if (mode == CommandState.MODE_REPLACE) {
        processInsert(editor, context);
      }
      state.pushState(mode, 0, KeyParser.MAPPING_INSERT);

      resetCursor(editor, true);
    }
  }

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   * @param started
   */
  private void repeatInsert(Editor editor, DataContext context, int count, boolean started) {
    int cpos;
    if (repeatLines > 0) {
      int vline = EditorHelper.getCurrentVisualLine(editor);
      int lline = EditorHelper.getCurrentLogicalLine(editor);
      cpos = editor.logicalPositionToOffset(new LogicalPosition(vline, repeatColumn));
      for (int i = 0; i < repeatLines; i++) {
        if (repeatAppend && repeatColumn < MotionGroup.LAST_COLUMN &&
            EditorHelper.getVisualLineLength(editor, vline + i) < repeatColumn) {
          String pad = EditorHelper.pad(editor, lline + i, repeatColumn);
          if (pad.length() > 0) {
            int off = editor.getDocument().getLineEndOffset(lline + i);
            insertText(editor, context, off, pad);
          }
        }
        if (repeatColumn >= MotionGroup.LAST_COLUMN) {
          editor.getCaretModel().moveToOffset(
            CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, lline + i, true));
          repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
        }
        else if (EditorHelper.getVisualLineLength(editor, vline + i) >= repeatColumn) {
          editor.getCaretModel().moveToVisualPosition(new VisualPosition(vline + i, repeatColumn));
          repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
        }
      }
    }
    else {
      repeatInsertText(editor, context, count);
      cpos = CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1, false);
    }

    repeatLines = 0;
    repeatColumn = 0;
    repeatAppend = false;

    MotionGroup.moveCaret(editor, context, cpos);
  }

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  private void repeatInsertText(Editor editor, DataContext context, int count) {
    if (lastStrokes == null) {
      return;
    }
    for (int i = 0; i < count; i++) {
      // Treat other keys special by performing the appropriate action they represent in insert/replace mode
      for (Object lastStroke : lastStrokes) {
        if (lastStroke instanceof AnAction) {
          KeyHandler.executeAction((AnAction)lastStroke, context);
          strokes.add(lastStroke);
        }
        else if (lastStroke instanceof Character) {
          processKey(editor, context, KeyStroke.getKeyStroke((Character)lastStroke));
        }
      }
    }
  }

  /**
   * Terminate insert/replace mode after the user presses Escape or Ctrl-C
   *
   * @param editor  The editor that was being edited
   * @param context The data context
   */
  public void processEscape(Editor editor, DataContext context) {
    logger.debug("processing escape");
    int cnt = lastInsert.getCount();
    // Turn off overwrite mode if we were in replace mode
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_REPLACE) {
      KeyHandler.executeAction("VimInsertReplaceToggle", context);
    }
    // If this command doesn't allow repeats, set count to 1
    if ((lastInsert.getFlags() & Command.FLAG_NO_REPEAT) != 0) {
      cnt = 1;
    }

    // Save off current list of keystrokes
    lastStrokes = new ArrayList(strokes);

    // TODO - support . register
    //CommandGroups.getInstance().getRegister().storeKeys(lastStrokes, Command.FLAG_MOT_CHARACTERWISE, '.');

    // If the insert/replace command was preceded by a count, repeat again N - 1 times
    repeatInsert(editor, context, cnt - 1, true);

    CommandGroups.getInstance().getMark().setMark(editor, context, '^', editor.getCaretModel().getOffset());
    CommandGroups.getInstance().getMark().setMark(editor, context, ']', editor.getCaretModel().getOffset());
    CommandState.getInstance(editor).popState();

    if (!CommandState.inInsertMode(editor)) {
      resetCursor(editor, false);
    }
  }

  /**
   * Processes the user pressing the Enter key. If this is REPLACE mode we need to turn off OVERWRITE before and
   * then turn OVERWRITE back on after sending the "Enter" key.
   *
   * @param editor  The editor to press "Enter" in
   * @param context The data context
   */
  public void processEnter(Editor editor, DataContext context) {
    if (editor.isOneLineMode()) {
      return;
    }

    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_REPLACE) {
      KeyHandler.executeAction("VimEditorToggleInsertState", context);
    }
    KeyHandler.executeAction("VimEditorEnter", context);
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_REPLACE) {
      KeyHandler.executeAction("VimEditorToggleInsertState", context);
    }
  }

  /**
   * Processes the user pressing the Insert key while in INSERT or REPLACE mode. This simply toggles the
   * Insert/Overwrite state which updates the status bar.
   *
   * @param editor  The editor to toggle the state in
   * @param context The data context
   */
  public void processInsert(Editor editor, DataContext context) {
    KeyHandler.executeAction("VimEditorToggleInsertState", context);
    CommandState.getInstance(editor).toggleInsertOverwrite();
    inInsert = !inInsert;
  }

  /**
   * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
   * return to INSERT or REPLACE mode.
   *
   * @param editor  The editor to put into NORMAL mode for one command
   * @param context The data context
   */
  public void processSingleCommand(Editor editor, DataContext context) {
    CommandState.getInstance(editor).pushState(CommandState.MODE_COMMAND, CommandState.SUBMODE_SINGLE_COMMAND,
                                               KeyParser.MAPPING_NORMAL);
    clearStrokes(editor);
  }

  /**
   * This processes all "regular" keystrokes entered while in insert/replace mode
   *
   * @param editor  The editor the character was typed into
   * @param context The data context
   * @param key     The user entered keystroke
   * @return true if this was a regular character, false if not
   */
  public boolean processKey(final Editor editor, final DataContext context, final KeyStroke key) {
    if (logger.isDebugEnabled()) {
      logger.debug("processKey(" + key + ")");
    }

    if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
      // Regular characters are not handled by us, pass them back to Idea. We just keep track of the keystroke
      // for repeating later.
      strokes.add(key.getKeyChar());

      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          KeyHandler.getInstance().getOriginalHandler().execute(editor, key.getKeyChar(), context);
        }
      });

      return true;
    }

    return false;
  }

  /**
   * This processes all keystrokes in Insert/Replace mode that were converted into Commands. Some of these
   * commands need to be saved off so the inserted/replaced text can be repeated properly later if needed.
   *
   * @param editor  The editor the command was executed in
   * @param context The data context
   * @param cmd     The command that was executed
   * @return true if the command was stored for later repeat, false if not
   */
  public boolean processCommand(Editor editor, DataContext context, Command cmd) {
    if ((cmd.getFlags() & Command.FLAG_SAVE_STROKE) != 0) {
      strokes.add(cmd.getAction());

      return true;
    }
    else if ((cmd.getFlags() & Command.FLAG_CLEAR_STROKES) != 0) {
      clearStrokes(editor);
      return false;
    }
    else {
      return false;
    }
  }

  /**
   * Clears all the keystrokes from the current insert command
   *
   * @param editor The editor to clear strokes from.
   */
  private void clearStrokes(Editor editor) {
    strokes.clear();
    insertStart = editor.getCaretModel().getOffset();
  }

  /**
   * Deletes count characters from the editor
   *
   * @param editor  The editor to remove the characters from
   * @param context The data context
   * @param count   The number of characters to delete
   * @return true if able to delete, false if not
   */
  public boolean deleteCharacter(Editor editor, DataContext context, int count) {
    int offset = CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, count, true);
    if (offset != -1) {
      boolean res = deleteText(editor, context, new TextRange(editor.getCaretModel().getOffset(), offset), Command.FLAG_MOT_INCLUSIVE);
      int pos = editor.getCaretModel().getOffset();
      int norm = EditorHelper.normalizeOffset(editor, EditorHelper.getCurrentLogicalLine(editor), pos, false);
      if (norm != pos) {
        MotionGroup.moveCaret(editor, context, norm);
      }

      return res;
    }

    return false;
  }

  /**
   * Deletes count lines including the current line
   *
   * @param editor  The editor to remove the lines from
   * @param context The data context
   * @param count   The number of lines to delete
   * @return true if able to delete the lines, false if not
   */
  public boolean deleteLine(Editor editor, DataContext context, int count) {
    int start = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
    int offset = Math.min(CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(editor,
                                                                                           count - 1, true) + 1,
                          EditorHelper.getFileSize(editor, true));
    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("offset=" + offset);
    }
    if (offset != -1) {
      boolean res = deleteText(editor, context, new TextRange(start, offset), Command.FLAG_MOT_LINEWISE);
      if (res && editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) &&
          editor.getCaretModel().getOffset() != 0) {
        MotionGroup.moveCaret(editor, context,
                              CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, -1));
      }

      return res;
    }

    return false;
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down
   *
   * @param editor  The editor to delete from
   * @param context The data context
   * @param count   The number of lines affected
   * @return true if able to delete the text, false if not
   */
  public boolean deleteEndOfLine(Editor editor, DataContext context, int count) {
    int offset = CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(editor, count - 1, true);
    if (offset != -1) {
      boolean res = deleteText(editor, context, new TextRange(editor.getCaretModel().getOffset(), offset), Command.FLAG_MOT_INCLUSIVE);
      int pos = CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1, false);
      if (pos != -1) {
        MotionGroup.moveCaret(editor, context, pos);
      }

      return res;
    }

    return false;
  }

  /**
   * Joins count lines togetheri starting at the cursor. No count or a count of one still joins two lines.
   *
   * @param editor  The editor to join the lines in
   * @param context The data context
   * @param count   The number of lines to join
   * @param spaces  If true the joined lines will have one space between them and any leading space on the second line
   *                will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  public boolean deleteJoinLines(Editor editor, DataContext context, int count, boolean spaces) {
    if (count < 2) count = 2;
    int lline = EditorHelper.getCurrentLogicalLine(editor);
    int total = EditorHelper.getLineCount(editor);
    //noinspection SimplifiableIfStatement
    if (lline + count > total) {
      return false;
    }

    return deleteJoinNLines(editor, context, lline, count, spaces);
  }

  /**
   * Joins all the lines selected by the current visual selection.
   *
   * @param editor  The editor to join the lines in
   * @param context The data context
   * @param range   The range of the visual selection
   * @param spaces  If true the joined lines will have one space between them and any leading space on the second line
   *                will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  public boolean deleteJoinRange(Editor editor, DataContext context, TextRange range, boolean spaces) {
    int startLine = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    int endLine = editor.offsetToLogicalPosition(range.getEndOffset()).line;
    int count = endLine - startLine + 1;
    if (count < 2) count = 2;

    return deleteJoinNLines(editor, context, startLine, count, spaces);
  }

  /**
   * This does the actual joining of the lines
   *
   * @param editor    The editor to join the lines in
   * @param context   The data context
   * @param startLine The starting logical line
   * @param count     The number of lines to join including startLine
   * @param spaces    If true the joined lines will have one space between them and any leading space on the second line
   *                  will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  private boolean deleteJoinNLines(Editor editor, DataContext context, int startLine, int count, boolean spaces) {
    // start my moving the cursor to the very end of the first line
    MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, startLine, true));
    for (int i = 1; i < count; i++) {
      int start = CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, true);
      MotionGroup.moveCaret(editor, context, start);
      int offset;
      if (spaces) {
        offset = CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, 1);
      }
      else {
        offset = CommandGroups.getInstance().getMotion().moveCaretToLineStartOffset(editor, 1);
      }
      deleteText(editor, context, new TextRange(editor.getCaretModel().getOffset(), offset), 0);
      if (spaces) {
        insertText(editor, context, start, " ");
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1, false));
      }
    }

    return true;
  }

  /**
   * Delete all text moved over by the supplied motion command argument.
   *
   * @param editor   The editor to delete the text from
   * @param context  The data context
   * @param count    The number of times to repear the deletion
   * @param rawCount The actual count entered by the user
   * @param argument The motion command
   * @param isChange if from a change
   * @return true if able to delete the text, false if not
   */
  public boolean deleteMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument, boolean isChange) {
    TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, true, false);
    if (range == null) {
      return (EditorHelper.getFileSize(editor) == 0);
    }

    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is deleted when it shouldn't be.
    String text = EditorHelper.getDocumentChars(editor).subSequence(range.getStartOffset(),
                                                                    range.getEndOffset()).toString();
    if (text.indexOf('\n') >= 0 &&
        !(range.getStartOffset() == 0 || EditorHelper.getDocumentChars(editor).charAt(range.getStartOffset() - 1) == '\n')) {
      String id = ActionManager.getInstance().getId(argument.getMotion().getAction());
      if (logger.isDebugEnabled()) {
        logger.debug("action id=" + id);
      }
      if (id.equals("VimMotionWordRight") || id.equals("VimMotionBigWordRight") || id.equals("VimMotionCamelRight")) {
        range = new TextRange(range.getStartOffset(), range.getEndOffset() - 1);
      }
    }

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    if (!isChange && (argument.getMotion().getFlags() & Command.FLAG_MOT_LINEWISE) == 0) {
      LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      LogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (start.line != end.line) {
        if (!SearchHelper.anyNonWhitespace(editor, range.getStartOffset(), -1) &&
            !SearchHelper.anyNonWhitespace(editor, range.getEndOffset(), 1)) {
          int flags = argument.getMotion().getFlags();
          flags &= ~Command.FLAG_MOT_EXCLUSIVE;
          flags &= ~Command.FLAG_MOT_INCLUSIVE;
          flags |= Command.FLAG_MOT_LINEWISE;
          argument.getMotion().setFlags(flags);
        }
      }
    }
    return deleteRange(editor, context, range, argument.getMotion().getFlags(), isChange);
  }

  /**
   * Delete the range of text.
   *
   * @param editor   The editor to delete the text from
   * @param context  The data context
   * @param range    The range to delete
   * @param type     The type of deletion (FLAG_MOT_LINEWISE, FLAG_MOT_EXCLUSIVE, or FLAG_MOT_INCLUSIVE)
   * @param isChange If from a change
   * @return true if able to delete the text, false if not
   */
  public boolean deleteRange(Editor editor, DataContext context, TextRange range, int type, boolean isChange) {
    if (range == null) {
      return false;
    }
    else {
      boolean res = deleteText(editor, context, range, type);

      if (res && editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) &&
          editor.getCaretModel().getOffset() != 0 && !isChange) {
        MotionGroup.moveCaret(editor, context,
                              CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, -1));
      }
      else if (res && range.isMultiple()) {
        MotionGroup.moveCaret(editor, context, range.getStartOffset());
      }
      else if (res && !isChange) {
        VisualPosition vp = editor.getCaretModel().getVisualPosition();
        int col = EditorHelper.normalizeVisualColumn(editor, vp.line, vp.column, false);
        if (col != vp.column) {
          editor.getCaretModel().moveToVisualPosition(new VisualPosition(vp.line, col));
        }
      }

      return res;
    }
  }

  /**
   * Begin Replace mode
   *
   * @param editor  The editor to replace in
   * @param context The data context
   * @return true
   */
  public boolean changeReplace(Editor editor, DataContext context) {
    initInsert(editor, context, CommandState.MODE_REPLACE);

    return true;
  }

  /**
   * Replace each of the next count characters with the charcter ch
   *
   * @param editor  The editor to chage
   * @param context The data context
   * @param count   The number of characters to change
   * @param ch      The character to change to
   * @return true if able to change count characters, false if not
   */
  public boolean changeCharacter(Editor editor, DataContext context, int count, char ch) {
    int col = EditorHelper.getCurrentLogicalColumn(editor);
    int len = EditorHelper.getLineLength(editor);
    int offset = editor.getCaretModel().getOffset();
    if (len - col < count) {
      return false;
    }

    // Special case - if char is newline, only add one despite count
    int num = count;
    String space = null;
    if (ch == '\n') {
      num = 1;
      space = EditorHelper.getLeadingWhitespace(editor, editor.offsetToLogicalPosition(offset).line);
      if (logger.isDebugEnabled()) {
        logger.debug("space='" + space + "'");
      }
    }

    StringBuffer repl = new StringBuffer(count);
    for (int i = 0; i < num; i++) {
      repl.append(ch);
    }

    replaceText(editor, context, offset, offset + count, repl.toString());

    // Indent new line if we replaced with a newline
    if (ch == '\n') {
      insertText(editor, context, offset + 1, space);
      int slen = space.length();
      if (slen == 0) {
        slen++;
      }
      editor.getCaretModel().moveToOffset(offset + slen);
    }

    return true;
  }

  /**
   * Each character in the supplied range gets replaced with the character ch
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param range   The range to change
   * @param ch      The replacing character
   * @return true if able to change the range, false if not
   */
  public boolean changeCharacterRange(Editor editor, DataContext context, TextRange range, char ch) {
    if (logger.isDebugEnabled()) {
      logger.debug("change range: " + range + " to " + ch);
    }
    /*
    int max = range.getMaxLength();
    StringBuffer chs = new StringBuffer(max);
    for (int i = 0; i < max; i++)
    {
        chs.append(ch);
    }

    for (int i = 0; i < range.size(); i++)
    {
        int soff = range.getStartOffsets()[i];
        int eoff = EditorHelper.getLineEndForOffset(editor, range.getEndOffsets()[i]) - 1;
        eoff = Math.min(eoff, range.getEndOffsets()[i]);
        if (eoff > soff)
        {
            replaceText(editor, context, soff, eoff, chs.substring(0, eoff - soff));
        }
    }
    */

    CharSequence chars = EditorHelper.getDocumentChars(editor);
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    for (int j = ends.length - 1; j >= 0; j--) {
      for (int i = starts[j]; i < ends[j]; i++) {
        if (i < chars.length() && '\n' != chars.charAt(i)) {
          replaceText(editor, context, i, i + 1, Character.toString(ch));
        }
      }
    }

    return true;
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param count   The number of characters to change
   * @return true if able to delete count characters, false if not
   */
  public boolean changeCharacters(Editor editor, DataContext context, int count) {
    int len = EditorHelper.getLineLength(editor);
    int col = EditorHelper.getCurrentLogicalColumn(editor);
    if (col + count >= len) {
      return changeEndOfLine(editor, context, 1);
    }

    boolean res = deleteCharacter(editor, context, count);
    if (res) {
      initInsert(editor, context, CommandState.MODE_INSERT);
    }

    return res;
  }

  /**
   * Delete count lines and then enter insert mode
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param count   The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  public boolean changeLine(Editor editor, DataContext context, int count) {
    boolean res = deleteLine(editor, context, count);
    if (res) {
      insertNewLineAbove(editor, context);
    }

    return res;
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down and enter insert mode
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param count   The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  public boolean changeEndOfLine(Editor editor, DataContext context, int count) {
    boolean res = deleteEndOfLine(editor, context, count);
    if (res) {
      insertAfterLineEnd(editor, context);
    }

    return res;
  }

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param context  The data context
   * @param count    The number of time to repeat the change
   * @param rawCount The actual count entered by the user
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  public boolean changeMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument) {
    // TODO: Hack - find better way to do this exceptional case - at least make constants out of these strings

    // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
    String id = ActionManager.getInstance().getId(argument.getMotion().getAction());
    boolean kludge = false;
    boolean skipPunc = false;
    if (id.equals("VimMotionWordRight")) {
      if (EditorHelper.getFileSize(editor) > 0 &&
          !Character.isWhitespace(EditorHelper.getDocumentChars(editor).charAt(editor.getCaretModel().getOffset()))) {
        kludge = true;
        argument.getMotion().setAction(ActionManager.getInstance().getAction("VimMotionWordEndRight"));
        argument.getMotion().setFlags(Command.FLAG_MOT_INCLUSIVE);
      }
    }
    else if (id.equals("VimMotionBigWordRight")) {
      if (EditorHelper.getFileSize(editor) > 0 &&
          !Character.isWhitespace(EditorHelper.getDocumentChars(editor).charAt(editor.getCaretModel().getOffset()))) {
        kludge = true;
        skipPunc = true;
        argument.getMotion().setAction(ActionManager.getInstance().getAction("VimMotionBigWordEndRight"));
        argument.getMotion().setFlags(Command.FLAG_MOT_INCLUSIVE);
      }
    }
    else if (id.equals("VimMotionCamelRight")) {
      if (EditorHelper.getFileSize(editor) > 0 &&
          !Character.isWhitespace(EditorHelper.getDocumentChars(editor).charAt(editor.getCaretModel().getOffset()))) {
        kludge = true;
        argument.getMotion().setAction(ActionManager.getInstance().getAction("VimMotionCamelEndRight"));
        argument.getMotion().setFlags(Command.FLAG_MOT_INCLUSIVE);
      }
    }

    if (kludge) {
      int pos = editor.getCaretModel().getOffset();
      int size = EditorHelper.getFileSize(editor);
      int cnt = count * argument.getMotion().getCount();
      int pos1 = SearchHelper.findNextWordEnd(EditorHelper.getDocumentChars(editor), pos, size, cnt, skipPunc, false, false);
      int pos2 = SearchHelper.findNextWordEnd(EditorHelper.getDocumentChars(editor), pos1, size, -cnt, skipPunc, false, false);
      if (logger.isDebugEnabled()) {
        logger.debug("pos=" + pos);
        logger.debug("pos1=" + pos1);
        logger.debug("pos2=" + pos2);
        logger.debug("count=" + count);
        logger.debug("arg.count=" + argument.getMotion().getCount());
      }
      if (pos2 == pos) {
        if (count > 1) {
          count--;
          rawCount--;
        }
        else if (argument.getMotion().getCount() > 1) {
          argument.getMotion().setCount(argument.getMotion().getCount() - 1);
        }
        else {
          argument.getMotion().setFlags(Command.FLAG_MOT_EXCLUSIVE);
        }
      }
    }

    boolean res = deleteMotion(editor, context, count, rawCount, argument, true);
    if (res) {
      insertBeforeCursor(editor, context);
    }

    return res;
  }

  public boolean blockInsert(Editor editor, DataContext context, TextRange range, boolean append) {
    LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
    int lines = range.size();
    int line = start.line;
    int col = start.column;
    if (!range.isMultiple()) {
      col = 0;
    }
    else if (append) {
      col += range.getMaxLength();
      if (EditorData.getLastColumn(editor) == MotionGroup.LAST_COLUMN) {
        col = MotionGroup.LAST_COLUMN;
      }
    }

    int len = EditorHelper.getLineLength(editor, line);
    if (col < MotionGroup.LAST_COLUMN && len < col) {
      String pad = EditorHelper.pad(editor, line, col);
      int off = editor.getDocument().getLineEndOffset(line);
      insertText(editor, context, off, pad);
    }

    if (range.isMultiple() || !append) {
      editor.getCaretModel().moveToOffset(editor.logicalPositionToOffset(new LogicalPosition(line, col)));
    }
    if (range.isMultiple()) {
      setInsertRepeat(lines, col, append);
    }
    if (range.isMultiple() || !append) {
      insertBeforeCursor(editor, context);
    }
    else {
      insertAfterCursor(editor, context);
    }

    return true;
  }

  /**
   * Deletes the range of text and enters insert mode
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param range   The range to change
   * @param type    The type of the range (FLAG_MOT_LINEWISE, FLAG_MOT_CHARACTERWISE)
   * @return true if able to delete the range, false if not
   */
  public boolean changeRange(Editor editor, DataContext context, TextRange range, int type) {
    int col = 0;
    int lines = 0;
    if (type == Command.FLAG_MOT_BLOCKWISE) {
      lines = range.size();
      col = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      if (EditorData.getLastColumn(editor) == MotionGroup.LAST_COLUMN) {
        col = MotionGroup.LAST_COLUMN;
      }
    }
    boolean after = range.getEndOffset() >= EditorHelper.getFileSize(editor);
    boolean res = deleteRange(editor, context, range, type, true);
    if (res) {
      if (type == Command.FLAG_MOT_LINEWISE) {
        if (after) {
          insertNewLineBelow(editor, context);
        }
        else {
          insertNewLineAbove(editor, context);
        }
      }
      else {
        if (type == Command.FLAG_MOT_BLOCKWISE) {
          setInsertRepeat(lines, col, false);
        }
        insertBeforeCursor(editor, context);
      }
    }

    return res;
  }

  /**
   * Toggles the case of count characters
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param count   The number of characters to change
   * @return true if able to change count characters
   */
  public boolean changeCaseToggleCharacter(Editor editor, DataContext context, int count) {
    int offset = CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, count, true);
    if (offset == -1) {
      return false;
    }
    else {
      changeCase(editor, context, editor.getCaretModel().getOffset(), offset, CharacterHelper.CASE_TOGGLE);

      offset = EditorHelper.normalizeOffset(editor, offset, false);
      MotionGroup.moveCaret(editor, context, offset);

      return true;
    }
  }

  /**
   * Changes the case of all the character moved over by the motion argument.
   *
   * @param editor   The editor to change
   * @param context  The data context
   * @param count    The number of times to repeat the change
   * @param rawCount The actual count entered by the user
   * @param type     The case change type (TOGGLE, UPPER, LOWER)
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  public boolean changeCaseMotion(Editor editor, DataContext context, int count, int rawCount, char type, Argument argument) {
    TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, true, false);

    return changeCaseRange(editor, context, range, type);
  }

  /**
   * Changes the case of all the characters in the range
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param range   The range to change
   * @param type    The case change type (TOGGLE, UPPER, LOWER)
   * @return true if able to delete the text, false if not
   */
  public boolean changeCaseRange(Editor editor, DataContext context, TextRange range, char type) {
    if (range == null) {
      return false;
    }
    else {
      int[] starts = range.getStartOffsets();
      int[] ends = range.getEndOffsets();
      for (int i = ends.length - 1; i >= 0; i--) {
        changeCase(editor, context, starts[i], ends[i], type);
      }
      MotionGroup.moveCaret(editor, context, range.getStartOffset());

      return true;
    }
  }

  /**
   * This performs the actual case change.
   *
   * @param editor  The editor to change
   * @param context The data context
   * @param start   The start offset to change
   * @param end     The end offset to change
   * @param type    The type of change (TOGGLE, UPPER, LOWER)
   */
  private void changeCase(Editor editor, DataContext context, int start, int end, char type) {
    if (start > end) {
      int t = end;
      end = start;
      start = t;
    }

    CharSequence chars = EditorHelper.getDocumentChars(editor);
    for (int i = start; i < end; i++) {
      if (i >= chars.length()) {
        break;
      }

      char ch = CharacterHelper.changeCase(chars.charAt(i), type);
      if (ch != chars.charAt(i)) {
        replaceText(editor, context, i, i + 1, Character.toString(ch));
      }
    }
  }

  public void autoIndentLines(Editor editor, DataContext context, int lines) {
    KeyHandler.executeAction("OrigAutoIndentLines", context);
  }

  public void indentLines(Editor editor, DataContext context, int lines, int dir) {
    int cnt = 1;
    if (CommandState.inInsertMode(editor)) {
      if (strokes.size() > 0) {
        Object stroke = strokes.get(strokes.size() - 1);
        if (stroke instanceof Character) {
          Character key = (Character)stroke;
          if (key == '0') {
            deleteCharacter(editor, context, -1);
            cnt = 99;
          }
        }
      }
    }

    int start = editor.getCaretModel().getOffset();
    int end = CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(editor, lines - 1, false);

    indentRange(editor, context, new TextRange(start, end), cnt, dir);
  }

  public void indentMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument, int dir) {
    TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, false, false);

    indentRange(editor, context, range, 1, dir);
  }

  public void indentRange(Editor editor, DataContext context, TextRange range, int count, int dir) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
    }
    if (range == null) return;

    Project proj = PlatformDataKeys.PROJECT.getData(context); // API change - don't merge
    int tabSize = 8;
    int indentSize = 8;
    boolean useTabs = true;
    VirtualFile file = EditorData.getVirtualFile(editor);
    if (file != null) {
      FileType type = FileTypeManager.getInstance().getFileTypeByFile(file);
      CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(proj);
      tabSize = settings.getTabSize(type);
      indentSize = settings.getIndentSize(type);
      useTabs = settings.useTabCharacter(type);
    }

    int sline = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    int eline = editor.offsetToLogicalPosition(range.getEndOffset()).line;
    int eoff = EditorHelper.getLineStartForOffset(editor, range.getEndOffset());
    if (eoff == range.getEndOffset()) {
      eline--;
    }

    if (range.isMultiple()) {
      int col = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      int size = indentSize * count;
      if (dir == 1) {
        // Right shift blockwise selection
        StringBuffer space = new StringBuffer();
        int tabCnt = 0;
        int spcCnt;
        if (useTabs) {
          tabCnt = size / tabSize;
          spcCnt = size % tabSize;
        }
        else {
          spcCnt = size;
        }

        for (int i = 0; i < tabCnt; i++) {
          space.append('\t');
        }
        for (int i = 0; i < spcCnt; i++) {
          space.append(' ');
        }

        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(editor, l);
          if (len > col) {
            LogicalPosition spos = new LogicalPosition(l, col);
            insertText(editor, context, editor.logicalPositionToOffset(spos), space.toString());
          }
        }
      }
      else {
        // Left shift blockwise selection
        CharSequence chars = EditorHelper.getDocumentChars(editor);
        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(editor, l);
          if (len > col) {
            LogicalPosition spos = new LogicalPosition(l, col);
            LogicalPosition epos = new LogicalPosition(l, col + size - 1);
            int wsoff = editor.logicalPositionToOffset(spos);
            int weoff = editor.logicalPositionToOffset(epos);
            int pos;
            for (pos = wsoff; pos <= weoff; pos++) {
              if (CharacterHelper.charType(chars.charAt(pos), false) != CharacterHelper.TYPE_SPACE) {
                break;
              }
            }
            if (pos > wsoff) {
              deleteText(editor, context, new TextRange(wsoff, pos), 0);
            }
          }
        }
      }
    }
    else {
      // Shift non-blockwise selection
      for (int l = sline; l <= eline; l++) {
        int soff = EditorHelper.getLineStartOffset(editor, l);
        int woff = CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor, l);
        int col = editor.offsetToVisualPosition(woff).column;
        int newCol = Math.max(0, col + dir * indentSize * count);
        if (dir == 1 || col > 0) {
          StringBuffer space = new StringBuffer();
          int tabCnt = 0;
          int spcCnt;
          if (useTabs) {
            tabCnt = newCol / tabSize;
            spcCnt = newCol % tabSize;
          }
          else {
            spcCnt = newCol;
          }

          for (int i = 0; i < tabCnt; i++) {
            space.append('\t');
          }
          for (int i = 0; i < spcCnt; i++) {
            space.append(' ');
          }

          replaceText(editor, context, soff, woff, space.toString());
        }
      }
    }

    if (!CommandState.inInsertMode(editor)) {
      if (!range.isMultiple()) {
        MotionGroup.moveCaret(editor, context,
                              CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor, sline));
      }
      else {
        MotionGroup.moveCaret(editor, context, range.getStartOffset());
      }
    }

    EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
  }

  /**
   * Insert text into the document
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param start   The starting offset to insert at
   * @param str     The text to insert
   */
  public void insertText(Editor editor, DataContext context, int start, String str) {
    editor.getDocument().insertString(start, str);
    editor.getCaretModel().moveToOffset(start + str.length());

    CommandGroups.getInstance().getMark().setMark(editor, context, '.', start);
    //CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
    //CommandGroups.getInstance().getMark().setMark(editor, context, ']', start + str.length());
  }

  /**
   * Delete text from the document. This will fail if being asked to store the deleted text into a read-only
   * register.
   *
   * @param editor  The editor to delete from
   * @param context The data context
   * @param range   The range to delete
   * @param type    The type of deletion (FLAG_MOT_LINEWISE, FLAG_MOT_CHARACTERWISE)
   * @return true if able to delete the text, false if not
   */
  private boolean deleteText(Editor editor, DataContext context, TextRange range, int type) {
    // Fix for http://youtrack.jetbrains.net/issue/VIM-35
    if (!range.normalize(EditorHelper.getFileSize(editor, true))) {
      return false;
    }

    if (type == 0 || CommandGroups.getInstance().getRegister().storeText(editor, context, range, type, true, false)) {
      for (int i = range.size() - 1; i >= 0; i--) {
        editor.getDocument().deleteString(range.getStartOffsets()[i], range.getEndOffsets()[i]);
      }

      if (type != 0) {
        int start = range.getStartOffset();
        CommandGroups.getInstance().getMark().setMark(editor, context, '.', start);
        CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
        CommandGroups.getInstance().getMark().setMark(editor, context, ']', start);
      }

      return true;
    }

    return false;
  }

  /**
   * Replace text in the editor
   *
   * @param editor  The editor to replace text in
   * @param context The data context
   * @param start   The start offset to change
   * @param end     The end offset to change
   * @param str     The new text
   */
  private void replaceText(Editor editor, DataContext context, int start, int end, String str) {
    editor.getDocument().replaceString(start, end, str);

    CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
    CommandGroups.getInstance().getMark().setMark(editor, context, ']', start + str.length());
    CommandGroups.getInstance().getMark().setMark(editor, context, '.', start + str.length());
  }

  private static void resetCursor(Editor editor, boolean insert) {
    Document doc = editor.getDocument();
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (vf != null) {
      resetCursor(vf, EditorData.getProject(editor), insert);
    }
    else {
      editor.getSettings().setBlockCursor(!insert);
    }
  }

  private static void resetCursor(VirtualFile virtualFile, Project proj, boolean insert) {
    logger.debug("resetCursor");
    Document doc = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (doc == null) return; // Must be no text editor (such as image)
    Editor[] editors = EditorFactory.getInstance().getEditors(doc, proj);
    if (logger.isDebugEnabled()) {
      logger.debug("There are " + editors.length + " editors for virtual file " + virtualFile.getName());
    }
    for (Editor editor : editors) {
      editor.getSettings().setBlockCursor(!insert);
    }
  }

  public boolean changeNumber(final Editor editor, final DataContext context, final int count) {
    final BoundListOption nf = (BoundListOption)Options.getInstance().getOption("nrformats");
    final boolean alpha = nf.contains("alpha");
    final boolean hex = nf.contains("hex");
    final boolean octal = nf.contains("octal");

    final TextRange range = SearchHelper.findNumberUnderCursor(editor, alpha, hex, octal);
    if (range == null) {
      logger.debug("no number on line");
      return false;
    }
    else {
      String text = EditorHelper.getText(editor, range);
      if (logger.isDebugEnabled()) {
        logger.debug("found range " + range);
        logger.debug("text=" + text);
      }
      String number = text;
      if (text.length() == 0) {
        return false;
      }

      char ch = text.charAt(0);
      if (hex && text.toLowerCase().startsWith("0x")) {
        for (int i = text.length() - 1; i >= 2; i--) {
          int index = "abcdefABCDEF".indexOf(text.charAt(i));
          if (index >= 0) {
            lastLower = index < 6;
            break;
          }
        }

        int num = (int)Long.parseLong(text.substring(2), 16);
        num += count;
        number = Integer.toHexString(num);
        number = StringHelper.pad(number, text.length() - 2, '0');

        if (!lastLower) {
          number = number.toUpperCase();
        }

        number = text.substring(0, 2) + number;
      }
      else if (octal && text.startsWith("0") && text.length() > 1) {
        int num = (int)Long.parseLong(text, 8);
        num += count;
        number = Integer.toOctalString(num);
        number = "0" + StringHelper.pad(number, text.length() - 1, '0');
      }
      else if (alpha && Character.isLetter(ch)) {
        ch += count;
        if (Character.isLetter(ch)) {
          number = "" + ch;
        }
      }
      else if (ch == '-' || Character.isDigit(ch)) {
        boolean pad = ch == '0';
        int len = text.length();
        if (ch == '-' && text.charAt(1) == '0') {
          pad = true;
          len--;
        }

        int num = Integer.parseInt(text);
        num += count;
        number = Integer.toString(num);

        if (!octal && pad) {
          boolean neg = false;
          if (number.charAt(0) == '-') {
            neg = true;
            number = number.substring(1);
          }
          number = StringHelper.pad(number, len, '0');
          if (neg) {
            number = "-" + number;
          }
        }
      }

      if (!text.equals(number)) {
        replaceText(editor, context, range.getStartOffset(), range.getEndOffset(), number);
        editor.getCaretModel().moveToOffset(range.getStartOffset() + number.length() - 1);
      }

      return true;
    }
  }

  private ArrayList strokes = new ArrayList();
  private ArrayList lastStrokes;
  private int insertStart;
  private Command lastInsert;
  private boolean inInsert;
  private int repeatLines;
  private int repeatColumn;
  private boolean repeatAppend;
  private boolean lastLower = true;

  private static Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}
