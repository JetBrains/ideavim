/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.RegisterActions;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
import com.maddyhome.idea.vim.group.visual.VisualModeHelperKt;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimInsertListener;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import com.maddyhome.idea.vim.option.BoundListOption;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.option.StrictMode;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import java.util.*;

/**
 * Provides all the insert/replace related functionality
 */
public class ChangeGroup {

  private static final int MAX_REPEAT_CHARS_COUNT = 10000;

  public static final String VIM_MOTION_BIG_WORD_RIGHT = "VimMotionBigWordRightAction";
  public static final String VIM_MOTION_WORD_RIGHT = "VimMotionWordRightAction";
  public static final String VIM_MOTION_CAMEL_RIGHT = "VimMotionCamelRightAction";
  private static final String VIM_MOTION_WORD_END_RIGHT = "VimMotionWordEndRightAction";
  private static final String VIM_MOTION_BIG_WORD_END_RIGHT = "VimMotionBigWordEndRightAction";
  private static final String VIM_MOTION_CAMEL_END_RIGHT = "VimMotionCamelEndRightAction";
  private static final ImmutableSet<String> wordMotions =
    ImmutableSet.of(VIM_MOTION_WORD_RIGHT, VIM_MOTION_BIG_WORD_RIGHT, VIM_MOTION_CAMEL_RIGHT);

  @NonNls private static final String HEX_START = "0x";
  @NonNls private static final String MAX_HEX_INTEGER = "ffffffffffffffff";

  private @Nullable Command lastInsert;

  private List<VimInsertListener> insertListeners = ContainerUtil.createLockFreeCopyOnWriteList();

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
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, 1, true));
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
  public void insertNewLineAbove(final @NotNull Editor editor, @NotNull DataContext context) {
    if (editor.isOneLineMode()) return;

    Set<Caret> firstLiners = new HashSet<>();
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (caret.getVisualPosition().line == 0) {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
        firstLiners.add(caret);
      }
      else {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
      }
    }

    initInsert(editor, context, CommandState.Mode.INSERT);
    runEnterAction(editor, context);

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (firstLiners.contains(caret)) {
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

    boolean firstLiner = false;
    if (caret.getVisualPosition().line == 0) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
      firstLiner = true;
    }
    else {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    }

    UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(editor).createIndentBySize(col));

    if (firstLiner) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretVertical(editor, caret, -1));
    }
  }

  /**
   * Begin insert after the current line by creating a new blank line below the current line
   * for all carets
   *
   * @param editor  The editor to insert into
   * @param context The data context
   */
  public void insertNewLineBelow(final @NotNull Editor editor, final @NotNull DataContext context) {
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
    UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(editor).createIndentBySize(col));
  }

  private void runEnterAction(Editor editor, @NotNull DataContext context) {
    CommandState state = CommandState.getInstance(editor);
    if (!state.isDotRepeatInProgress()) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
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

    insertBeforeCursor(editor, context);
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
      ModeHelper.exitInsertMode(editor, context);
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

  private @Nullable DocumentListener documentListener;

  /**
   * If the cursor is currently after the start of the current insert this deletes all the newly inserted text.
   * Otherwise it deletes all text from the cursor back to the first non-blank in the line.
   *
   * @param editor The editor to delete the text from
   * @param caret  The caret on which the action is performed
   * @return true if able to delete the text, false if not
   */
  public boolean insertDeleteInsertedText(@NotNull Editor editor, @NotNull Caret caret) {
    int deleteTo = UserDataManager.getVimInsertStart(caret).getStartOffset();
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
   * <p>
   * TODO This behavior should be configured via the `backspace` option
   *
   * @param editor The editor to delete the text from
   * @return true if able to delete text, false if not
   */
  public boolean insertDeletePreviousWord(@NotNull Editor editor, @NotNull Caret caret) {
    final int deleteTo;
    if (caret.getLogicalPosition().column == 0) {
      deleteTo = caret.getOffset() - 1;
    }
    else {
      int pointer = caret.getOffset() - 1;
      final CharSequence chars = editor.getDocument().getCharsSequence();
      while (pointer >= 0 && chars.charAt(pointer) == ' ' && chars.charAt(pointer) != '\n') pointer--;
      if (chars.charAt(pointer) == '\n') {
        deleteTo = pointer + 1;
      }
      else {
        deleteTo = VimPlugin.getMotion().findOffsetOfNextWord(editor, pointer + 1, -1, false);
      }
    }
    if (deleteTo < 0) {
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
      UserDataManager
        .setVimInsertStart(caret, editor.getDocument().createRangeMarker(caret.getOffset(), caret.getOffset()));
      if (caret == caretModel.getPrimaryCaret()) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_START, caret.getOffset());
      }
    }

    final Command cmd = state.getExecutingCommand();
    if (cmd != null && state.isDotRepeatInProgress()) {
      state.pushModes(mode, CommandState.SubMode.NONE);
      if (mode == CommandState.Mode.REPLACE) {
        setInsertEditorState(editor, false);
      }
      if (cmd.getFlags().contains(CommandFlags.FLAG_NO_REPEAT_INSERT)) {
        repeatInsert(editor, context, 1, false);
      }
      else {
        repeatInsert(editor, context, cmd.getCount(), false);
      }
      if (mode == CommandState.Mode.REPLACE) {
        setInsertEditorState(editor, true);
      }
      state.popModes();
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
      oldOffset = editor.getCaretModel().getOffset();
      setInsertEditorState(editor, mode == CommandState.Mode.INSERT);
      state.pushModes(mode, CommandState.SubMode.NONE);

      VisualGroupKt.updateCaretState(editor);
    }

    notifyListeners(editor);
  }

  // Workaround for VIM-1546. Another solution is highly appreciated.
  public boolean tabAction = false;

  private final @NotNull EditorMouseListener listener = new EditorMouseListener() {
    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
      Editor editor = event.getEditor();
      if (CommandStateHelper.inInsertMode(editor)) {
        clearStrokes(editor);
      }
    }
  };

  public void editorCreated(Editor editor) {
    EventFacade.getInstance().addEditorMouseListener(editor, listener);
  }

  public void editorReleased(Editor editor) {
    EventFacade.getInstance().removeEditorMouseListener(editor, listener);
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
          else if (lastStroke instanceof EditorActionHandlerBase) {
            KeyHandler.executeVimAction(editor, (EditorActionHandlerBase)lastStroke, context);
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
   * <p>
   * DEPRECATED. Please, don't use this function directly. Use ModeHelper.exitInsert
   */
  public void processEscape(@NotNull Editor editor, @Nullable DataContext context) {

    // Get the offset for marks before we exit insert mode - switching from insert to overtype subtracts one from the
    // column offset.
    int offset = editor.getCaretModel().getPrimaryCaret().getOffset();
    final MarkGroup markGroup = VimPlugin.getMark();
    markGroup.setMark(editor, '^', offset);
    markGroup.setMark(editor, MarkGroup.MARK_CHANGE_END, offset);

    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      setInsertEditorState(editor, true);
    }

    int cnt = lastInsert != null ? lastInsert.getCount() : 0;
    if (lastInsert != null && (lastInsert.getFlags().contains(CommandFlags.FLAG_NO_REPEAT_INSERT))) {
      cnt = 1;
    }

    if (document != null && documentListener != null) {
      EventFacade.getInstance().removeDocumentListener(document, documentListener);
      documentListener = null;
    }

    lastStrokes = new ArrayList<>(strokes);

    if (context != null) {
      repeatInsert(editor, context, cnt == 0 ? 0 : cnt - 1, true);
    }

    // The change pos '.' mark is the offset AFTER processing escape, and after switching to overtype
    offset = editor.getCaretModel().getPrimaryCaret().getOffset();
    markGroup.setMark(editor, MarkGroup.MARK_CHANGE_POS, offset);

    CommandState.getInstance(editor).popModes();
    exitAllSingleCommandInsertModes(editor);

    VisualGroupKt.updateCaretState(editor);
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
      setInsertEditorState(editor, true);
    }
    final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    final List<AnAction> actions = VimPlugin.getKey().getActions(editor.getComponent(), enterKeyStroke);
    for (AnAction action : actions) {
      if (KeyHandler.executeAction(action, context)) {
        break;
      }
    }
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      setInsertEditorState(editor, false);
    }
  }

  /**
   * Inserts the character above/below the cursor at the cursor location
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert after
   * @param dir    1 for getting from line below cursor, -1 for getting from line above cursor
   * @return true if able to get the character and insert it, false if not
   */
  public boolean insertCharacterAroundCursor(@NotNull Editor editor, @NotNull Caret caret, int dir) {
    boolean res = false;

    VisualPosition vp = caret.getVisualPosition();
    vp = new VisualPosition(vp.line + dir, vp.column);
    int len = EditorHelper.getLineLength(editor, EditorHelper.visualLineToLogicalLine(editor, vp.line));
    if (vp.column < len) {
      int offset = EditorHelper.visualPositionToOffset(editor, vp);
      CharSequence charsSequence = editor.getDocument().getCharsSequence();
      if (offset < charsSequence.length()) {
        char ch = charsSequence.charAt(offset);
        editor.getDocument().insertString(caret.getOffset(), Character.toString(ch));
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, 1, true));
        res = true;
      }
    }

    return res;
  }

  /**
   * Sets the insert/replace state of the editor.
   */
  private void setInsertEditorState(@NotNull Editor editor, boolean value) {
    final EditorEx editorEx = ObjectUtils.tryCast(editor, EditorEx.class);
    if (editorEx == null) return;
    editorEx.setInsertMode(value);
  }

  /**
   * Performs a mode switch after change action
   *
   * @param editor   The editor to switch mode in
   * @param context  The data context
   * @param toSwitch The mode to switch to
   */
  public void processPostChangeModeSwitch(@NotNull Editor editor,
                                          @NotNull DataContext context,
                                          @NotNull CommandState.Mode toSwitch) {
    if (toSwitch == CommandState.Mode.INSERT) {
      initInsert(editor, context, CommandState.Mode.INSERT);
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
          if (repeatAppend &&
              repeatColumn < MotionGroup.LAST_COLUMN &&
              EditorHelper.getVisualLineLength(editor, visualLine + i) < repeatColumn) {
            final String pad = EditorHelper.pad(editor, context, logicalLine + i, repeatColumn);
            if (pad.length() > 0) {
              final int offset = editor.getDocument().getLineEndOffset(logicalLine + i);
              insertText(editor, caret, offset, pad);
            }
          }
          if (repeatColumn >= MotionGroup.LAST_COLUMN) {
            caret.moveToOffset(VimPlugin.getMotion().moveCaretToLineEnd(editor, logicalLine + i, true));
            repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
          }
          else if (EditorHelper.getVisualLineLength(editor, visualLine + i) >= repeatColumn) {
            VisualPosition visualPosition = new VisualPosition(visualLine + i, repeatColumn);
            int inlaysCount = InlayHelperKt.amountOfInlaysBeforeVisualPosition(editor, visualPosition);
            caret.moveToVisualPosition(new VisualPosition(visualLine + i, repeatColumn + inlaysCount));
            repeatInsertText(editor, context, started ? (i == 0 ? count : count + 1) : count);
          }
        }

        MotionGroup.moveCaret(editor, caret, position);
      }
      else {
        repeatInsertText(editor, context, count);
        final int position = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, -1, false);

        MotionGroup.moveCaret(editor, caret, position);
      }
    }

    repeatLines = 0;
    repeatColumn = 0;
    repeatAppend = false;
  }

  /**
   * Processes the user pressing the Insert key while in INSERT or REPLACE mode. This simply toggles the
   * Insert/Overwrite state which updates the status bar.
   *
   * @param editor The editor to toggle the state in
   */
  public void processInsert(Editor editor) {
    final EditorEx editorEx = ObjectUtils.tryCast(editor, EditorEx.class);
    if (editorEx == null) return;
    editorEx.setInsertMode(!editorEx.isInsertMode());
    CommandState.getInstance(editor).toggleInsertOverwrite();
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
    if (cmd.getFlags().contains(CommandFlags.FLAG_SAVE_STROKE)) {
      strokes.add(cmd.getAction());
    }
    else if (cmd.getFlags().contains(CommandFlags.FLAG_CLEAR_STROKES)) {
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
      UserDataManager
        .setVimInsertStart(caret, editor.getDocument().createRangeMarker(caret.getOffset(), caret.getOffset()));
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
    final int endOffset = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, count, true);
    if (endOffset != -1) {
      final boolean res = deleteText(editor, new TextRange(caret.getOffset(), endOffset), SelectionType.CHARACTER_WISE);
      final int pos = caret.getOffset();
      final int norm = EditorHelper.normalizeOffset(editor, caret.getLogicalPosition().line, pos, isChange);
      if (norm != pos || editor.offsetToVisualPosition(norm) != EditorUtil.inlayAwareOffsetToVisualPosition(editor, norm)) {
        MotionGroup.moveCaret(editor, caret, norm);
      }

      return res;
    }

    return false;
  }

  /**
   * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
   * return to INSERT or REPLACE mode.
   *
   * @param editor The editor to put into NORMAL mode for one command
   */
  public void processSingleCommand(@NotNull Editor editor) {
    CommandState.getInstance(editor).pushModes(CommandState.Mode.COMMAND, CommandState.SubMode.SINGLE_COMMAND);
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
  public void beforeProcessKey(final @NotNull Editor editor,
                               final @NotNull DataContext context,
                               final @NotNull KeyStroke key,
                               @NotNull ActionPlan plan) {

    final TypedActionHandler originalHandler = KeyHandler.getInstance().getOriginalHandler();

    if (originalHandler instanceof TypedActionHandlerEx) {
      ((TypedActionHandlerEx)originalHandler).beforeExecute(editor, key.getKeyChar(), context, plan);
    }
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
    int initialOffset = caret.getOffset();
    int offset = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true);
    int lineStart = VimPlugin.getMotion().moveCaretToLineStart(editor, caret);

    int startOffset = initialOffset;
    if (offset == initialOffset && offset != lineStart) startOffset--; // handle delete from virtual space

    if (offset != -1) {
      final TextRange rangeToDelete = new TextRange(startOffset, offset);
      editor.getCaretModel().getAllCarets().stream().filter(c -> c != caret && rangeToDelete.contains(c.getOffset()))
        .forEach(c -> editor.getCaretModel().removeCaret(c));
      boolean res = deleteText(editor, rangeToDelete, SelectionType.CHARACTER_WISE);

      if (CommandStateHelper.getUsesVirtualSpace()) {
        MotionGroup.moveCaret(editor, caret, startOffset);
      }
      else {
        int pos = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, -1, false);
        if (pos != -1) {
          MotionGroup.moveCaret(editor, caret, pos);
        }
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
   * This processes all "regular" keystrokes entered while in insert/replace mode
   *
   * @param editor  The editor the character was typed into
   * @param context The data context
   * @param key     The user entered keystroke
   * @return true if this was a regular character, false if not
   */
  public boolean processKey(final @NotNull Editor editor,
                            final @NotNull DataContext context,
                            final @NotNull KeyStroke key) {
    if (logger.isDebugEnabled()) {
      logger.debug("processKey(" + key + ")");
    }

    if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
      final Document doc = editor.getDocument();
      CommandProcessor.getInstance().executeCommand(editor.getProject(), () -> ApplicationManager.getApplication()
                                                      .runWriteAction(() -> KeyHandler.getInstance().getOriginalHandler().execute(editor, key.getKeyChar(), context)),
                                                    "", doc, UndoConfirmationPolicy.DEFAULT, doc);

      return true;
    }

    return false;
  }

  public boolean processKeyInSelectMode(final @NotNull Editor editor,
                                        final @NotNull DataContext context,
                                        final @NotNull KeyStroke key) {
    boolean res;
    try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
      res = processKey(editor, context, key);

      ModeHelper.exitSelectMode(editor, false);
      KeyHandler.getInstance().reset(editor);

      if (isPrintableChar(key.getKeyChar()) || activeTemplateWithLeftRightMotion(editor, key)) {
        VimPlugin.getChange().insertBeforeCursor(editor, context);
      }
    }

    return res;
  }

  private boolean isPrintableChar(char c) {
    Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
    return (!Character.isISOControl(c)) &&
           c != KeyEvent.CHAR_UNDEFINED &&
           block != null &&
           block != Character.UnicodeBlock.SPECIALS;
  }

  private boolean activeTemplateWithLeftRightMotion(Editor editor, KeyStroke keyStroke) {
    return HelperKt.isTemplateActive(editor) &&
           (keyStroke.getKeyCode() == KeyEvent.VK_LEFT || keyStroke.getKeyCode() == KeyEvent.VK_RIGHT);
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
                          EditorHelperRt.getFileSize(editor));
    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("offset=" + offset);
    }
    if (offset != -1) {
      boolean res = deleteText(editor, new TextRange(start, offset), SelectionType.LINE_WISE);
      if (res && caret.getOffset() >= EditorHelperRt.getFileSize(editor) && caret.getOffset() != 0) {
        MotionGroup
          .moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor, caret, -1));
      }

      return res;
    }

    return false;
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
  public boolean deleteJoinRange(@NotNull Editor editor,
                                 @NotNull Caret caret,
                                 @NotNull TextRange range,
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
  private boolean deleteJoinNLines(@NotNull Editor editor,
                                   @NotNull Caret caret,
                                   int startLine,
                                   int count,
                                   boolean spaces) {
    // start my moving the cursor to the very end of the first line
    MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, startLine, true));
    for (int i = 1; i < count; i++) {
      int start = VimPlugin.getMotion().moveCaretToLineEnd(editor, caret);
      int trailingWhitespaceStart = VimPlugin.getMotion().moveCaretToLineEndSkipLeadingOffset(editor, caret, 0);
      boolean hasTrailingWhitespace = start != trailingWhitespaceStart + 1;

      MotionGroup.moveCaret(editor, caret, start);
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
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, -1, true));
      }
    }

    return true;
  }

  public boolean joinViaIdeaByCount(@NotNull Editor editor, @NotNull DataContext context, int count) {
    int executions = count > 1 ? count - 1 : 1;
    final boolean allowedExecution = editor.getCaretModel().getAllCarets().stream().anyMatch(caret -> {
      int lline = caret.getLogicalPosition().line;
      int total = EditorHelper.getLineCount(editor);
      return lline + count <= total;
    });
    if (!allowedExecution) return false;
    for (int i = 0; i < executions; i++) KeyHandler.executeAction(IdeActions.ACTION_EDITOR_JOIN_LINES, context);
    return true;
  }

  public void joinViaIdeaBySelections(@NotNull Editor editor,
                                      @NotNull DataContext context,
                                      @NotNull Map<Caret, ? extends VimSelection> caretsAndSelections) {
    caretsAndSelections.forEach((caret, range) -> {
      if (!caret.isValid()) return;
      final Pair<Integer, Integer> nativeRange = range.getNativeStartAndEnd();
      caret.setSelection(nativeRange.getFirst(), nativeRange.getSecond());
    });
    KeyHandler.executeAction(IdeActions.ACTION_EDITOR_JOIN_LINES, context);
    editor.getCaretModel().getAllCarets().forEach(caret -> {
      caret.removeSelection();
      final VisualPosition currentVisualPosition = caret.getVisualPosition();
      if (currentVisualPosition.line < 1) return;
      final VisualPosition newVisualPosition =
        new VisualPosition(currentVisualPosition.line - 1, currentVisualPosition.column);
      caret.moveToVisualPosition(newVisualPosition);
    });
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
      insertText(editor, caret, offset + 1, space);
      int slen = space.length();
      if (slen == 0) {
        slen++;
      }
      InlayHelperKt.moveToInlayAwareOffset(caret, offset + slen);
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

  public @Nullable Pair<TextRange, SelectionType> getDeleteRangeAndType(@NotNull Editor editor,
                                                                        @NotNull Caret caret,
                                                                        @NotNull DataContext context,
                                                                        int count,
                                                                        int rawCount,
                                                                        final @NotNull Argument argument,
                                                                        boolean isChange) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument);
    if (range == null) return null;

    // Delete motion commands that are not linewise become linewise if all the following are true:
    // 1) The range is across multiple lines
    // 2) There is only whitespace before the start of the range
    // 3) There is only whitespace after the end of the range
    SelectionType type;
    if (argument.getMotion().isLinewiseMotion()) {
      type = SelectionType.LINE_WISE;
    }
    else {
      type = SelectionType.CHARACTER_WISE;
    }
    final Command motion = argument.getMotion();
    if (!isChange && !motion.isLinewiseMotion()) {
      LogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      LogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (start.line != end.line) {
        if (!SearchHelper.anyNonWhitespace(editor, range.getStartOffset(), -1) &&
            !SearchHelper.anyNonWhitespace(editor, range.getEndOffset(), 1)) {
          type = SelectionType.LINE_WISE;
        }
      }
    }
    return new Pair<>(range, type);
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
  public boolean deleteRange(@NotNull Editor editor,
                             @NotNull Caret caret,
                             @NotNull TextRange range,
                             @Nullable SelectionType type,
                             boolean isChange) {

    boolean removeLastNewLine = removeLastNewLine(editor, range, type);
    final boolean res = deleteText(editor, range, type);
    if (removeLastNewLine) {
      int textLength = editor.getDocument().getTextLength();
      editor.getDocument().deleteString(textLength - 1, textLength);
    }

    if (res) {
      int pos = EditorHelper.normalizeOffset(editor, range.getStartOffset(), isChange);
      if (type == SelectionType.LINE_WISE) {
        pos = VimPlugin.getMotion().moveCaretToLineStart(editor, editor.offsetToLogicalPosition(pos).line);
      }
      MotionGroup.moveCaret(editor, caret, pos);
    }
    return res;
  }

  private boolean removeLastNewLine(@NotNull Editor editor, @NotNull TextRange range, @Nullable SelectionType type) {
    int endOffset = range.getEndOffset();
    int fileSize = EditorHelperRt.getFileSize(editor);
    if (endOffset > fileSize){
      StrictMode.INSTANCE.fail("Incorrect offset. File size: " + fileSize + ", offset: " + endOffset);
      endOffset = fileSize;
    }
    return type == SelectionType.LINE_WISE &&
           range.getStartOffset() != 0 &&
           editor.getDocument().getCharsSequence().charAt(endOffset - 1) != '\n' &&
           endOffset == fileSize;
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param count  The number of characters to change
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
      UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
    }

    return res;
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down and enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to perform action on
   * @param count  The number of lines to change
   * @return true if able to delete count lines, false if not
   */
  public boolean changeEndOfLine(@NotNull Editor editor, @NotNull Caret caret, int count) {
    boolean res = deleteEndOfLine(editor, caret, count);
    if (res) {
      MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
      UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
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
  public boolean changeMotion(@NotNull Editor editor,
                              @NotNull Caret caret,
                              @NotNull DataContext context,
                              int count,
                              int rawCount,
                              @NotNull Argument argument) {
    // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
    final Command motion = argument.getMotion();

    String id = motion.getAction().getId();
    boolean kludge = false;
    boolean bigWord = id.equals(VIM_MOTION_BIG_WORD_RIGHT);
    final CharSequence chars = editor.getDocument().getCharsSequence();
    final int offset = caret.getOffset();
    int fileSize = EditorHelperRt.getFileSize(editor);
    if (fileSize > 0 && offset < fileSize) {
      final CharacterHelper.CharacterType charType = CharacterHelper.charType(chars.charAt(offset), bigWord);
      if (charType != CharacterHelper.CharacterType.WHITESPACE) {
        final boolean lastWordChar =
          offset >= fileSize - 1 || CharacterHelper.charType(chars.charAt(offset + 1), bigWord) != charType;
        if (wordMotions.contains(id) && lastWordChar && motion.getCount() == 1) {
          final boolean res = deleteCharacter(editor, caret, 1, true);
          if (res) {
            UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
          }
          return res;
        }
        switch (id) {
          case VIM_MOTION_WORD_RIGHT:
            kludge = true;
            motion.setAction(RegisterActions.findActionOrDie(VIM_MOTION_WORD_END_RIGHT));

            break;
          case VIM_MOTION_BIG_WORD_RIGHT:
            kludge = true;
            motion.setAction(RegisterActions.findActionOrDie(VIM_MOTION_BIG_WORD_END_RIGHT));

            break;
          case VIM_MOTION_CAMEL_RIGHT:
            kludge = true;
            motion.setAction(RegisterActions.findActionOrDie(VIM_MOTION_CAMEL_END_RIGHT));

            break;
        }
      }
    }

    if (kludge) {
      int cnt = count * motion.getCount();
      int pos1 = SearchHelper.findNextWordEnd(chars, offset, fileSize, cnt, bigWord, false);
      int pos2 = SearchHelper.findNextWordEnd(chars, pos1, fileSize, -cnt, bigWord, false);
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
          motion.setFlags(EnumSet.noneOf(CommandFlags.class));
        }
      }
    }

    Pair<TextRange, SelectionType> deleteRangeAndType =
      getDeleteRangeAndType(editor, caret, context, count, rawCount, argument, true);
    if (deleteRangeAndType == null) return false;

    return changeRange(editor, caret, deleteRangeAndType.getFirst(), deleteRangeAndType.getSecond(), context);
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
    final int offset = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, count, true);
    if (offset == -1) {
      return false;
    }
    changeCase(editor, caret.getOffset(), offset, CharacterHelper.CASE_TOGGLE);
    MotionGroup.moveCaret(editor, caret, EditorHelper.normalizeOffset(editor, offset, false));
    return true;
  }

  public boolean blockInsert(@NotNull Editor editor,
                             @NotNull DataContext context,
                             @NotNull TextRange range,
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
        if (UserDataManager.getVimLastColumn(caret) == MotionGroup.LAST_COLUMN) {
          column = MotionGroup.LAST_COLUMN;
        }
      }

      final int lineLength = EditorHelper.getLineLength(editor, line);
      if (column < MotionGroup.LAST_COLUMN && lineLength < column) {
        final String pad = EditorHelper.pad(editor, context, line, column);
        final int offset = editor.getDocument().getLineEndOffset(line);
        insertText(editor, caret, offset, pad);
      }

      if (range.isMultiple() || !append) {
        InlayHelperKt.moveToInlayAwareLogicalPosition(caret, new LogicalPosition(line, column));
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

  /**
   * Deletes the range of text and enters insert mode
   *
   * @param editor  The editor to change
   * @param caret   The caret to be moved after range deletion
   * @param range   The range to change
   * @param type    The type of the range
   * @param context
   * @return true if able to delete the range, false if not
   */
  public boolean changeRange(@NotNull Editor editor,
                             @NotNull Caret caret,
                             @NotNull TextRange range,
                             @NotNull SelectionType type,
                             DataContext context) {
    int col = 0;
    int lines = 0;
    if (type == SelectionType.BLOCK_WISE) {
      lines = getLinesCountInVisualBlock(editor, range);
      col = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      if (UserDataManager.getVimLastColumn(caret) == MotionGroup.LAST_COLUMN) {
        col = MotionGroup.LAST_COLUMN;
      }
    }
    boolean after = range.getEndOffset() >= EditorHelper.getFileSize(editor, true);

    final LogicalPosition lp =
      editor.offsetToLogicalPosition(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));

    boolean res = deleteRange(editor, caret, range, type, true);
    if (res) {
      if (type == SelectionType.LINE_WISE) {
        // Please don't use `getDocument().getText().isEmpty()` because it converts CharSequence into String
        if (editor.getDocument().getTextLength() == 0) {
          insertBeforeCursor(editor, context);
        }
        else if (after && !EditorHelperRt.endsWithNewLine(editor)) {
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
        UserDataManager.setVimChangeActionSwitchMode(editor, CommandState.Mode.INSERT);
      }
    }
    else {
      insertBeforeCursor(editor, context);
    }

    return true;
  }

  private void restoreCursor(@NotNull Editor editor, @NotNull Caret caret, int startLine) {
    if (caret != editor.getCaretModel().getPrimaryCaret()) {
      editor.getCaretModel().addCaret(
        editor.offsetToVisualPosition(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, startLine)), false);
    }
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
  public boolean changeCaseMotion(@NotNull Editor editor,
                                  @NotNull Caret caret,
                                  DataContext context,
                                  int count,
                                  int rawCount,
                                  char type,
                                  @NotNull Argument argument) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument);
    return range != null && changeCaseRange(editor, caret, range, type);
  }

  public boolean reformatCodeMotion(@NotNull Editor editor,
                                    @NotNull Caret caret,
                                    DataContext context,
                                    int count,
                                    int rawCount,
                                    @NotNull Argument argument) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument);
    return range != null && reformatCodeRange(editor, caret, range);
  }

  public void reformatCodeSelection(@NotNull Editor editor, @NotNull Caret caret, @NotNull VimSelection range) {
    final TextRange textRange = range.toVimTextRange(true);
    reformatCodeRange(editor, caret, textRange);
  }

  private boolean reformatCodeRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range) {
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    final int firstLine = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    for (int i = ends.length - 1; i >= 0; i--) {
      final int startOffset = EditorHelper.getLineStartForOffset(editor, starts[i]);
      final int endOffset = EditorHelper.getLineEndForOffset(editor, ends[i] - (startOffset == ends[i] ? 0 : 1));
      reformatCode(editor, startOffset, endOffset);
    }
    final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    MotionGroup.moveCaret(editor, caret, newOffset);
    return true;
  }

  private void reformatCode(@NotNull Editor editor, int start, int end) {
    final Project project = editor.getProject();
    if (project == null) return;
    final PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);
    if (file == null) return;
    final com.intellij.openapi.util.TextRange textRange = com.intellij.openapi.util.TextRange.create(start, end);
    CodeStyleManager.getInstance(project).reformatText(file, Collections.singletonList(textRange));
  }

  public void autoIndentMotion(@NotNull Editor editor,
                               @NotNull Caret caret,
                               @NotNull DataContext context,
                               int count,
                               int rawCount,
                               @NotNull Argument argument) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument);
    if (range != null) {
      autoIndentRange(editor, caret, context,
                      new TextRange(range.getStartOffset(), HelperKt.getEndOffsetInclusive(range)));
    }
  }

  public void autoIndentRange(@NotNull Editor editor,
                              @NotNull Caret caret,
                              @NotNull DataContext context,
                              @NotNull TextRange range) {
    final int startOffset = EditorHelper.getLineStartForOffset(editor, range.getStartOffset());
    final int endOffset = EditorHelper.getLineEndForOffset(editor, range.getEndOffset());

    VisualModeHelperKt.vimSetSystemSelectionSilently(editor.getSelectionModel(), startOffset, endOffset);

    KeyHandler.executeAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES, context);

    final int firstLine = editor.offsetToLogicalPosition(Math.min(startOffset, endOffset)).line;
    final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    MotionGroup.moveCaret(editor, caret, newOffset);
    restoreCursor(editor, caret, caret.getLogicalPosition().line);
  }

  public void indentLines(@NotNull Editor editor,
                          @NotNull Caret caret,
                          @NotNull DataContext context,
                          int lines,
                          int dir) {
    int start = caret.getOffset();
    int end = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, lines - 1, true);
    indentRange(editor, caret, context, new TextRange(start, end), 1, dir);
  }

  /**
   * Inserts text into the document
   *
   * @param editor The editor to insert into
   * @param caret  The caret to start insertion in
   * @param str    The text to insert
   */
  public void insertText(@NotNull Editor editor, @NotNull Caret caret, int offset, @NotNull String str) {
    editor.getDocument().insertString(offset, str);
    InlayHelperKt.moveToInlayAwareOffset(caret, offset + str.length());

    VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, offset);
  }

  public void insertText(@NotNull Editor editor, @NotNull Caret caret, @NotNull String str) {
    insertText(editor, caret, caret.getOffset(), str);
  }

  public void insertText(@NotNull Editor editor, @NotNull Caret caret, @NotNull LogicalPosition start, @NotNull String str) {
    insertText(editor, caret, editor.logicalPositionToOffset(start), str);
  }

  public void indentMotion(@NotNull Editor editor,
                           @NotNull Caret caret,
                           @NotNull DataContext context,
                           int count,
                           int rawCount,
                           @NotNull Argument argument,
                           int dir) {
    final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument);
    if (range != null) {
      indentRange(editor, caret, context, range, 1, dir);
    }
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

  public void indentRange(@NotNull Editor editor,
                          @NotNull Caret caret,
                          @NotNull DataContext context,
                          @NotNull TextRange range,
                          int count,
                          int dir) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
    }

    IndentConfig indentConfig = IndentConfig.create(editor, context);

    final int sline = editor.offsetToLogicalPosition(range.getStartOffset()).line;
    final LogicalPosition endLogicalPosition = editor.offsetToLogicalPosition(range.getEndOffset());
    final int eline =
      endLogicalPosition.column == 0 ? Math.max(endLogicalPosition.line - 1, 0) : endLogicalPosition.line;

    if (range.isMultiple()) {
      final int from = editor.offsetToLogicalPosition(range.getStartOffset()).column;
      if (dir == 1) {
        // Right shift blockwise selection
        final String indent = indentConfig.createIndentByCount(count);

        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(editor, l);
          if (len > from) {
            LogicalPosition spos = new LogicalPosition(l, from);
            insertText(editor, caret, spos, indent);
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
            LogicalPosition epos = new LogicalPosition(l, from + indentConfig.getTotalIndent(count) - 1);
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
        final int limit = Math.max(0, col + dir * indentConfig.getTotalIndent(count));
        if (col > 0 || soff != eoff) {
          final String indent = indentConfig.createIndentBySize(limit);
          replaceText(editor, soff, woff, indent);
        }
      }
    }

    if (!CommandStateHelper.inInsertMode(editor)) {
      if (!range.isMultiple()) {
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, sline));
      }
      else {
        MotionGroup.moveCaret(editor, caret, range.getStartOffset());
      }
    }

    UserDataManager.setVimLastColumn(caret, caret.getVisualPosition().column);
  }

  /**
   * Delete text from the document. This will fail if being asked to store the deleted text into a read-only
   * register.
   * <p>
   * End offset of range is exclusive
   * <p>
   * delete new TextRange(1, 5)
   * 0123456789
   * Hello, xyz
   * .||||....
   * <p>
   * end <= text.length
   *
   * @param editor The editor to delete from
   * @param range  The range to delete
   * @param type   The type of deletion
   * @return true if able to delete the text, false if not
   */
  private boolean deleteText(final @NotNull Editor editor,
                             final @NotNull TextRange range,
                             @Nullable SelectionType type) {
    TextRange updatedRange = range;
    // Fix for https://youtrack.jetbrains.net/issue/VIM-35
    if (!range.normalize(EditorHelperRt.getFileSize(editor))) {
      if (range.getStartOffset() == range.getEndOffset() &&
          range.getStartOffset() == EditorHelperRt.getFileSize(editor) &&
          range.getStartOffset() != 0) {
        updatedRange = new TextRange(range.getStartOffset() - 1, range.getEndOffset());
      }
      else {
        return false;
      }
    }

    if (type == null ||
        CommandStateHelper.inInsertMode(editor) ||
        VimPlugin.getRegister().storeText(editor, updatedRange, type, true)) {
      final Document document = editor.getDocument();
      final int[] startOffsets = updatedRange.getStartOffsets();
      final int[] endOffsets = updatedRange.getEndOffsets();
      for (int i = updatedRange.size() - 1; i >= 0; i--) {
        document.deleteString(startOffsets[i], endOffsets[i]);
      }

      if (type != null) {
        final int start = updatedRange.getStartOffset();
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, start);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(start, start + 1));
      }

      return true;
    }

    return false;
  }

  public static void resetCaret(@NotNull Editor editor, boolean insert) {
    editor.getSettings().setBlockCursor(!insert);
  }

  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @return true if able to sort the text, false if not
   */
  public boolean sortRange(@NotNull Editor editor,
                           @NotNull LineRange range,
                           @NotNull Comparator<String> lineComparator) {
    final int startLine = range.startLine;
    final int endLine = range.endLine;
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
  private boolean sortTextRange(@NotNull Editor editor,
                                int start,
                                int end,
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

  /**
   * Perform increment and decrement for numbers in visual mode
   * <p>
   * Flag [avalanche] marks if increment (or decrement) should be performed in avalanche mode
   * (for v_g_Ctrl-A and v_g_Ctrl-X commands)
   *
   * @return true
   */
  public boolean changeNumberVisualMode(final @NotNull Editor editor,
                                        @NotNull Caret caret,
                                        @NotNull TextRange selectedRange,
                                        final int count,
                                        boolean avalanche) {
    BoundListOption nf = OptionsManager.INSTANCE.getNrformats();
    boolean alpha = nf.contains("alpha");
    boolean hex = nf.contains("hex");
    boolean octal = nf.contains("octal");

    @NotNull List<Pair<TextRange, SearchHelper.NumberType>>
      numberRanges = SearchHelper.findNumbersInRange(editor, selectedRange, alpha, hex, octal);

    List<String> newNumbers = new ArrayList<>();
    for (int i = 0; i < numberRanges.size(); i++) {
      Pair<TextRange, SearchHelper.NumberType> numberRange = numberRanges.get(i);
      int iCount = avalanche ? (i + 1) * count : count;
      String newNumber = changeNumberInRange(editor, numberRange, iCount, alpha, hex, octal);
      newNumbers.add(newNumber);
    }

    for (int i = newNumbers.size() - 1; i >= 0; i--) {
      // Replace text bottom up. In other direction ranges will be desynchronized after inc numbers like 99
      Pair<TextRange, SearchHelper.NumberType> rangeToReplace = numberRanges.get(i);
      String newNumber = newNumbers.get(i);
      replaceText(editor, rangeToReplace.getFirst().getStartOffset(), rangeToReplace.getFirst().getEndOffset(), newNumber);
    }

    InlayHelperKt.moveToInlayAwareOffset(caret, selectedRange.getStartOffset());
    return true;
  }

  private void exitAllSingleCommandInsertModes(@NotNull Editor editor) {
    while (CommandStateHelper.inSingleCommandMode(editor)) {
      CommandState.getInstance(editor).popModes();
      if (CommandStateHelper.inInsertMode(editor)) {
        CommandState.getInstance(editor).popModes();
      }
    }
  }

  private final List<Object> strokes = new ArrayList<>();
  private int repeatCharsCount;
  private @Nullable List<Object> lastStrokes;

  public boolean changeNumber(final @NotNull Editor editor, @NotNull Caret caret, final int count) {
    final BoundListOption nf = OptionsManager.INSTANCE.getNrformats();
    final boolean alpha = nf.contains("alpha");
    final boolean hex = nf.contains("hex");
    final boolean octal = nf.contains("octal");

    @Nullable Pair<TextRange, SearchHelper.NumberType> range = SearchHelper.findNumberUnderCursor(editor, caret, alpha, hex, octal);
    if (range == null) {
      logger.debug("no number on line");
      return false;
    }

    String newNumber = changeNumberInRange(editor, range, count, alpha, hex, octal);
    if (newNumber == null) {
      return false;
    }
    else {
      replaceText(editor, range.getFirst().getStartOffset(), range.getFirst().getEndOffset(), newNumber);
      InlayHelperKt.moveToInlayAwareOffset(caret, range.getFirst().getStartOffset() + newNumber.length() - 1);
      return true;
    }
  }

  public void reset() {
    strokes.clear();
    repeatCharsCount = 0;
    if (lastStrokes != null) {
      lastStrokes.clear();
    }
  }

  private int repeatLines;
  private int repeatColumn;
  private boolean repeatAppend;
  private boolean lastLower = true;
  private Document document;

  public @Nullable String changeNumberInRange(final @NotNull Editor editor,
                                              Pair<TextRange, SearchHelper.NumberType> range,
                                              final int count,
                                              boolean alpha,
                                              boolean hex,
                                              boolean octal) {
    String text = EditorHelper.getText(editor, range.getFirst());
    SearchHelper.NumberType numberType = range.getSecond();
    if (logger.isDebugEnabled()) {
      logger.debug("found range " + range);
      logger.debug("text=" + text);
    }
    String number = text;
    if (text.length() == 0) {
      return null;
    }

    char ch = text.charAt(0);
    if (hex && SearchHelper.NumberType.HEX.equals(numberType)) {
      if (!text.toLowerCase().startsWith(HEX_START)) throw new RuntimeException("Hex number should start with 0x: " + text);
      for (int i = text.length() - 1; i >= 2; i--) {
        int index = "abcdefABCDEF".indexOf(text.charAt(i));
        if (index >= 0) {
          lastLower = index < 6;
          break;
        }
      }

      BigInteger num = new BigInteger(text.substring(2), 16);
      num = num.add(BigInteger.valueOf(count));
      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = new BigInteger(MAX_HEX_INTEGER, 16).add(BigInteger.ONE).add(num);
      }
      number = num.toString(16);
      number = StringsKt.padStart(number, text.length() - 2, '0');

      if (!lastLower) {
        number = number.toUpperCase();
      }

      number = text.substring(0, 2) + number;
    }
    else if (octal && SearchHelper.NumberType.OCT.equals(numberType) && text.length() > 1) {
      if (!text.startsWith("0")) throw new RuntimeException("Oct number should start with 0: " + text);
      BigInteger num = new BigInteger(text, 8).add(BigInteger.valueOf(count));

      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = new BigInteger("1777777777777777777777", 8).add(BigInteger.ONE).add(num);
      }
      number = num.toString(8);
      number = "0" + StringsKt.padStart(number, text.length() - 1, '0');
    }
    else if (alpha && SearchHelper.NumberType.ALPHA.equals(numberType)) {
      if (!Character.isLetter(ch)) throw new RuntimeException("Not alpha number : " + text);
      ch += count;
      if (Character.isLetter(ch)) {
        number = String.valueOf(ch);
      }
    }
    else if (SearchHelper.NumberType.DEC.equals(numberType)) {
      if (ch != '-' && !Character.isDigit(ch)) throw new RuntimeException("Not dec number : " + text);
      boolean pad = ch == '0';
      int len = text.length();
      if (ch == '-' && text.charAt(1) == '0') {
        pad = true;
        len--;
      }

      BigInteger num = new BigInteger(text);
      num = num.add(BigInteger.valueOf(count));
      number = num.toString();

      if (!octal && pad) {
        boolean neg = false;
        if (number.charAt(0) == '-') {
          neg = true;
          number = number.substring(1);
        }
        number = StringsKt.padStart(number, len, '0');
        if (neg) {
          number = "-" + number;
        }
      }
    }

    return number;
  }

  public void addInsertListener(VimInsertListener listener) {
    insertListeners.add(listener);
  }

  public void removeInsertListener(VimInsertListener listener) {
    insertListeners.remove(listener);
  }

  private void notifyListeners(Editor editor) {
    insertListeners.forEach(listener -> listener.insertModeStarted(editor));
  }

  private int oldOffset = -1;

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
      if (!tabAction && newFragmentLength > 1 && newFragment.trim().isEmpty()) {
        return;
      }
      tabAction = false;

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

    private @NotNull List<EditorActionHandlerBase> getAdjustCaretActions(@NotNull DocumentEvent e) {
      final int delta = e.getOffset() - oldOffset;
      if (oldOffset >= 0 && delta != 0) {
        final List<EditorActionHandlerBase> positionCaretActions = new ArrayList<>();
        final String motionName = delta < 0 ? "VimMotionLeftAction" : "VimMotionRightAction";
        final EditorActionHandlerBase action = RegisterActions.findAction(motionName);
        final int count = Math.abs(delta);
        for (int i = 0; i < count; i++) {
          positionCaretActions.add(action);
        }
        return positionCaretActions;
      }
      return Collections.emptyList();
    }
  }

  private static final Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}
