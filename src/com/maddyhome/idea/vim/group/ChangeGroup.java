/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.util.ArrayUtil;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.BoundListOption;
import com.maddyhome.idea.vim.option.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides all the insert/replace related functionality
 * TODO - change cursor for the different modes
 */
public class ChangeGroup {

  private static final int MAX_REPEAT_CHARS_COUNT = 10000;

  private static final String VIM_MOTION_BIG_WORD_RIGHT = "VimMotionBigWordRight";
  private static final String VIM_MOTION_WORD_RIGHT = "VimMotionWordRight";
  private static final String VIM_MOTION_CAMEL_RIGHT = "VimMotionCamelRight";
  private static final String VIM_MOTION_WORD_END_RIGHT = "VimMotionWordEndRight";
  private static final String VIM_MOTION_BIG_WORD_END_RIGHT = "VimMotionBigWordEndRight";
  private static final String VIM_MOTION_CAMEL_END_RIGHT = "VimMotionCamelEndRight";

  /**
   * Creates the group
   */
  public ChangeGroup() {
    // We want to know when a user clicks the mouse somewhere in the editor so we can clear any
    // saved text for the current insert mode.
    final EventFacade eventFacade = EventFacade.getInstance();

    eventFacade.addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        eventFacade.addEditorMouseListener(editor, listener);
        EditorData.setChangeGroup(editor, true);
      }

      public void editorReleased(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        if (EditorData.getChangeGroup(editor)) {
          eventFacade.removeEditorMouseListener(editor, listener);
          EditorData.setChangeGroup(editor, false);
        }
      }

      @NotNull
      private final EditorMouseAdapter listener = new EditorMouseAdapter() {
        public void mouseClicked(@NotNull EditorMouseEvent event) {
          Editor editor = event.getEditor();
          if (!VimPlugin.isEnabled()) {
            return;
          }

          if (CommandState.inInsertMode(editor)) {
            clearStrokes(editor);
          }
        }
      };
    }, ApplicationManager.getApplication());
  }

  private void setInsertRepeat(int lines, int column, boolean append) {
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
  public void insertBeforeCursor(@NotNull Editor editor, @NotNull DataContext context) {
    initInsert(editor, context, CommandState.Mode.INSERT);
  }

  /**
   * Begin insert before the first non-blank on the current line
   *
   * @param editor The editor to insert into
   */
  public void insertBeforeFirstNonBlank(@NotNull Editor editor, @NotNull DataContext context) {
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));
    }
    initInsert(editor, context, CommandState.Mode.INSERT);
  }

  /**
   * Begin insert before the start of the current line
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertLineStart(@NotNull Editor editor, @NotNull DataContext context) {
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
    }
    initInsert(editor, context, CommandState.Mode.INSERT);
  }

  /**
   * Begin insert after the cursor position
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertAfterCursor(@NotNull Editor editor, @NotNull DataContext context) {
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretHorizontal(editor, caret, 1, true));
    }
    initInsert(editor, context, CommandState.Mode.INSERT);
  }

  public void insertAfterLineEnd(@NotNull Editor editor, @NotNull DataContext context) {
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    }
    initInsert(editor, context, CommandState.Mode.INSERT);
  }

  /**
   * Begin insert before the current line by creating a new blank line above the current line
   * for all carets
   *
   * @param editor The editor to insert into
   */
  public void insertNewLineAbove(@NotNull final Editor editor, @NotNull DataContext context) {
    if (editor.isOneLineMode()) return;

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (caret.getVisualPosition().line == 0) {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
        CaretData.setWasInFirstLine(caret, true);
      }
      else {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
      }
    }

    initInsert(editor, context, CommandState.Mode.INSERT);
    runEnterAction(editor, context);

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (CaretData.wasInFirstLine(caret)) {
        CaretData.setWasInFirstLine(caret, false);
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
      }
    }
  }

  /**
   * Inserts a new line above the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert above
   * @param col    The column to indent to
   */
  private void insertNewLineAbove(@NotNull Editor editor, @NotNull Caret caret, int col) {
    if (editor.isOneLineMode()) return;

    if (caret.getVisualPosition().line == 0) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
      CaretData.setWasInFirstLine(caret, true);
    }
    else {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    }

    EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + StringUtil.repeat(" ", col));

    if (CaretData.wasInFirstLine(caret)) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
      CaretData.setWasInFirstLine(caret, false);
    }
  }

  /**
   * Begin insert after the current line by creating a new blank line below the current line
   * for all carets
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertNewLineBelow(@NotNull final Editor editor, @NotNull final DataContext context) {
    if (editor.isOneLineMode()) return;

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    }

    initInsert(editor, context, CommandState.Mode.INSERT);
    runEnterAction(editor, context);
  }

  /**
   * Inserts a new line below the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert after
   * @param col    The column to indent to
   */
  private void insertNewLineBelow(@NotNull Editor editor, @NotNull Caret caret, int col) {
    if (editor.isOneLineMode()) return;

    MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + StringUtil.repeat(" ", col));
  }

  private void runEnterAction(Editor editor, @NotNull DataContext context) {
    CommandState state = CommandState.getInstance(editor);
    if (state.getMode() != CommandState.Mode.REPEAT) {
      final ActionManager actionManager = ActionManager.getInstance();
      final AnAction action = actionManager.getAction("EditorEnter");
      if (action != null) {
        strokes.add(action);
        KeyHandler.executeAction(action, context);
      }
    }
  }

  /**
   * Begin insert at the location of the previous insert
   *
   * @param editor The editor to insert into
   */
  public void insertAtPreviousInsert(@NotNull Editor editor, @NotNull DataContext context) {
    editor.getCaretModel().removeSecondaryCarets();

    final Caret caret = editor.getCaretModel().getPrimaryCaret();
    final int offset = VimPlugin.getMotion().moveCaretToMark(editor, '^', false);
    if (offset != -1) {
      MotionGroup.moveCaret(editor, caret, offset);
    }

    insertAfterCursor(editor, context);
  }

  /**
   * Inserts previously inserted text
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param exit    true if insert mode should be exited after the insert, false should stay in insert mode
   */
  public void insertPreviousInsert(@NotNull Editor editor, @NotNull DataContext context, boolean exit) {
    repeatInsertText(editor, context, 1);
    if (exit) {
      processEscape(editor, context);
    }
  }

  /**
   * Inserts the contents of the specified register
   *
   * @param editor  The editor to insert the text into
   * @param context The data context
   * @param key     The register name
   * @return true if able to insert the register contents, false if not
   */
  public boolean insertRegister(@NotNull Editor editor, @NotNull DataContext context, char key) {
    final Register register = VimPlugin.getRegister().getRegister(key);
    if (register != null) {
      final String text = register.getText();
      if (text != null) {
        final int length = text.length();
        for (int i = 0; i < length; i++) {
          processKey(editor, context, KeyStroke.getKeyStroke(text.charAt(i)));
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Inserts the character above/below the cursor at the cursor location
   *
   * @param editor  The editor to insert into
   * @param caret   The caret to insert after
   * @param dir     1 for getting from line below cursor, -1 for getting from line above cursor
   * @return true if able to get the character and insert it, false if not
   */
  public boolean insertCharacterAroundCursor(@NotNull Editor editor, @NotNull Caret caret, int dir) {
    boolean res = false;

    VisualPosition vp = caret.getVisualPosition();
    vp = new VisualPosition(vp.line + dir, vp.column);
    int len = EditorHelper.getLineLength(editor, EditorHelper.visualLineToLogicalLine(editor, vp.line));
    if (vp.column < len) {
      int offset = EditorHelper.visualPositionToOffset(editor, vp);
      char ch = editor.getDocument().getCharsSequence().charAt(offset);
      editor.getDocument().insertString(caret.getOffset(), Character.toString(ch));
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretHorizontal(editor, caret, 1, true));
      res = true;
    }

    return res;
  }

  /**
   * If the cursor is currently after the start of the current insert this deletes all the newly inserted text.
   * Otherwise it deletes all text from the cursor back to the first non-blank in the line.
   *
   * @param editor The editor to delete the text from
   * @param caret  The caret on which the action is performed
   * @return true if able to delete the text, false if not
   */
  public boolean insertDeleteInsertedText(@NotNull Editor editor, @NotNull Caret caret) {
    int deleteTo = CaretData.getInsertStart(caret);
    int offset = caret.getOffset();
    if (offset == deleteTo) {
      deleteTo = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret);
    }

    if (deleteTo != -1) {
      deleteRange(editor, caret, new TextRange(deleteTo, offset), SelectionType.CHARACTER_WISE, false);

      return true;
    }

    return false;
  }

  /**
   * Deletes the text from the cursor to the start of the previous word
   *
   * @param editor The editor to delete the text from
   * @return true if able to delete text, false if not
   */
  public boolean insertDeletePreviousWord(@NotNull Editor editor, @NotNull Caret caret) {
    final int deleteTo = VimPlugin.getMotion().moveCaretToNextWord(editor, caret, -1, false);
    if (deleteTo == -1) {
      return false;
    }
    final TextRange range = new TextRange(deleteTo, caret.getOffset());
    deleteRange(editor, caret, range, SelectionType.CHARACTER_WISE, true);
    return true;
  }

  /**
   * Begin insert/replace mode
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param mode    The mode - indicate insert or replace
   */
  private void initInsert(@NotNull Editor editor, @NotNull DataContext context, @NotNull CommandState.Mode mode) {
    final CommandState state = CommandState.getInstance(editor);

    final CaretModel caretModel = editor.getCaretModel();
    for (Caret caret : caretModel.getAllCarets()) {
      CaretData.setInsertStart(caret, caret.getOffset());
      if (caret == caretModel.getPrimaryCaret()) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_START, caret.getOffset());
      }
    }

    final Command cmd = state.getCommand();
    if (cmd != null && state.getMode() == CommandState.Mode.REPEAT) {
      if (mode == CommandState.Mode.REPLACE) {
        processInsert(editor, context);
      }
      if ((cmd.getFlags() & Command.FLAG_NO_REPEAT) != 0) {
        repeatInsert(editor, context, 1, false);
      }
      else {
        repeatInsert(editor, context, cmd.getCount(), false);
      }
      if (mode == CommandState.Mode.REPLACE) {
        processInsert(editor, context);
      }
    }
    else {
      lastInsert = cmd;
      strokes.clear();
      repeatCharsCount = 0;
      final EventFacade eventFacade = EventFacade.getInstance();
      if (document != null && documentListener != null) {
        eventFacade.removeDocumentListener(document, documentListener);
      }
      document = editor.getDocument();
      documentListener = new InsertActionsDocumentListener();
      eventFacade.addDocumentListener(document, documentListener);
      oldOffset = -1;
      inInsert = true;
      if (mode == CommandState.Mode.REPLACE) {
        processInsert(editor, context);
      }
      state.pushState(mode, CommandState.SubMode.NONE, MappingMode.INSERT);

      resetCursor(editor, true);
    }
  }

  /**
   * Performs a mode switch after change action
   *
   * @param editor   The editor to switch mode in
   * @param context  The data context
   * @param toSwitch The mode to switch to
   */
  public void processPostChangeModeSwitch(@NotNull Editor editor, @NotNull DataContext context,
                                          @NotNull CommandState.Mode toSwitch) {
    if (toSwitch == CommandState.Mode.INSERT) {
      initInsert(editor, context, toSwitch);
    }
  }

  private class InsertActionsDocumentListener implements DocumentListener {
    @Override
    public void documentChanged(@NotNull DocumentEvent e) {
      final String newFragment = e.getNewFragment().toString();
      final String oldFragment = e.getOldFragment().toString();
      final int newFragmentLength = newFragment.length();
      final int oldFragmentLength = oldFragment.length();

      // Repeat buffer limits
      if (repeatCharsCount > MAX_REPEAT_CHARS_COUNT) {
        return;
      }

      // <Enter> is added to strokes as an action during processing in order to indent code properly in the repeat
      // command
      if (newFragment.startsWith("\n") && newFragment.trim().isEmpty()) {
        strokes.addAll(getAdjustCaretActions(e));
        oldOffset = -1;
        return;
      }

      // Ignore multi-character indents as they should be inserted automatically while repeating <Enter> actions
      if (newFragmentLength > 1 && newFragment.trim().isEmpty()) {
        return;
      }

      strokes.addAll(getAdjustCaretActions(e));

      if (oldFragmentLength > 0) {
        final AnAction editorDelete = ActionManager.getInstance().getAction("EditorDelete");
        for (int i = 0; i < oldFragmentLength; i++) {
          strokes.add(editorDelete);
        }
      }

      if (newFragmentLength > 0) {
        strokes.add(newFragment.toCharArray());
      }
      repeatCharsCount += newFragmentLength;
      oldOffset = e.getOffset() + newFragmentLength;
    }

    @NotNull
    private List<AnAction> getAdjustCaretActions(@NotNull DocumentEvent e) {
      final int delta = e.getOffset() - oldOffset;
      if (oldOffset >= 0 && delta != 0) {
        final List<AnAction> positionCaretActions = new ArrayList<>();
        final String motionName = delta < 0 ? "VimMotionLeft" : "VimMotionRight";
        final AnAction action = ActionManager.getInstance().getAction(motionName);
        final int count = Math.abs(delta);
        for (int i = 0; i < count; i++) {
          positionCaretActions.add(action);
        }
        return positionCaretActions;
      }
      return Collections.emptyList();
    }
  }

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  private void repeatInsert(@NotNull Editor editor, @NotNull DataContext context, int count, boolean started) {
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (repeatLines > 0) {
        final int visualLine = caret.getVisualPosition().line;
        final int logicalLine = caret.getLogicalPosition().line;
        final int position = editor.logicalPositionToOffset(new LogicalPosition(logicalLine, repeatColumn));

        for (int i = 0; i < repeatLines; i++) {
          if (repeatAppend && repeatColumn < MotionGroup.LAST_COLUMN &&
              EditorHelper.getVisualLineLength(editor, visualLine + i) < repeatColumn) {
            final String pad = EditorHelper.pad(editor, context, logicalLine + i, repeatColumn);
            if (pad.length() > 0) {
              final int offset = editor.getDocument().getLineEndOffset(logicalLine + i);
              caret.moveToOffset(offset);
              insertText(editor, caret, pad);
            }
          }
          if (repeatColumn >= MotionGroup.LAST_COLUMN) {
            caret.moveToOffset(VimPlugin.getMotion().moveCaretToLineEnd(editor, logicalLine + i, true));
            repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
          }
          else if (EditorHelper.getVisualLineLength(editor, visualLine + i) >= repeatColumn) {
            caret.moveToVisualPosition(new VisualPosition(visualLine + i, repeatColumn));
            repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
          }
        }

        MotionGroup.moveCaret(editor, caret, position);
      }
      else {
        repeatInsertText(editor, context, count);
        final int position = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -1, false);

        MotionGroup.moveCaret(editor, caret, position);
      }
    }

    repeatLines = 0;
    repeatColumn = 0;
    repeatAppend = false;
  }

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  private void repeatInsertText(@NotNull Editor editor, @NotNull DataContext context, int count) {
    if (lastStrokes == null) {
      return;
    }

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      for (int i = 0; i < count; i++) {
        for (Object lastStroke : lastStrokes) {
          if (lastStroke instanceof AnAction) {
            KeyHandler.executeAction((AnAction)lastStroke, context);
            strokes.add(lastStroke);
          }
          else if (lastStroke instanceof char[]) {
            final char[] chars = (char[])lastStroke;
            insertText(editor, caret, new String(chars));
          }
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
  public void processEscape(@NotNull Editor editor, @NotNull DataContext context) {
    int cnt = lastInsert != null ? lastInsert.getCount() : 0;
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      KeyHandler.executeAction("VimInsertReplaceToggle", context);
    }

    if (lastInsert != null && (lastInsert.getFlags() & Command.FLAG_NO_REPEAT) != 0) {
      cnt = 1;
    }

    if (document != null && documentListener != null) {
      EventFacade.getInstance().removeDocumentListener(document, documentListener);
      documentListener = null;
    }

    lastStrokes = new ArrayList<>(strokes);

    repeatInsert(editor, context, cnt == 0 ? 0 : cnt - 1, true);

    final MarkGroup markGroup = VimPlugin.getMark();
    final int offset = editor.getCaretModel().getPrimaryCaret().getOffset();
    markGroup.setMark(editor, '^', offset);
    markGroup.setMark(editor, MarkGroup.MARK_CHANGE_END, offset);
    markGroup.setMark(editor, MarkGroup.MARK_CHANGE_POS, offset);
    CommandState.getInstance(editor).popState();

    if (!CommandState.inInsertMode(editor)) {
      resetCursor(editor, false);
    }
  }

  /**
   * Processes the Enter key by running the first successful action registered for "ENTER" keystroke.
   * <p>
   * If this is REPLACE mode we need to turn off OVERWRITE before and then turn OVERWRITE back on after sending the
   * "ENTER" key.
   *
   * @param editor  The editor to press "Enter" in
   * @param context The data context
   */
  public void processEnter(@NotNull Editor editor, @NotNull DataContext context) {
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      KeyHandler.executeAction("EditorToggleInsertState", context);
    }
    final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    final List<AnAction> actions = VimPlugin.getKey().getActions(editor.getComponent(), enterKeyStroke);
    for (AnAction action : actions) {
      if (KeyHandler.executeAction(action, context)) {
        break;
      }
    }
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      KeyHandler.executeAction("EditorToggleInsertState", context);
    }
  }

  /**
   * Processes the user pressing the Insert key while in INSERT or REPLACE mode. This simply toggles the
   * Insert/Overwrite state which updates the status bar.
   *
   * @param editor  The editor to toggle the state in
   * @param context The data context
   */
  public void processInsert(Editor editor, @NotNull DataContext context) {
    KeyHandler.executeAction("EditorToggleInsertState", context);
    CommandState.getInstance(editor).toggleInsertOverwrite();
    inInsert = !inInsert;
  }

  /**
   * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
   * return to INSERT or REPLACE mode.
   *
   * @param editor The editor to put into NORMAL mode for one command
   */
  public void processSingleCommand(@NotNull Editor editor) {
    CommandState.getInstance(editor).pushState(CommandState.Mode.COMMAND, CommandState.SubMode.SINGLE_COMMAND,
                                               MappingMode.NORMAL);
    clearStrokes(editor);
  }

  /**
   * Drafts an {@link ActionPlan} for preemptive rendering before "regular" keystroke processing in insert/replace mode.
   * <p>
   * Like {@link #processKey(Editor, DataContext, KeyStroke)}, delegates the task to the original handler.
   *
   * @param editor  The editor the character was typed into
   * @param context The data context
   * @param key     The user entered keystroke
   * @param plan    the current action plan draft
   */
  public void beforeProcessKey(@NotNull final Editor editor, @NotNull final DataContext context,
                               @NotNull final KeyStroke key, @NotNull ActionPlan plan) {

    final TypedActionHandler originalHandler = KeyHandler.getInstance().getOriginalHandler();

    if (originalHandler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)originalHandler).beforeExecute(editor, key.getKeyChar(), context, plan);
    }
  }

  /**
   * This processes all "regular" keystrokes entered while in insert/replace mode
   *
   * @param editor  The editor the character was typed into
   * @param context The data context
   * @param key     The user entered keystroke
   * @return true if this was a regular character, false if not
   */
  public boolean processKey(@NotNull final Editor editor, @NotNull final DataContext context,
                            @NotNull final KeyStroke key) {
    if (logger.isDebugEnabled()) {
      logger.debug("processKey(" + key + ")");
    }

    if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
      final Document doc = editor.getDocument();
      CommandProcessor.getInstance().executeCommand(editor.getProject(),
                                                    () -> ApplicationManager.getApplication().runWriteAction(
                                                        () -> KeyHandler.getInstance().getOriginalHandler().execute(
                                                            editor, key.getKeyChar(), context)), "", doc,
                                                    UndoConfirmationPolicy.DEFAULT, doc);

      return true;
    }

    return false;
  }

  /**
   * This processes all keystrokes in Insert/Replace mode that were converted into Commands. Some of these
   * commands need to be saved off so the inserted/replaced text can be repeated properly later if needed.
   *
   * @param editor The editor the command was executed in
   * @param cmd    The command that was executed
   */
  public void processCommand(@NotNull Editor editor, @NotNull Command cmd) {
    // return value never used here
    if ((cmd.getFlags() & Command.FLAG_SAVE_STROKE) != 0) {
      strokes.add(cmd.getAction());
    }
    else if ((cmd.getFlags() & Command.FLAG_CLEAR_STROKES) != 0) {
      clearStrokes(editor);
    }
  }

  /**
   * Clears all the keystrokes from the current insert command
   *
   * @param editor The editor to clear strokes from.
   */
  private void clearStrokes(@NotNull Editor editor) {
    strokes.clear();
    repeatCharsCount = 0;
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      CaretData.setInsertStart(caret, caret.getOffset());
    }
  }

  /**
   * Deletes count character after the caret from the editor
   *
   * @param editor The editor to remove characters from
   * @param caret  The caret on which the operation is performed
   * @param count  The numbers of characters to delete.
   * @return true if able to delete, false if not
   */
  public boolean deleteCharacter(@NotNull Editor editor, @NotNull Caret caret, int count, boolean isChange) {
    final int endOffset = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, count, true);
    if (endOffset != -1) {
      final boolean res = deleteText(editor, new TextRange(caret.getOffset(), endOffset), SelectionType.CHARACTER_WISE);
      final int pos = caret.getOffset();
      final int norm = EditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, pos, isChange);
      if (norm != pos) {
        MotionGroup.moveCaret(editor, caret, norm);
      }

      return res;
    }

    return false;
  }

  public boolean deleteCharacter(@NotNull Editor editor, int count, boolean isChange) {
    final int caretCount = editor.getCaretModel().getCaretCount();
    final List<Integer> startOffsets = Lists.newArrayListWithCapacity(caretCount);
    final List<Integer> endOffsets = Lists.newArrayListWithCapacity(caretCount);
    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, count > 0 ? CaretOrder.DECREASING_OFFSET
                                                                                   : CaretOrder.INCREASING_OFFSET);
    boolean result = true;
    for (int i = 0; i < caretCount; i++) {
      final Caret caret = carets.get(i);
      final int endOffset = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, count, true);
      if (endOffset == -1) {
        result = false;
        continue;
      }

      final int startOffset = caret.getOffset();
      startOffsets.add(startOffset);
      endOffsets.add(endOffset);

      result = deleteText(editor, new TextRange(startOffset, endOffset), SelectionType.CHARACTER_WISE);

      final int normalizeOffset = EditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, startOffset,
                                                               isChange);
      if (normalizeOffset != startOffset) MotionGroup.moveCaret(editor, caret, normalizeOffset);
    }

    if (caretCount > 1 && result) {
      final TextRange range = new TextRange(ArrayUtil.toIntArray(startOffsets), ArrayUtil.toIntArray(endOffsets));
      VimPlugin.getRegister().storeText(editor, range,
                                        range.isMultiple() ? SelectionType.LINE_WISE : SelectionType.CHARACTER_WISE,
                                        true);
    }

    return result;
  }

  /**
   * Deletes count lines including the current line
   *
   * @param editor The editor to remove the lines from
   * @param count  The number of lines to delete
   * @return true if able to delete the lines, false if not
   */

  public boolean deleteLine(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int start = VimPlugin.getMotion().moveCaretToLineStart(editor, caret);
    int offset = Math.min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1,
                          EditorHelper.getFileSize(editor, true));
    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("offset=" + offset);
    }
    if (offset != -1) {
      boolean res = deleteText(editor, new TextRange(start, offset), SelectionType.LINE_WISE);
      if (res && caret.getOffset() >= EditorHelper.getFileSize(editor) && caret.getOffset() != 0) {
        MotionGroup.moveCaret(editor, caret,
                              VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor, caret, -1));
      }

      return res;
    }

    return false;
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down
   *
   * @param editor The editor to delete from
   * @param caret  Caret on the position to start
   * @param count  The number of lines affected
   * @return true if able to delete the text, false if not
   */
  public boolean deleteEndOfLine(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int offset = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true);
    if (offset != -1) {
      boolean res = deleteText(editor, new TextRange(caret.getOffset(), offset), SelectionType.CHARACTER_WISE);
      int pos = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -1, false);
      if (pos != -1) {
        MotionGroup.moveCaret(editor, caret, pos);
      }

      return res;
    }

    return false;
  }

  /**
   * Joins count lines together starting at the cursor. No count or a count of one still joins two lines.
   *
   * @param editor The editor to join the lines in
   * @param caret  The caret in the first line to be joined.
   * @param count  The number of lines to join
   * @param spaces If true the joined lines will have one space between them and any leading space on the second line
   *               will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  public boolean deleteJoinLines(@NotNull Editor editor, @NotNull Caret caret, int count, boolean spaces) {
    if (count < 2) count = 2;
    int lline = caret.getLogicalPosition().line;
    int total = EditorHelper.getLineCount(editor);
    //noinspection SimplifiableIfStatement
    if (lline + count > total) {
      return false;
    }

    return deleteJoinNLines(editor, caret, lline, count, spaces);
  }

  /**
   * Joins all the lines selected by the current visual selection.
   *
   * @param editor The editor to join the lines in
   * @param caret  The caret to be moved after joining
   * @param range  The range of the visual selection
   * @param spaces If true the joined lines will have one space between them and any leading space on the second line
   *               will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  public boolean deleteJoinRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range,
                                 boolean spaces) {
    int startLine = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    int endLine = editor.offsetToLogicalPosition(range.getEndOffset()).line;
    int count = endLine - startLine + 1;
    if (count < 2) count = 2;

    return deleteJoinNLines(editor, caret, startLine, count, spaces);
  }

  /**
   * This does the actual joining of the lines
   *
   * @param editor    The editor to join the lines in
   * @param caret     The caret on the starting line (to be moved)
   * @param startLine The starting logical line
   * @param count     The number of lines to join including startLine
   * @param spaces    If true the joined lines will have one space between them and any leading space on the second line
   *                  will be removed. If false, only the newline is removed to join the lines.
   * @return true if able to join the lines, false if not
   */
  private boolean deleteJoinNLines(@NotNull Editor editor, @NotNull Caret caret, int startLine, int count,
                                   boolean spaces) {
    // start my moving the cursor to the very end of the first line
    MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, startLine, true), true);
    for (int i = 1; i < count; i++) {
      int start = VimPlugin.getMotion().moveCaretToLineEnd(editor, caret);
      int trailingWhitespaceStart = VimPlugin.getMotion().moveCaretToLineEndSkipLeadingOffset(editor, caret, 0);
      boolean hasTrailingWhitespace = start != trailingWhitespaceStart + 1;

      MotionGroup.moveCaret(editor, caret, start, true);
      int offset;
      if (spaces) {
        offset = VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor, caret, 1);
      }
      else {
        offset = VimPlugin.getMotion().moveCaretToLineStart(editor, caret.getLogicalPosition().line + 1);
      }
      deleteText(editor, new TextRange(caret.getOffset(), offset), null);
      if (spaces && !hasTrailingWhitespace) {
        insertText(editor, caret, " ");
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -1, true), true);
      }
    }

    return true;
  }

  /**
   * Delete all text moved over by the supplied motion command argument.
   *
   * @param editor   The editor to delete the text from
   * @param caret    The caret on which the motion appears to be performed
   * @param context  The data context
   * @param count    The number of times to repeat the deletion
   * @param rawCount The actual count entered by the user
   * @param argument The motion command
   * @param isChange if from a change
   * @return true if able to delete the text, false if not
   */
  public boolean deleteMotion(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                              int rawCount, @NotNull final Argument argument, boolean isChange) {
    final TextRange range = getDeleteMotionRange(editor, caret, context, count, rawCount, argument);
    if (range == null) {
      return (EditorHelper.getFileSize(editor) == 0);
    }

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    final Command motion = argument.getMotion();
    if (motion == null) {
      return false;
    }
    if (!isChange && (motion.getFlags() & Command.FLAG_MOT_LINEWISE) == 0) {
      LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      LogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (start.line != end.line) {
        if (!SearchHelper.anyNonWhitespace(editor, range.getStartOffset(), -1) &&
            !SearchHelper.anyNonWhitespace(editor, range.getEndOffset(), 1)) {
          int flags = motion.getFlags();
          flags &= ~Command.FLAG_MOT_EXCLUSIVE;
          flags &= ~Command.FLAG_MOT_INCLUSIVE;
          flags |= Command.FLAG_MOT_LINEWISE;
          motion.setFlags(flags);
        }
      }
    }
    return deleteRange(editor, caret, range, SelectionType.fromCommandFlags(motion.getFlags()), isChange);
  }

  @Nullable
  private static TextRange getDeleteMotionRange(@NotNull Editor editor, @NotNull Caret caret,
                                                @NotNull DataContext context, int count, int rawCount,
                                                @NotNull Argument argument) {
    TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, true);
    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is deleted when it shouldn't be.
    if (range != null) {
      String text = editor.getDocument().getCharsSequence().subSequence(range.getStartOffset(),
                                                                        range.getEndOffset()).toString();
      final int lastNewLine = text.lastIndexOf('\n');
      if (lastNewLine > 0) {
        final Command motion = argument.getMotion();
        if (motion != null) {
          final String id = ActionManager.getInstance().getId(motion.getAction());
          if (id.equals(VIM_MOTION_WORD_RIGHT) ||
              id.equals(VIM_MOTION_BIG_WORD_RIGHT) ||
              id.equals(VIM_MOTION_CAMEL_RIGHT)) {
            if (!SearchHelper.anyNonWhitespace(editor, range.getEndOffset(), -1)) {
              final int start = range.getStartOffset();
              range = new TextRange(start, start + lastNewLine);
            }
          }
        }
      }
    }
    return range;
  }

  /**
   * Delete the range of text.
   *
   * @param editor   The editor to delete the text from
   * @param caret    The caret to be moved after deletion
   * @param range    The range to delete
   * @param type     The type of deletion
   * @param isChange Is from a change action
   * @return true if able to delete the text, false if not
   */
  public boolean deleteRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range,
                             @Nullable SelectionType type, boolean isChange) {

    final boolean res = deleteText(editor, range, type);
    final int size = EditorHelper.getFileSize(editor);
    if (res) {
      final int pos;
      if (caret.getOffset() > size) {
        pos = size - 1;
      }
      else {
        pos = EditorHelper.normalizeOffset(editor, range.getStartOffset(), isChange);
      }
      MotionGroup.moveCaret(editor, caret, pos, true);
    }
    return res;
  }

  /**
   * Begin Replace mode
   *
   * @param editor  The editor to replace in
   * @param context The data context
   */
  public void changeReplace(@NotNull Editor editor, @NotNull DataContext context) {
    initInsert(editor, context, CommandState.Mode.REPLACE);
  }

  /**
   * Replace each of the next count characters with the character ch
   *
   * @param editor The editor to change
   * @param caret  The caret to perform action on
   * @param count  The number of characters to change
   * @param ch     The character to change to
   * @return true if able to change count characters, false if not
   */
  public boolean changeCharacter(@NotNull Editor editor, @NotNull Caret caret, int count, char ch) {
    int col = caret.getLogicalPosition().column;
    int len = EditorHelper.getLineLength(editor);
    int offset = caret.getOffset();
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

    StringBuilder repl = new StringBuilder(count);
    for (int i = 0; i < num; i++) {
      repl.append(ch);
    }

    replaceText(editor, offset, offset + count, repl.toString());

    // Indent new line if we replaced with a newline
    if (ch == '\n') {
      caret.moveToOffset(offset + 1);
      insertText(editor, caret, space);
      int slen = space.length();
      if (slen == 0) {
        slen++;
      }
      caret.moveToOffset(offset + slen);
    }

    return true;
  }

  /**
   * Each character in the supplied range gets replaced with the character ch
   *
   * @param editor The editor to change
   * @param range  The range to change
   * @param ch     The replacing character
   * @return true if able to change the range, false if not
   */
  public boolean changeCharacterRange(@NotNull Editor editor, @NotNull TextRange range, char ch) {
    if (logger.isDebugEnabled()) {
      logger.debug("change range: " + range + " to " + ch);
    }

    CharSequence chars = editor.getDocument().getCharsSequence();
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    for (int j = ends.length - 1; j >= 0; j--) {
      for (int i = starts[j]; i < ends[j]; i++) {
        if (i < chars.length() && '\n' != chars.charAt(i)) {
          replaceText(editor, i, i + 1, Character.toString(ch));
        }
      }
    }

    return true;
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor  The editor to change
   * @param caret   The caret to be moved
   * @param count   The number of characters to change
   * @return true if able to delete count characters, false if not
   */
  public boolean changeCharacters(@NotNull Editor editor, @NotNull Caret caret, int count) {
    int len = EditorHelper.getLineLength(editor);
    int col = caret.getLogicalPosition().column;
    if (col + count >= len) {
      return changeEndOfLine(editor, caret, 1);
    }

    boolean res = deleteCharacter(editor, caret, count, true);
    if (res) {
      EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
    }

    return res;
  }

  /**
   * Delete count lines and then enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret on the line to be changed
   * @param count  The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  public boolean changeLine(@NotNull Editor editor, @NotNull Caret caret, int count) {
    final LogicalPosition pos = editor.offsetToLogicalPosition(caret.getOffset());
    final boolean insertBelow = pos.line + count >= EditorHelper.getLineCount(editor);

    final LogicalPosition lp = editor.offsetToLogicalPosition(
      VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));

    boolean res = deleteLine(editor, caret, count);
    if (res) {
      if (insertBelow) {
        insertNewLineBelow(editor, caret, lp.column);
      }
      else {
        insertNewLineAbove(editor, caret, lp.column);
      }
    }

    return res;
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down and enter insert mode
   *
   * @param editor  The editor to change
   * @param caret   The caret to perform action on
   * @param count   The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  public boolean changeEndOfLine(@NotNull Editor editor, @NotNull Caret caret, int count) {
    boolean res = deleteEndOfLine(editor, caret, count);
    if (res) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
      EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
    }

    return res;
  }

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param caret    The caret on which the motion is supposed to be performed
   * @param context  The data context
   * @param count    The number of time to repeat the change
   * @param rawCount The actual count entered by the user
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  public boolean changeMotion(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                              int rawCount, @NotNull Argument argument) {
    // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
    final Command motion = argument.getMotion();
    if (motion == null) {
      return false;
    }
    String id = ActionManager.getInstance().getId(motion.getAction());
    boolean kludge = false;
    boolean bigWord = id.equals(VIM_MOTION_BIG_WORD_RIGHT);
    final CharSequence chars = editor.getDocument().getCharsSequence();
    final int offset = caret.getOffset();
    final CharacterHelper.CharacterType charType = CharacterHelper.charType(chars.charAt(offset), bigWord);
    if (EditorHelper.getFileSize(editor) > 0 && charType != CharacterHelper.CharacterType.WHITESPACE) {
      final boolean lastWordChar = offset > EditorHelper.getFileSize(editor) ||
                                   CharacterHelper.charType(chars.charAt(offset + 1), bigWord) != charType;
      final ImmutableSet<String> wordMotions =
        ImmutableSet.of(VIM_MOTION_WORD_RIGHT, VIM_MOTION_BIG_WORD_RIGHT, VIM_MOTION_CAMEL_RIGHT);
      if (wordMotions.contains(id) && lastWordChar && motion.getCount() == 1) {
        final boolean res = deleteCharacter(editor, caret, 1, true);
        if (res) {
          EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
        }
        return res;
      }
      switch (id) {
        case VIM_MOTION_WORD_RIGHT:
          kludge = true;
          motion.setAction(ActionManager.getInstance().getAction(VIM_MOTION_WORD_END_RIGHT));
          motion.setFlags(Command.FLAG_MOT_INCLUSIVE);
          break;
        case VIM_MOTION_BIG_WORD_RIGHT:
          kludge = true;
          motion.setAction(ActionManager.getInstance().getAction(VIM_MOTION_BIG_WORD_END_RIGHT));
          motion.setFlags(Command.FLAG_MOT_INCLUSIVE);
          break;
        case VIM_MOTION_CAMEL_RIGHT:
          kludge = true;
          motion.setAction(ActionManager.getInstance().getAction(VIM_MOTION_CAMEL_END_RIGHT));
          motion.setFlags(Command.FLAG_MOT_INCLUSIVE);
          break;
      }
    }

    if (kludge) {
      int size = EditorHelper.getFileSize(editor);
      int cnt = count * motion.getCount();
      int pos1 = SearchHelper.findNextWordEnd(chars, offset, size, cnt, bigWord, false);
      int pos2 = SearchHelper.findNextWordEnd(chars, pos1, size, -cnt, bigWord, false);
      if (logger.isDebugEnabled()) {
        logger.debug("pos=" + offset);
        logger.debug("pos1=" + pos1);
        logger.debug("pos2=" + pos2);
        logger.debug("count=" + count);
        logger.debug("arg.count=" + motion.getCount());
      }
      if (pos2 == offset) {
        if (count > 1) {
          count--;
          rawCount--;
        }
        else if (motion.getCount() > 1) {
          motion.setCount(motion.getCount() - 1);
        }
        else {
          motion.setFlags(Command.FLAG_MOT_EXCLUSIVE);
        }
      }
    }

    boolean res = deleteMotion(editor, caret, context, count, rawCount, argument, true);
    if (res) {
      EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
    }

    return res;
  }

  public boolean blockInsert(@NotNull Editor editor, @NotNull DataContext context, @NotNull TextRange range,
                             boolean append) {
    final int lines = getLinesCountInVisualBlock(editor, range);
    final LogicalPosition startPosition = editor.offsetToLogicalPosition(range.getStartOffset());

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final int line = startPosition.line;
      int column = startPosition.column;
      if (!range.isMultiple()) {
        column = 0;
      }
      else if (append) {
        column += range.getMaxLength();
        if (CaretData.getLastColumn(caret) == MotionGroup.LAST_COLUMN) {
          column = MotionGroup.LAST_COLUMN;
        }
      }

      final int lineLength = EditorHelper.getLineLength(editor, line);
      if (column < MotionGroup.LAST_COLUMN && lineLength < column) {
        final String pad = EditorHelper.pad(editor, context, line, column);
        final int offset = editor.getDocument().getLineEndOffset(line);
        caret.moveToOffset(offset);
        insertText(editor, caret, pad);
      }

      if (range.isMultiple() || !append) {
        caret.moveToOffset(editor.logicalPositionToOffset(new LogicalPosition(line, column)));
      }
      if (range.isMultiple()) {
        setInsertRepeat(lines, column, append);
      }
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
   * @param editor The editor to change
   * @param caret  The caret to be moved after range deletion
   * @param range  The range to change
   * @param type   The type of the range
   * @return true if able to delete the range, false if not
   */
  public boolean changeRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range,
                             @NotNull SelectionType type) {
    int col = 0;
    int lines = 0;
    if (type == SelectionType.BLOCK_WISE) {
      lines = getLinesCountInVisualBlock(editor, range);
      col = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      if (CaretData.getLastColumn(caret) == MotionGroup.LAST_COLUMN) {
        col = MotionGroup.LAST_COLUMN;
      }
    }
    boolean after = range.getEndOffset() >= EditorHelper.getFileSize(editor);

    final LogicalPosition lp = editor.offsetToLogicalPosition(
        VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));

    boolean res = deleteRange(editor, caret, range, type, true);
    if (res) {
      if (type == SelectionType.LINE_WISE) {
        if (after) {
          insertNewLineBelow(editor, caret, lp.column);
        }
        else {
          insertNewLineAbove(editor, caret, lp.column);
        }
      }
      else {
        if (type == SelectionType.BLOCK_WISE) {
          setInsertRepeat(lines, col, false);
        }
        EditorData.setChangeSwitchMode(editor, CommandState.Mode.INSERT);
      }
    }

    return res;
  }

  /**
   * Counts number of lines in the visual block.
   * <p>
   * The result includes empty and short lines which does not have explicit start position (caret).
   *
   * @param editor The editor the block was selected in
   * @param range  The range corresponding to the selected block
   * @return total number of lines
   */
  private static int getLinesCountInVisualBlock(@NotNull Editor editor, @NotNull TextRange range) {
    final int[] startOffsets = range.getStartOffsets();
    if (startOffsets.length == 0) return 0;
    final LogicalPosition firstStart = editor.offsetToLogicalPosition(startOffsets[0]);
    final LogicalPosition lastStart = editor.offsetToLogicalPosition(startOffsets[range.size() - 1]);
    return lastStart.line - firstStart.line + 1;
  }

  /**
   * Toggles the case of count characters
   *
   * @param editor The editor to change
   * @param caret  The caret on which the operation is performed
   * @param count  The number of characters to change
   * @return true if able to change count characters
   */
  public boolean changeCaseToggleCharacter(@NotNull Editor editor, @NotNull Caret caret, int count) {
    final int offset = VimPlugin.getMotion().moveCaretHorizontal(editor, caret, count, true);
    if (offset == -1) {
      return false;
    }
    changeCase(editor, caret.getOffset(), offset, CharacterHelper.CASE_TOGGLE);
    MotionGroup.moveCaret(editor, caret, EditorHelper.normalizeOffset(editor, offset, false));
    return true;
  }

  /**
   * Changes the case of all the character moved over by the motion argument.
   *
   * @param editor   The editor to change
   * @param caret    The caret on which motion pretends to be performed
   * @param context  The data context
   * @param count    The number of times to repeat the change
   * @param rawCount The actual count entered by the user
   * @param type     The case change type (TOGGLE, UPPER, LOWER)
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  public boolean changeCaseMotion(@NotNull Editor editor, @NotNull Caret caret, DataContext context, int count,
                                  int rawCount, char type, @NotNull Argument argument) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, true);
    return range != null && changeCaseRange(editor, caret, range, type);
  }

  /**
   * Changes the case of all the characters in the range
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param range  The range to change
   * @param type   The case change type (TOGGLE, UPPER, LOWER)
   * @return true if able to delete the text, false if not
   */
  public boolean changeCaseRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range, char type) {
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    for (int i = ends.length - 1; i >= 0; i--) {
      changeCase(editor, starts[i], ends[i], type);
    }
    MotionGroup.moveCaret(editor, caret, range.getStartOffset());
    return true;
  }

  /**
   * This performs the actual case change.
   *
   * @param editor The editor to change
   * @param start  The start offset to change
   * @param end    The end offset to change
   * @param type   The type of change (TOGGLE, UPPER, LOWER)
   */
  private void changeCase(@NotNull Editor editor, int start, int end, char type) {
    if (start > end) {
      int t = end;
      end = start;
      start = t;
    }
    end = EditorHelper.normalizeOffset(editor, end);

    CharSequence chars = editor.getDocument().getCharsSequence();
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < end; i++) {
      sb.append(CharacterHelper.changeCase(chars.charAt(i), type));
    }
    replaceText(editor, start, end, sb.toString());
  }

  public void autoIndentLines(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count) {
    final int startLine = caret.getLogicalPosition().line;
    final int endLine = startLine + count - 1;

    if (endLine <= EditorHelper.getLineCount(editor)) {
      final TextRange range = new TextRange(caret.getOffset(), editor.getDocument().getLineEndOffset(endLine));
      autoIndentRange(editor, caret, context, range);
    }
  }

  public void autoIndentMotion(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                               int rawCount, @NotNull Argument argument) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, false);
    if (range != null) {
      autoIndentRange(editor, caret, context, range);
    }
  }

  private void restoreCursor(@NotNull Editor editor, @NotNull Caret caret, int startLine) {
    if (caret != editor.getCaretModel().getPrimaryCaret()) {
      editor.getCaretModel().addCaret(
        editor.offsetToVisualPosition(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, startLine)), false);
    }
  }

  public void autoIndentRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                              @NotNull TextRange range) {
    final int startOffset = EditorHelper.getLineStartForOffset(editor, range.getStartOffset());
    final int endOffset = EditorHelper.getLineEndForOffset(editor, range.getEndOffset());

    editor.getSelectionModel().setSelection(startOffset, endOffset);

    KeyHandler.executeAction("AutoIndentLines", context);

    final int firstLine = editor.offsetToLogicalPosition(Math.min(startOffset, endOffset)).line;
    final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    MotionGroup.moveCaret(editor, caret, newOffset);
    restoreCursor(editor, caret, caret.getLogicalPosition().line);
  }

  public void reformatCode(@NotNull DataContext context) {
    KeyHandler.executeAction("ReformatCode", context);
  }

  public void indentLines(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int lines,
                          int dir) {
    int start = caret.getOffset();
    int end = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, lines - 1, false);
    indentRange(editor, caret, context, new TextRange(start, end), 1, dir);
  }

  public void indentMotion(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int count,
                           int rawCount, @NotNull Argument argument, int dir) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, false);
    if (range != null) {
      indentRange(editor, caret, context, range, 1, dir);
    }
  }

  public void indentRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                          @NotNull TextRange range, int count, int dir) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
    }

    final Project proj = PlatformDataKeys.PROJECT.getData(context); // API change - don't merge
    VirtualFile file = EditorData.getVirtualFile(editor);
    final int tabSize;
    final int indentSize;
    final boolean useTabs;
    if (file != null) {
      FileType type = FileTypeManager.getInstance().getFileTypeByFile(file);
      CodeStyleSettings settings = proj == null ? CodeStyle.getDefaultSettings() : CodeStyle.getSettings(proj);
      tabSize = settings.getTabSize(type);
      indentSize = settings.getIndentSize(type);
      useTabs = settings.useTabCharacter(type);
    }
    else {
      tabSize = 8;
      indentSize = 8;
      useTabs = true;
    }

    final int sline = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    final int eline = editor.offsetToLogicalPosition(range.getEndOffset()).line;

    if (range.isMultiple()) {
      final int from = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      final int size = indentSize * count;
      if (dir == 1) {
        // Right shift blockwise selection
        final int tabCnt;
        final int spcCnt;
        if (useTabs) {
          tabCnt = size / tabSize;
          spcCnt = size % tabSize;
        }
        else {
          tabCnt = 0;
          spcCnt = size;
        }

        final String indent = StringUtil.repeat("\t", tabCnt) + StringUtil.repeat(" ", spcCnt);

        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(editor, l);
          if (len > from) {
            LogicalPosition spos = new LogicalPosition(l, from);
            caret.moveToOffset(editor.logicalPositionToOffset(spos));
            insertText(editor, caret, indent);
          }
        }
      }
      else {
        // Left shift blockwise selection
        CharSequence chars = editor.getDocument().getCharsSequence();
        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(editor, l);
          if (len > from) {
            LogicalPosition spos = new LogicalPosition(l, from);
            LogicalPosition epos = new LogicalPosition(l, from + size - 1);
            int wsoff = editor.logicalPositionToOffset(spos);
            int weoff = editor.logicalPositionToOffset(epos);
            int pos;
            for (pos = wsoff; pos <= weoff; pos++) {
              if (CharacterHelper.charType(chars.charAt(pos), false) != CharacterHelper.CharacterType.WHITESPACE) {
                break;
              }
            }
            if (pos > wsoff) {
              deleteText(editor, new TextRange(wsoff, pos), null);
            }
          }
        }
      }
    }
    else {
      // Shift non-blockwise selection
      for (int l = sline; l <= eline; l++) {
        final int soff = EditorHelper.getLineStartOffset(editor, l);
        final int eoff = EditorHelper.getLineEndOffset(editor, l, true);
        final int woff = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, l);
        final int col = editor.offsetToVisualPosition(woff).column;
        final int limit = Math.max(0, col + dir * indentSize * count);
        if (col > 0 || soff != eoff) {
          final int tabsCnt;
          final int spacesCnt;
          if (useTabs) {
            tabsCnt = limit / tabSize;
            spacesCnt = limit % tabSize;
          }
          else {
            tabsCnt = 0;
            spacesCnt = limit;
          }

          final String indent = StringUtil.repeat("\t", tabsCnt) + StringUtil.repeat(" ", spacesCnt);
          replaceText(editor, soff, woff, indent);
        }
      }
    }

    if (!CommandState.inInsertMode(editor)) {
      if (!range.isMultiple()) {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, sline));
      }
      else {
        MotionGroup.moveCaret(editor, caret, range.getStartOffset());
      }
    }

    CaretData.setLastColumn(editor, caret, caret.getVisualPosition().column);
  }

  /**
   * Inserts text into the document
   *
   * @param editor The editor to insert into
   * @param caret  The caret to start insertion in
   * @param str    The text to insert
   */
  public void insertText(@NotNull Editor editor, @NotNull Caret caret, @NotNull String str) {
    int start = caret.getOffset();
    editor.getDocument().insertString(start, str);
    caret.moveToOffset(start + str.length());

    VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, start);
  }

  /**
   * Delete text from the document. This will fail if being asked to store the deleted text into a read-only
   * register.
   *
   * @param editor The editor to delete from
   * @param range  The range to delete
   * @param type   The type of deletion
   * @return true if able to delete the text, false if not
   */
  private boolean deleteText(@NotNull final Editor editor, @NotNull final TextRange range,
                             @Nullable SelectionType type) {
    // Fix for http://youtrack.jetbrains.net/issue/VIM-35
    if (!range.normalize(EditorHelper.getFileSize(editor, true))) {
      return false;
    }

    if (type == null || VimPlugin.getRegister().storeText(editor, range, type, true)) {
      final Document document = editor.getDocument();
      final int[] startOffsets = range.getStartOffsets();
      final int[] endOffsets = range.getEndOffsets();
      for (int i = range.size() - 1; i >= 0; i--) {
        document.deleteString(startOffsets[i], endOffsets[i]);
      }

      if (type != null) {
        final int start = range.getStartOffset();
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, start);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(start, start));
      }

      return true;
    }

    return false;
  }

  /**
   * Replace text in the editor
   *
   * @param editor The editor to replace text in
   * @param start  The start offset to change
   * @param end    The end offset to change
   * @param str    The new text
   */
  private void replaceText(@NotNull Editor editor, int start, int end, @NotNull String str) {
    editor.getDocument().replaceString(start, end, str);

    final int newEnd = start + str.length();
    VimPlugin.getMark().setChangeMarks(editor, new TextRange(start, newEnd));
    VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, newEnd);
  }

  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @return true if able to sort the text, false if not
   */
  public boolean sortRange(@NotNull Editor editor, @NotNull LineRange range,
                           @NotNull Comparator<String> lineComparator) {
    final int startLine = range.getStartLine();
    final int endLine = range.getEndLine();
    final int count = endLine - startLine + 1;
    if (count < 2) {
      return false;
    }

    final int startOffset = editor.getDocument().getLineStartOffset(startLine);
    final int endOffset = editor.getDocument().getLineEndOffset(endLine);

    return sortTextRange(editor, startOffset, endOffset, lineComparator);
  }

  /**
   * Sorts a text range with a comparator. Returns true if a replace was performed, false otherwise.
   *
   * @param editor         The editor to replace text in
   * @param start          The starting position for the sort
   * @param end            The ending position for the sort
   * @param lineComparator The comparator to use to sort
   * @return true if able to sort the text, false if not
   */
  private boolean sortTextRange(@NotNull Editor editor, int start, int end,
                                @NotNull Comparator<String> lineComparator) {
    final String selectedText = editor.getDocument().getText(new TextRangeInterval(start, end));
    final List<String> lines = Lists.newArrayList(Splitter.on("\n").split(selectedText));
    if (lines.size() < 1) {
      return false;
    }
    lines.sort(lineComparator);
    replaceText(editor, start, end, StringUtil.join(lines, "\n"));
    return true;
  }

  private static void resetCursor(@NotNull Editor editor, boolean insert) {
    Document doc = editor.getDocument();
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (vf != null) {
      resetCursor(vf, editor.getProject(), insert);
    }
    else {
      editor.getSettings().setBlockCursor(!insert);
    }
  }

  private static void resetCursor(@NotNull VirtualFile virtualFile, Project proj, boolean insert) {
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

  public boolean changeNumber(@NotNull final Editor editor, @NotNull Caret caret, final int count) {
    final BoundListOption nf = (BoundListOption)Options.getInstance().getOption("nrformats");
    final boolean alpha = nf.contains("alpha");
    final boolean hex = nf.contains("hex");
    final boolean octal = nf.contains("octal");

    final TextRange range = SearchHelper.findNumberUnderCursor(editor, caret, alpha, hex, octal);
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
        number = StringHelper.rightJustify(number, text.length() - 2, '0');

        if (!lastLower) {
          number = number.toUpperCase();
        }

        number = text.substring(0, 2) + number;
      }
      else if (octal && text.startsWith("0") && text.length() > 1) {
        int num = (int)Long.parseLong(text, 8);
        num += count;
        number = Integer.toOctalString(num);
        number = "0" + StringHelper.rightJustify(number, text.length() - 1, '0');
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
          number = StringHelper.rightJustify(number, len, '0');
          if (neg) {
            number = "-" + number;
          }
        }
      }

      if (!text.equals(number)) {
        replaceText(editor, range.getStartOffset(), range.getEndOffset(), number);
        caret.moveToOffset(range.getStartOffset() + number.length() - 1);
      }

      return true;
    }
  }

  private final List<Object> strokes = new ArrayList<>();
  private int repeatCharsCount;
  private List<Object> lastStrokes;
  @Nullable
  private Command lastInsert;
  private boolean inInsert;
  private int repeatLines;
  private int repeatColumn;
  private boolean repeatAppend;
  private boolean lastLower = true;
  private Document document;
  @Nullable
  private DocumentListener documentListener;
  private int oldOffset = -1;

  private static final Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}