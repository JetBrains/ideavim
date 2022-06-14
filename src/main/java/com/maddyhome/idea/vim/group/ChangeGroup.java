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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.EditorEx;
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
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualModeHelperKt;
import com.maddyhome.idea.vim.handler.Motion;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.key.KeyHandlerKeeper;
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor;
import com.maddyhome.idea.vim.listener.VimInsertListener;
import com.maddyhome.idea.vim.listener.VimListenerSuppressor;
import com.maddyhome.idea.vim.newapi.*;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.register.Register;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import java.util.*;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.mark.VimMarkConstants.*;
import static com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER;

/**
 * Provides all the insert/replace related functionality
 */
public class ChangeGroup extends VimChangeGroupBase {

  public static final String VIM_MOTION_BIG_WORD_RIGHT = "VimMotionBigWordRightAction";
  public static final String VIM_MOTION_WORD_RIGHT = "VimMotionWordRightAction";
  public static final String VIM_MOTION_CAMEL_RIGHT = "VimMotionCamelRightAction";
  private static final String VIM_MOTION_WORD_END_RIGHT = "VimMotionWordEndRightAction";
  private static final String VIM_MOTION_BIG_WORD_END_RIGHT = "VimMotionBigWordEndRightAction";
  private static final String VIM_MOTION_CAMEL_END_RIGHT = "VimMotionCamelEndRightAction";
  private static final ImmutableSet<String> wordMotions = ImmutableSet.of(VIM_MOTION_WORD_RIGHT, VIM_MOTION_BIG_WORD_RIGHT, VIM_MOTION_CAMEL_RIGHT);

  @NonNls private static final String HEX_START = "0x";
  @NonNls private static final String MAX_HEX_INTEGER = "ffffffffffffffff";

  private final List<VimInsertListener> insertListeners = ContainerUtil.createLockFreeCopyOnWriteList();


  /**
   * Begin insert before the current line by creating a new blank line above the current line
   * for all carets
   *
   * @param editor The editor to insert into
   */
  @Override
  public void insertNewLineAbove(final @NotNull VimEditor editor, @NotNull ExecutionContext context) {
    if (((IjVimEditor) editor).getEditor().isOneLineMode()) return;

    // See also EditorStartNewLineBefore. That will move the caret to line start, call EditorEnter to create a new line,
    //   and then move up and call EditorLineEnd. We get better indent positioning by going to the line end of the
    //   previous line and hitting enter, especially with plain text files.
    // However, we'll use EditorStartNewLineBefore in PyCharm notebooks where the last character of the previous line
    //   may be locked with a guard

    // Note that we're deliberately bypassing MotionGroup.moveCaret to avoid side effects, most notably unncessary
    // scrolling
    Set<Caret> firstLiners = new HashSet<>();
    Set<Pair<Caret, Integer>> moves = new HashSet<>();
    for (Caret caret : ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets()) {
      final int offset;
      if (caret.getVisualPosition().line == 0) {
        // Fake indenting for the first line. Works well for plain text to match the existing indent
        offset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, new IjVimCaret(caret));
        firstLiners.add(caret);
      }
      else {
        offset = VimPlugin.getMotion().moveCaretToLineEnd(editor, caret.getLogicalPosition().line - 1, true);
      }
      moves.add(new Pair<>(caret, offset));
    }

    // Check if the "last character on previous line" has a guard
    // This is actively used in pycharm notebooks https://youtrack.jetbrains.com/issue/VIM-2495
    boolean hasGuards = moves.stream().anyMatch(it -> ((IjVimEditor) editor).getEditor().getDocument().getOffsetGuard(it.getSecond()) != null);
    if (!hasGuards) {
      for (Pair<Caret, Integer> move : moves) {
        move.getFirst().moveToOffset(move.getSecond());
      }

      initInsert(editor, context, CommandState.Mode.INSERT);
      runEnterAction(editor, context);

      for (Caret caret : ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets()) {
        if (firstLiners.contains(caret)) {
          final int offset = VimPlugin.getMotion().moveCaretToLineEnd(editor, 0, true);
          injector.getMotion().moveCaret(editor, new IjVimCaret(caret), offset);
        }
      }
    } else {
      initInsert(editor, context, CommandState.Mode.INSERT);
      runEnterAboveAction(editor, context);
    }

    MotionGroup.scrollCaretIntoView(((IjVimEditor) editor).getEditor());
  }

  /**
   * Inserts a new line above the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert above
   * @param col    The column to indent to
   */
  private void insertNewLineAbove(@NotNull VimEditor editor, @NotNull VimCaret caret, int col) {
    if (((IjVimEditor) editor).getEditor().isOneLineMode()) return;

    boolean firstLiner = false;
    if (((IjVimCaret) caret).getCaret().getVisualPosition().line == 0) {
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor,
                                                                                      caret));
      firstLiner = true;
    }
    else {
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().getVerticalMotionOffset(editor, caret, -1));
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    }

    UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(((IjVimEditor) editor).getEditor()).createIndentBySize(col));

    if (firstLiner) {
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().getVerticalMotionOffset(editor, caret, -1));
    }
  }

  /**
   * Begin insert after the current line by creating a new blank line below the current line
   * for all carets
   *  @param editor  The editor to insert into
   * @param context The data context
   */
  @Override
  public void insertNewLineBelow(final @NotNull VimEditor editor, final @NotNull ExecutionContext context) {
    if (((IjVimEditor) editor).getEditor().isOneLineMode()) return;

    for (Caret caret : ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets()) {
      injector.getMotion().moveCaret(editor, new IjVimCaret(caret), VimPlugin.getMotion().moveCaretToLineEnd(editor, new IjVimCaret(caret)));
    }

    initInsert(editor, context, CommandState.Mode.INSERT);
    runEnterAction(editor, context);

    MotionGroup.scrollCaretIntoView(((IjVimEditor) editor).getEditor());
  }

  /**
   * Inserts a new line below the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert after
   * @param col    The column to indent to
   */
  private void insertNewLineBelow(@NotNull VimEditor editor, @NotNull VimCaret caret, int col) {
    if (((IjVimEditor) editor).getEditor().isOneLineMode()) return;

    injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
    UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(((IjVimEditor) editor).getEditor()).createIndentBySize(col));
  }

  private void runEnterAction(VimEditor editor, @NotNull ExecutionContext context) {
    CommandState state = CommandState.getInstance(editor);
    if (!state.isDotRepeatInProgress()) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      final NativeAction action = VimInjectorKt.getInjector().getNativeActionManager().getEnterAction();
      if (action != null) {
        strokes.add(action);
        VimInjectorKt.getInjector().getActionExecutor().executeAction(action, context);
      }
    }
  }

  private void runEnterAboveAction(VimEditor editor, @NotNull ExecutionContext context) {
    CommandState state = CommandState.getInstance(editor);
    if (!state.isDotRepeatInProgress()) {
      // While repeating the enter action has been already executed because `initInsert` repeats the input
      final NativeAction action = VimInjectorKt.getInjector().getNativeActionManager().getCreateLineAboveCaret();
      if (action != null) {
        strokes.add(action);
        VimInjectorKt.getInjector().getActionExecutor().executeAction(action, context);
      }
    }
  }

  /**
   * Begin insert at the location of the previous insert
   *
   * @param editor The editor to insert into
   */
  @Override
  public void insertAtPreviousInsert(@NotNull VimEditor editor, @NotNull ExecutionContext context) {
    editor.removeSecondaryCarets();

    final VimCaret caret = editor.primaryCaret();
    final int offset = VimPlugin.getMotion().moveCaretToMark(editor, '^', false);
    if (offset != -1) {
      injector.getMotion().moveCaret(editor, caret, offset);
    }

    insertBeforeCursor(editor, context);
  }

  /**
   * Inserts previously inserted text
   *  @param editor  The editor to insert into
   * @param context The data context
   * @param exit    true if insert mode should be exited after the insert, false should stay in insert mode
   */
  @Override
  public void insertPreviousInsert(@NotNull VimEditor editor,
                                   @NotNull ExecutionContext context,
                                   boolean exit,
                                   @NotNull OperatorArguments operatorArguments) {
    repeatInsertText(editor, context, 1, operatorArguments);
    if (exit) {
      ModeHelper.exitInsertMode(((IjVimEditor) editor).getEditor(), ((IjExecutionContext) context).getContext(), operatorArguments);
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
  @Override
  public boolean insertRegister(@NotNull VimEditor editor, @NotNull ExecutionContext context, char key) {
    final Register register = VimPlugin.getRegister().getRegister(key);
    if (register != null) {
      final List<KeyStroke> keys = register.getKeys();
      for (KeyStroke k : keys) {
        processKey(editor, context, k);
      }
      return true;
    }

    return false;
  }

  /**
   * If the cursor is currently after the start of the current insert this deletes all the newly inserted text.
   * Otherwise it deletes all text from the cursor back to the first non-blank in the line.
   *
   * @param editor The editor to delete the text from
   * @param caret  The caret on which the action is performed
   * @return true if able to delete the text, false if not
   */
  @Override
  public boolean insertDeleteInsertedText(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    int deleteTo = UserDataManager.getVimInsertStart(((IjVimCaret) caret).getCaret()).getStartOffset();
    int offset = ((IjVimCaret) caret).getCaret().getOffset();
    if (offset == deleteTo) {
      deleteTo = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor,
                                                                       caret);
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
  @Override
  public boolean insertDeletePreviousWord(@NotNull VimEditor editor, @NotNull VimCaret caret) {
    final int deleteTo;
    if (((IjVimCaret) caret).getCaret().getLogicalPosition().column == 0) {
      deleteTo = ((IjVimCaret) caret).getCaret().getOffset() - 1;
    }
    else {
      int pointer = ((IjVimCaret) caret).getCaret().getOffset() - 1;
      final CharSequence chars = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
      while (pointer >= 0 && chars.charAt(pointer) == ' ' && chars.charAt(pointer) != '\n') pointer--;
      if (chars.charAt(pointer) == '\n') {
        deleteTo = pointer + 1;
      }
      else {
        Motion motion = VimPlugin.getMotion().findOffsetOfNextWord(editor, pointer + 1, -1, false);
        if (motion instanceof Motion.AbsoluteOffset) {
          deleteTo = ((Motion.AbsoluteOffset)motion).getOffset();
        }
        else {
          return false;
        }
      }
    }
    if (deleteTo < 0) {
      return false;
    }
    final TextRange range = new TextRange(deleteTo, ((IjVimCaret) caret).getCaret().getOffset());
    deleteRange(editor, caret, range, SelectionType.CHARACTER_WISE, true);
    return true;
  }


  private final @NotNull EditorMouseListener listener = new EditorMouseListener() {
    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
      Editor editor = event.getEditor();
      if (CommandStateHelper.inInsertMode(editor)) {
        clearStrokes(new IjVimEditor(editor));
      }
    }
  };

  @Override
  public void editorCreated(VimEditor editor) {
    EventFacade.getInstance().addEditorMouseListener(((IjVimEditor) editor).getEditor(), listener);
  }

  @Override
  public void editorReleased(VimEditor editor) {
    EventFacade.getInstance().removeEditorMouseListener(((IjVimEditor) editor).getEditor(), listener);
  }


  /**
   * Terminate insert/replace mode after the user presses Escape or Ctrl-C
   * <p>
   * DEPRECATED. Please, don't use this function directly. Use ModeHelper.exitInsertMode in file ModeExtensions.kt
   */
  @Override
  public void processEscape(@NotNull VimEditor editor, @Nullable ExecutionContext context, @NotNull OperatorArguments operatorArguments) {
    // Get the offset for marks before we exit insert mode - switching from insert to overtype subtracts one from the
    // column offset.
    int offset = ((IjVimEditor) editor).getEditor().getCaretModel().getPrimaryCaret().getOffset();
    final MarkGroup markGroup = VimPlugin.getMark();
    markGroup.setMark(editor, '^', offset);
    markGroup.setMark(editor, MARK_CHANGE_END, offset);

    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      editor.setInsertMode(true);
    }

    int cnt = lastInsert != null ? lastInsert.getCount() : 0;
    if (lastInsert != null && (lastInsert.getFlags().contains(CommandFlags.FLAG_NO_REPEAT_INSERT))) {
      cnt = 1;
    }

    if (vimDocument != null && vimDocumentListener != null) {
      vimDocument.removeChangeListener(vimDocumentListener);
      vimDocumentListener = null;
    }

    lastStrokes = new ArrayList<>(strokes);
    if (context != null) {
      repeatInsert(editor, context, cnt == 0 ? 0 : cnt - 1, true, operatorArguments);
    }

    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.INSERT) {
      updateLastInsertedTextRegister();
    }

    // The change pos '.' mark is the offset AFTER processing escape, and after switching to overtype
    offset = ((IjVimEditor) editor).getEditor().getCaretModel().getPrimaryCaret().getOffset();
    markGroup.setMark(editor, MARK_CHANGE_POS, offset);

    CommandState.getInstance(editor).popModes();
    exitAllSingleCommandInsertModes(editor);
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
  @Override
  public void processEnter(@NotNull VimEditor editor, @NotNull ExecutionContext context) {
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      editor.setInsertMode(true);
    }
    final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    final List<NativeAction> actions = VimPlugin.getKey().getActions(editor, enterKeyStroke);
    for (NativeAction action : actions) {
      if (VimInjectorKt.getInjector().getActionExecutor().executeAction(action, context)) {
        break;
      }
    }
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.REPLACE) {
      editor.setInsertMode(false);
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
  @Override
  public boolean insertCharacterAroundCursor(@NotNull VimEditor editor, @NotNull VimCaret caret, int dir) {
    boolean res = false;

    VimVisualPosition vp = caret.getVisualPosition();
    vp = new VimVisualPosition(vp.getLine() + dir, vp.getColumn(), false);
    int len = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor(), EditorHelper.visualLineToLogicalLine(((IjVimEditor) editor).getEditor(), vp.getLine()));
    if (vp.getColumn() < len) {
      int offset = EditorHelper.visualPositionToOffset(((IjVimEditor) editor).getEditor(), new VisualPosition(vp.getLine(), vp.getColumn()));
      CharSequence charsSequence = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
      if (offset < charsSequence.length()) {
        char ch = charsSequence.charAt(offset);
        ((IjVimEditor) editor).getEditor().getDocument().insertString(((IjVimCaret) caret).getCaret().getOffset(), Character.toString(ch));
        injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion()
          .getOffsetOfHorizontalMotion(editor, caret, 1, true));
        res = true;
      }
    }

    return res;
  }

  /**
   * Performs a mode switch after change action
   *  @param editor   The editor to switch mode in
   * @param context  The data context
   * @param toSwitch The mode to switch to
   */
  @Override
  public void processPostChangeModeSwitch(@NotNull VimEditor editor,
                                          @NotNull ExecutionContext context,
                                          @NotNull CommandState.Mode toSwitch) {
    if (toSwitch == CommandState.Mode.INSERT) {
      initInsert(editor, context, CommandState.Mode.INSERT);
    }
  }


  /**
   * Processes the user pressing the Insert key while in INSERT or REPLACE mode. This simply toggles the
   * Insert/Overwrite state which updates the status bar.
   *
   * @param editor The editor to toggle the state in
   */
  @Override
  public void processInsert(VimEditor editor) {
    final EditorEx editorEx = ObjectUtils.tryCast(((IjVimEditor) editor).getEditor(), EditorEx.class);
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
  @Override
  public void processCommand(@NotNull VimEditor editor, @NotNull Command cmd) {
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
  private void clearStrokes(@NotNull VimEditor editor) {
    strokes.clear();
    repeatCharsCount = 0;
    for (Caret caret : ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets()) {
      UserDataManager
        .setVimInsertStart(caret, ((IjVimEditor) editor).getEditor().getDocument().createRangeMarker(caret.getOffset(), caret.getOffset()));
    }
  }

  /**
   * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
   * return to INSERT or REPLACE mode.
   *
   * @param editor The editor to put into NORMAL mode for one command
   */
  @Override
  public void processSingleCommand(@NotNull VimEditor editor) {
    CommandState.getInstance(editor).pushModes(CommandState.Mode.INSERT_NORMAL, CommandState.SubMode.NONE);
    clearStrokes(editor);
  }

  /**
   * Delete from the cursor to the end of count - 1 lines down
   *
   * @param editor The editor to delete from
   * @param caret  VimCaret on the position to start
   * @param count  The number of lines affected
   * @return true if able to delete the text, false if not
   */
  @Override
  public boolean deleteEndOfLine(@NotNull VimEditor editor, @NotNull VimCaret caret, int count) {
    int initialOffset = ((IjVimCaret) caret).getCaret().getOffset();
    int offset = VimPlugin.getMotion().moveCaretToLineEndOffset(editor,
                                                                caret, count - 1, true);
    int lineStart = VimPlugin.getMotion().moveCaretToLineStart(editor,
                                                               caret);

    int startOffset = initialOffset;
    if (offset == initialOffset && offset != lineStart) startOffset--; // handle delete from virtual space

    //noinspection ConstantConditions
    if (offset != -1) {
      final TextRange rangeToDelete = new TextRange(startOffset, offset);
      editor.nativeCarets().stream().filter(c -> !c.equals(caret) && rangeToDelete.contains(c.getOffset().getPoint()))
        .forEach(editor::removeCaret);
      boolean res = deleteText(editor, rangeToDelete, SelectionType.CHARACTER_WISE);

      if (EngineHelperKt.getUsesVirtualSpace()) {
        injector.getMotion().moveCaret(editor, caret, startOffset);
      }
      else {
        int pos = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, -1, false);
        if (pos != -1) {
          injector.getMotion().moveCaret(editor, caret, pos);
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
  @Override
  public boolean deleteJoinLines(@NotNull VimEditor editor, @NotNull VimCaret caret, int count, boolean spaces) {
    if (count < 2) count = 2;
    int lline = ((IjVimCaret) caret).getCaret().getLogicalPosition().line;
    int total = EditorHelper.getLineCount(((IjVimEditor) editor).getEditor());
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
  @Override
  public boolean processKey(final @NotNull VimEditor editor,
                            final @NotNull ExecutionContext context,
                            final @NotNull KeyStroke key) {
    if (logger.isDebugEnabled()) {
      logger.debug("processKey(" + key + ")");
    }

    if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
      type(editor, context, key.getKeyChar());
      return true;
    }

    // Shift-space
    if (key.getKeyCode() == 32 && ((key.getModifiers() & KeyEvent.SHIFT_DOWN_MASK) != 0)) {
      type(editor, context, ' ');
      return true;
    }


    return false;
  }

  private void type(@NotNull VimEditor vimEditor, @NotNull ExecutionContext context, char key) {
    Editor editor = ((IjVimEditor) vimEditor).getEditor();
    DataContext ijContext = IjExecutionContextKt.getIj(context);
    final Document doc = ((IjVimEditor) vimEditor).getEditor().getDocument();
    CommandProcessor.getInstance().executeCommand(editor.getProject(), () -> ApplicationManager.getApplication()
                                                    .runWriteAction(() -> KeyHandlerKeeper.getInstance().getOriginalHandler().execute(editor, key, ijContext)), "", doc,
                                                  UndoConfirmationPolicy.DEFAULT, doc);
    MotionGroup.scrollCaretIntoView(editor);
  }

  @Override
  public boolean processKeyInSelectMode(final @NotNull VimEditor editor,
                                        final @NotNull ExecutionContext context,
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

  private boolean activeTemplateWithLeftRightMotion(VimEditor editor, KeyStroke keyStroke) {
    return HelperKt.isTemplateActive(((IjVimEditor) editor).getEditor()) &&
           (keyStroke.getKeyCode() == KeyEvent.VK_LEFT || keyStroke.getKeyCode() == KeyEvent.VK_RIGHT);
  }

  /**
   * Deletes count lines including the current line
   *
   * @param editor The editor to remove the lines from
   * @param count  The number of lines to delete
   * @return true if able to delete the lines, false if not
   */

  @Override
  public boolean deleteLine(@NotNull VimEditor editor, @NotNull VimCaret caret, int count) {
    int start = VimPlugin.getMotion().moveCaretToLineStart(editor,
                                                           caret);
    int offset = Math.min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor,
                                                                         caret, count - 1, true) + 1,
                          EditorHelperRt.getFileSize(((IjVimEditor) editor).getEditor()));
    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("offset=" + offset);
    }
    if (offset != -1) {
      boolean res = deleteText(editor, new TextRange(start, offset), SelectionType.LINE_WISE);
      if (res && ((IjVimCaret) caret).getCaret().getOffset() >= EditorHelperRt.getFileSize(((IjVimEditor) editor).getEditor()) && ((IjVimCaret) caret).getCaret().getOffset() != 0) {
        injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor,
                                                                                                caret, -1));
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
  @Override
  public boolean deleteJoinRange(@NotNull VimEditor editor, @NotNull VimCaret caret, @NotNull TextRange range, boolean spaces) {
    int startLine = editor.offsetToLogicalPosition(range.getStartOffset()).getLine();
    int endLine = editor.offsetToLogicalPosition(range.getEndOffset()).getLine();
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
  private boolean deleteJoinNLines(@NotNull VimEditor editor,
                                   @NotNull VimCaret caret,
                                   int startLine,
                                   int count,
                                   boolean spaces) {
    // start my moving the cursor to the very end of the first line
    injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, startLine, true));
    for (int i = 1; i < count; i++) {
      int start = VimPlugin.getMotion().moveCaretToLineEnd(editor, caret);
      int trailingWhitespaceStart = VimPlugin.getMotion().moveCaretToLineEndSkipLeadingOffset(editor,
                                                                                              caret, 0);
      boolean hasTrailingWhitespace = start != trailingWhitespaceStart + 1;

      injector.getMotion().moveCaret(editor, caret, start);
      int offset;
      if (spaces) {
        offset = VimPlugin.getMotion().moveCaretToLineStartSkipLeadingOffset(editor,
                                                                             caret, 1);
      }
      else {
        offset = VimPlugin.getMotion().moveCaretToLineStart(editor, ((IjVimCaret) caret).getCaret().getLogicalPosition().line + 1);
      }
      deleteText(editor, new TextRange(((IjVimCaret) caret).getCaret().getOffset(), offset), null);
      if (spaces && !hasTrailingWhitespace) {
        insertText(editor, caret, " ");
        injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor,
                                                                                      caret, -1, true));
      }
    }

    return true;
  }

  @Override
  public boolean joinViaIdeaByCount(@NotNull VimEditor editor, @NotNull ExecutionContext context, int count) {
    int executions = count > 1 ? count - 1 : 1;
    final boolean allowedExecution = ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets().stream().anyMatch(caret -> {
      int lline = caret.getLogicalPosition().line;
      int total = EditorHelper.getLineCount(((IjVimEditor) editor).getEditor());
      return lline + count <= total;
    });
    if (!allowedExecution) return false;
    for (int i = 0; i < executions; i++) {
      NativeAction joinLinesAction = VimInjectorKt.getInjector().getNativeActionManager().getJoinLines();
      if (joinLinesAction != null) {
        VimInjectorKt.getInjector().getActionExecutor().executeAction(joinLinesAction, context);
      }
    }
    return true;
  }

  @Override
  public void joinViaIdeaBySelections(@NotNull VimEditor editor,
                                      @NotNull ExecutionContext context,
                                      @NotNull Map<@NotNull VimCaret, @NotNull ? extends VimSelection> caretsAndSelections) {
    caretsAndSelections.forEach((caret, range) -> {
      if (!caret.isValid()) return;
      final Pair<Integer, Integer> nativeRange = range.getNativeStartAndEnd();
      ((IjVimCaret) caret).getCaret().setSelection(nativeRange.getFirst(), nativeRange.getSecond());
    });
    NativeAction joinLinesAction = VimInjectorKt.getInjector().getNativeActionManager().getJoinLines();
    if (joinLinesAction != null) {
      VimInjectorKt.getInjector().getActionExecutor().executeAction(joinLinesAction, context);
    }
    ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets().forEach(caret -> {
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
   *  @param editor  The editor to replace in
   * @param context The data context
   */
  @Override
  public void changeReplace(@NotNull VimEditor editor, @NotNull ExecutionContext context) {
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
  @Override
  public boolean changeCharacter(@NotNull VimEditor editor, @NotNull VimCaret caret, int count, char ch) {
    int col = ((IjVimCaret) caret).getCaret().getLogicalPosition().column;
    int len = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor());
    int offset = ((IjVimCaret) caret).getCaret().getOffset();
    if (len - col < count) {
      return false;
    }

    // Special case - if char is newline, only add one despite count
    int num = count;
    String space = null;
    if (ch == '\n') {
      num = 1;
      space = EditorHelper.getLeadingWhitespace(((IjVimEditor) editor).getEditor(), editor.offsetToLogicalPosition(offset).getLine());
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
      InlayHelperKt.moveToInlayAwareOffset(((IjVimCaret) caret).getCaret(), offset + slen);
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
  @Override
  public boolean changeCharacterRange(@NotNull VimEditor editor, @NotNull TextRange range, char ch) {
    if (logger.isDebugEnabled()) {
      logger.debug("change range: " + range + " to " + ch);
    }

    CharSequence chars = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
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

  @Override
  public @Nullable Pair<@NotNull TextRange, @NotNull SelectionType> getDeleteRangeAndType(@NotNull VimEditor editor,
                                                                        @NotNull VimCaret caret,
                                                                        @NotNull ExecutionContext context,
                                                                        final @NotNull Argument argument,
                                                                        boolean isChange,
                                                                        @NotNull OperatorArguments operatorArguments) {
    final TextRange range =
      injector.getMotion().getMotionRange(editor, caret, context, argument, operatorArguments);
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
      VimLogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      VimLogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (start.getLine() != end.getLine()) {
        if (!SearchHelper.anyNonWhitespace(((IjVimEditor) editor).getEditor(), range.getStartOffset(), -1) &&
            !SearchHelper.anyNonWhitespace(((IjVimEditor) editor).getEditor(), range.getEndOffset(), 1)) {
          type = SelectionType.LINE_WISE;
        }
      }
    }
    return new Pair<>(range, type);
  }

  @Override
  public @Nullable Pair<@NotNull TextRange, @NotNull SelectionType> getDeleteRangeAndType2(@NotNull VimEditor editor,
                                                                         @NotNull VimCaret caret,
                                                                         @NotNull ExecutionContext context,
                                                                         final @NotNull Argument argument,
                                                                         boolean isChange,
                                                                         @NotNull OperatorArguments operatorArguments) {
    final TextRange range = MotionGroup.getMotionRange2(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), ((IjExecutionContext) context).getContext(), argument, operatorArguments);
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
      VimLogicalPosition start = editor.offsetToLogicalPosition(range.getStartOffset());
      VimLogicalPosition end = editor.offsetToLogicalPosition(range.getEndOffset());
      if (start.getLine() != end.getLine()) {
        if (!SearchHelper.anyNonWhitespace(((IjVimEditor) editor).getEditor(), range.getStartOffset(), -1) &&
            !SearchHelper.anyNonWhitespace(((IjVimEditor) editor).getEditor(), range.getEndOffset(), 1)) {
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
  @Override
  public boolean deleteRange(@NotNull VimEditor editor,
                             @NotNull VimCaret caret,
                             @NotNull TextRange range,
                             @Nullable SelectionType type,
                             boolean isChange) {

    // Update the last column before we delete, or we might be retrieving the data for a line that no longer exists
    UserDataManager.setVimLastColumn(((IjVimCaret) caret).getCaret(), InlayHelperKt.getInlayAwareVisualColumn(((IjVimCaret) caret).getCaret()));

    boolean removeLastNewLine = removeLastNewLine(editor, range, type);
    final boolean res = deleteText(editor, range, type);
    if (removeLastNewLine) {
      int textLength = ((IjVimEditor) editor).getEditor().getDocument().getTextLength();
      ((IjVimEditor) editor).getEditor().getDocument().deleteString(textLength - 1, textLength);
    }

    if (res) {
      int pos = EditorHelper.normalizeOffset(((IjVimEditor) editor).getEditor(), range.getStartOffset(), isChange);
      if (type == SelectionType.LINE_WISE) {
        pos = VimPlugin.getMotion()
          .moveCaretToLineWithStartOfLineOption(editor, editor.offsetToLogicalPosition(pos).getLine(),
                                                caret);
      }
      injector.getMotion().moveCaret(editor, caret, pos);
    }
    return res;
  }

  private boolean removeLastNewLine(@NotNull VimEditor editor, @NotNull TextRange range, @Nullable SelectionType type) {
    int endOffset = range.getEndOffset();
    int fileSize = EditorHelperRt.getFileSize(((IjVimEditor) editor).getEditor());
    if (endOffset > fileSize) {
      if (injector.getOptionService().isSet(OptionScope.GLOBAL.INSTANCE, OptionConstants.ideastrictmodeName, OptionConstants.ideastrictmodeName)) {
        throw new IllegalStateException("Incorrect offset. File size: " + fileSize + ", offset: " + endOffset);
      }
      endOffset = fileSize;
    }
    return type == SelectionType.LINE_WISE &&
           range.getStartOffset() != 0 &&
           ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence().charAt(endOffset - 1) != '\n' &&
           endOffset == fileSize;
  }

  @Override
  public void insertLineAround(@NotNull VimEditor editor, @NotNull ExecutionContext context, int shift) {
    com.maddyhome.idea.vim.newapi.ChangeGroupKt.insertLineAround(editor, context, shift);
  }

  @Override
  public boolean deleteRange2(@NotNull VimEditor editor,
                              @NotNull VimCaret caret,
                              @NotNull TextRange range,
                              @NotNull SelectionType type) {
    return com.maddyhome.idea.vim.newapi.ChangeGroupKt.deleteRange(editor, caret, range, type);
  }

  /**
   * Delete count characters and then enter insert mode
   *
   * @param editor The editor to change
   * @param caret  The caret to be moved
   * @param count  The number of characters to change
   * @return true if able to delete count characters, false if not
   */
  @Override
  public boolean changeCharacters(@NotNull VimEditor editor, @NotNull VimCaret caret, int count) {
    int len = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor());
    int col = ((IjVimCaret) caret).getCaret().getLogicalPosition().column;
    if (col + count >= len) {
      return changeEndOfLine(editor, caret, 1);
    }

    boolean res = deleteCharacter(editor, caret, count, true);
    if (res) {
      UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
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
  @Override
  public boolean changeEndOfLine(@NotNull VimEditor editor, @NotNull VimCaret caret, int count) {
    boolean res = deleteEndOfLine(editor, caret, count);
    if (res) {
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineEnd(editor, caret));
      UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
    }

    return res;
  }

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param caret    The caret on which the motion is supposed to be performed
   * @param context  The data context
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  @Override
  public boolean changeMotion(@NotNull VimEditor editor,
                              @NotNull VimCaret caret,
                              @NotNull ExecutionContext context,
                              @NotNull Argument argument,
                              @NotNull OperatorArguments operatorArguments) {
    int count0 = operatorArguments.getCount0();
    // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
    final Command motion = argument.getMotion();

    String id = motion.getAction().getId();
    boolean kludge = false;
    boolean bigWord = id.equals(VIM_MOTION_BIG_WORD_RIGHT);
    final CharSequence chars = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
    final int offset = ((IjVimCaret) caret).getCaret().getOffset();
    int fileSize = EditorHelperRt.getFileSize(((IjVimEditor) editor).getEditor());
    if (fileSize > 0 && offset < fileSize) {
      final CharacterHelper.CharacterType charType = CharacterHelper.charType(chars.charAt(offset), bigWord);
      if (charType != CharacterHelper.CharacterType.WHITESPACE) {
        final boolean lastWordChar =
          offset >= fileSize - 1 || CharacterHelper.charType(chars.charAt(offset + 1), bigWord) != charType;
        if (wordMotions.contains(id) && lastWordChar && motion.getCount() == 1) {
          final boolean res = deleteCharacter(editor, caret, 1, true);
          if (res) {
            UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
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
      int cnt = operatorArguments.getCount1() * motion.getCount();
      int pos1 = SearchHelper.findNextWordEnd(chars, offset, fileSize, cnt, bigWord, false);
      int pos2 = SearchHelper.findNextWordEnd(chars, pos1, fileSize, -cnt, bigWord, false);
      if (logger.isDebugEnabled()) {
        logger.debug("pos=" + offset);
        logger.debug("pos1=" + pos1);
        logger.debug("pos2=" + pos2);
        logger.debug("count=" + operatorArguments.getCount1());
        logger.debug("arg.count=" + motion.getCount());
      }
      if (pos2 == offset) {
        if (operatorArguments.getCount1() > 1) {
          count0--;
        }
        else if (motion.getCount() > 1) {
          motion.setCount(motion.getCount() - 1);
        }
        else {
          motion.setFlags(EnumSet.noneOf(CommandFlags.class));
        }
      }
    }

    if (VimPlugin.getOptionService().isSet(OptionScope.GLOBAL.INSTANCE, OptionConstants.experimentalapiName, OptionConstants.experimentalapiName)) {
      Pair<TextRange, SelectionType> deleteRangeAndType =
        getDeleteRangeAndType2(editor, caret, context, argument, true, operatorArguments.withCount0(count0));
      if (deleteRangeAndType == null) return false;
      ChangeGroupKt.changeRange(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), deleteRangeAndType.getFirst(), deleteRangeAndType.getSecond(), ((IjExecutionContext) context).getContext());
      return true;
    }
    else {
      Pair<TextRange, SelectionType> deleteRangeAndType =
        getDeleteRangeAndType(editor, caret, context, argument, true, operatorArguments.withCount0(count0));
      if (deleteRangeAndType == null) return false;
      return changeRange(editor, caret, deleteRangeAndType.getFirst(), deleteRangeAndType.getSecond(), context);
    }
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
  public static int getLinesCountInVisualBlock(@NotNull VimEditor editor, @NotNull TextRange range) {
    final int[] startOffsets = range.getStartOffsets();
    if (startOffsets.length == 0) return 0;
    final VimLogicalPosition firstStart = editor.offsetToLogicalPosition(startOffsets[0]);
    final VimLogicalPosition lastStart = editor.offsetToLogicalPosition(startOffsets[range.size() - 1]);
    return lastStart.getLine() - firstStart.getLine() + 1;
  }

  /**
   * Toggles the case of count characters
   *
   * @param editor The editor to change
   * @param caret  The caret on which the operation is performed
   * @param count  The number of characters to change
   * @return true if able to change count characters
   */
  @Override
  public boolean changeCaseToggleCharacter(@NotNull VimEditor editor, @NotNull VimCaret caret, int count) {
    final int offset = VimPlugin.getMotion().getOffsetOfHorizontalMotion(editor, caret, count, true);
    if (offset == -1) {
      return false;
    }
    changeCase(editor, ((IjVimCaret) caret).getCaret().getOffset(), offset, CharacterHelper.CASE_TOGGLE);
    injector.getMotion().moveCaret(editor, caret, EditorHelper.normalizeOffset(((IjVimEditor) editor).getEditor(), offset, false));
    return true;
  }

  @Override
  public boolean blockInsert(@NotNull VimEditor editor,
                             @NotNull ExecutionContext context,
                             @NotNull TextRange range,
                             boolean append,
                             @NotNull OperatorArguments operatorArguments) {
    final int lines = getLinesCountInVisualBlock(editor, range);
    final VimLogicalPosition startPosition = editor.offsetToLogicalPosition(range.getStartOffset());

    boolean visualBlockMode = operatorArguments.getMode() == CommandState.Mode.VISUAL &&
                              operatorArguments.getSubMode() == CommandState.SubMode.VISUAL_BLOCK;
    for (Caret caret : ((IjVimEditor) editor).getEditor().getCaretModel().getAllCarets()) {
      final int line = startPosition.getLine();
      int column = startPosition.getColumn();
      if (!visualBlockMode) {
        column = 0;
      }
      else if (append) {
        column += range.getMaxLength();
        if (UserDataManager.getVimLastColumn(caret) == VimMotionGroupBase.LAST_COLUMN) {
          column = VimMotionGroupBase.LAST_COLUMN;
        }
      }

      final int lineLength = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor(), line);
      if (column < VimMotionGroupBase.LAST_COLUMN && lineLength < column) {
        final String pad = EditorHelper.pad(((IjVimEditor) editor).getEditor(), ((IjExecutionContext) context).getContext(), line, column);
        final int offset = ((IjVimEditor) editor).getEditor().getDocument().getLineEndOffset(line);
        insertText(editor, new IjVimCaret(caret), offset, pad);
      }

      if (visualBlockMode || !append) {
        InlayHelperKt.moveToInlayAwareLogicalPosition(caret, new LogicalPosition(line, column));
      }
      if (visualBlockMode) {
        setInsertRepeat(lines, column, append);
      }
    }

    if (visualBlockMode || !append) {
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
  @Override
  public boolean changeCaseRange(@NotNull VimEditor editor, @NotNull VimCaret caret, @NotNull TextRange range, char type) {
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    for (int i = ends.length - 1; i >= 0; i--) {
      changeCase(editor, starts[i], ends[i], type);
    }
    injector.getMotion().moveCaret(editor, caret, range.getStartOffset());
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
  private void changeCase(@NotNull VimEditor editor, int start, int end, char type) {
    if (start > end) {
      int t = end;
      end = start;
      start = t;
    }
    end = EditorHelper.normalizeOffset(((IjVimEditor) editor).getEditor(), end);

    CharSequence chars = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < end; i++) {
      sb.append(CharacterHelper.changeCase(chars.charAt(i), type));
    }
    replaceText(editor, start, end, sb.toString());
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
  @Override
  public boolean changeRange(@NotNull VimEditor editor,
                             @NotNull VimCaret caret,
                             @NotNull TextRange range,
                             @NotNull SelectionType type,
                             ExecutionContext context) {
    int col = 0;
    int lines = 0;
    if (type == SelectionType.BLOCK_WISE) {
      lines = getLinesCountInVisualBlock(editor, range);
      col = editor.offsetToLogicalPosition(range.getStartOffset()).getColumn();
      if (UserDataManager.getVimLastColumn(((IjVimCaret) caret).getCaret()) == VimMotionGroupBase.LAST_COLUMN) {
        col = VimMotionGroupBase.LAST_COLUMN;
      }
    }
    boolean after = range.getEndOffset() >= EditorHelperRt.getFileSize(((IjVimEditor) editor).getEditor());

    final VimLogicalPosition lp = editor.offsetToLogicalPosition(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));

    boolean res = deleteRange(editor, caret, range, type, true);
    if (res) {
      if (type == SelectionType.LINE_WISE) {
        // Please don't use `getDocument().getText().isEmpty()` because it converts CharSequence into String
        if (((IjVimEditor) editor).getEditor().getDocument().getTextLength() == 0) {
          insertBeforeCursor(editor, context);
        }
        else if (after && !EditorHelperRt.endsWithNewLine(((IjVimEditor) editor).getEditor())) {
          insertNewLineBelow(editor, caret, lp.getColumn());
        }
        else {
          insertNewLineAbove(editor, caret, lp.getColumn());
        }
      }
      else {
        if (type == SelectionType.BLOCK_WISE) {
          setInsertRepeat(lines, col, false);
        }
        UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), CommandState.Mode.INSERT);
      }
    }
    else {
      insertBeforeCursor(editor, context);
    }

    return true;
  }

  private void restoreCursor(@NotNull VimEditor editor, @NotNull VimCaret caret, int startLine) {
    if (!caret.equals(editor.primaryCaret())) {
      ((IjVimEditor) editor).getEditor().getCaretModel().addCaret(
        ((IjVimEditor) editor).getEditor().offsetToVisualPosition(VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, startLine)), false);
    }
  }

  /**
   * Changes the case of all the character moved over by the motion argument.
   *
   * @param editor   The editor to change
   * @param caret    The caret on which motion pretends to be performed
   * @param context  The data context
   * @param type     The case change type (TOGGLE, UPPER, LOWER)
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  @Override
  public boolean changeCaseMotion(@NotNull VimEditor editor,
                                  @NotNull VimCaret caret,
                                  ExecutionContext context,
                                  char type,
                                  @NotNull Argument argument,
                                  @NotNull OperatorArguments operatorArguments) {
    final TextRange range = injector.getMotion().getMotionRange(editor, caret, context, argument,
                                                       operatorArguments);
    return range != null && changeCaseRange(editor, caret, range, type);
  }

  @Override
  public boolean reformatCodeMotion(@NotNull VimEditor editor,
                                    @NotNull VimCaret caret,
                                    ExecutionContext context,
                                    @NotNull Argument argument,
                                    @NotNull OperatorArguments operatorArguments) {
    final TextRange range = injector.getMotion().getMotionRange(editor, caret, context, argument,
                                                       operatorArguments);
    return range != null && reformatCodeRange(editor, caret, range);
  }

  @Override
  public void reformatCodeSelection(@NotNull VimEditor editor, @NotNull VimCaret caret, @NotNull VimSelection range) {
    final TextRange textRange = range.toVimTextRange(true);
    reformatCodeRange(editor, caret, textRange);
  }

  private boolean reformatCodeRange(@NotNull VimEditor editor, @NotNull VimCaret caret, @NotNull TextRange range) {
    int[] starts = range.getStartOffsets();
    int[] ends = range.getEndOffsets();
    final int firstLine = editor.offsetToLogicalPosition(range.getStartOffset()).getLine();
    for (int i = ends.length - 1; i >= 0; i--) {
      final int startOffset = EditorHelper.getLineStartForOffset(((IjVimEditor) editor).getEditor(), starts[i]);
      final int endOffset = EditorHelper.getLineEndForOffset(((IjVimEditor) editor).getEditor(), ends[i] - (startOffset == ends[i] ? 0 : 1));
      reformatCode(editor, startOffset, endOffset);
    }
    final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    injector.getMotion().moveCaret(editor, caret, newOffset);
    return true;
  }

  private void reformatCode(@NotNull VimEditor editor, int start, int end) {
    final Project project = ((IjVimEditor) editor).getEditor().getProject();
    if (project == null) return;
    final PsiFile file = PsiUtilBase.getPsiFileInEditor(((IjVimEditor) editor).getEditor(), project);
    if (file == null) return;
    final com.intellij.openapi.util.TextRange textRange = com.intellij.openapi.util.TextRange.create(start, end);
    CodeStyleManager.getInstance(project).reformatText(file, Collections.singletonList(textRange));
  }

  @Override
  public void autoIndentMotion(@NotNull VimEditor editor,
                               @NotNull VimCaret caret,
                               @NotNull ExecutionContext context,
                               @NotNull Argument argument,
                               @NotNull OperatorArguments operatorArguments) {
    final TextRange range = injector.getMotion().getMotionRange(editor, caret, context, argument, operatorArguments);
    if (range != null) {
      autoIndentRange(editor, caret, context,
                      new TextRange(range.getStartOffset(), EngineHelperKt.getEndOffsetInclusive(range)));
    }
  }

  @Override
  public void autoIndentRange(@NotNull VimEditor editor,
                              @NotNull VimCaret caret,
                              @NotNull ExecutionContext context,
                              @NotNull TextRange range) {
    final int startOffset = injector.getEngineEditorHelper().getLineStartForOffset(editor, range.getStartOffset());
    final int endOffset = injector.getEngineEditorHelper().getLineEndForOffset(editor, range.getEndOffset());

    VisualModeHelperKt.vimSetSystemSelectionSilently(((IjVimEditor) editor).getEditor().getSelectionModel(), startOffset, endOffset);

    NativeAction joinLinesAction = VimInjectorKt.getInjector().getNativeActionManager().getIndentLines();
    if (joinLinesAction != null) {
      VimInjectorKt.getInjector().getActionExecutor().executeAction(joinLinesAction, context);
    }

    final int firstLine = editor.offsetToLogicalPosition(Math.min(startOffset, endOffset)).getLine();
    final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    injector.getMotion().moveCaret(editor, caret, newOffset);
    restoreCursor(editor, caret, ((IjVimCaret) caret).getCaret().getLogicalPosition().line);
  }

  @Override
  public void indentLines(@NotNull VimEditor editor,
                          @NotNull VimCaret caret,
                          @NotNull ExecutionContext context,
                          int lines,
                          int dir) {
    int start = ((IjVimCaret) caret).getCaret().getOffset();
    int end = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, lines - 1, true);
    indentRange(editor, caret, context, new TextRange(start, end), 1, dir);
  }

  @Override
  public void indentMotion(@NotNull VimEditor editor,
                           @NotNull VimCaret caret,
                           @NotNull ExecutionContext context,
                           @NotNull Argument argument,
                           int dir,
                           @NotNull OperatorArguments operatorArguments) {
    final TextRange range =
      injector.getMotion().getMotionRange(editor, caret, context, argument, operatorArguments);
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
  private void replaceText(@NotNull VimEditor editor, int start, int end, @NotNull String str) {
    ((IjVimEditor) editor).getEditor().getDocument().replaceString(start, end, str);

    final int newEnd = start + str.length();
    VimPlugin.getMark().setChangeMarks(editor, new TextRange(start, newEnd));
    VimPlugin.getMark().setMark(editor, MARK_CHANGE_POS, newEnd);
  }

  @Override
  public void indentRange(@NotNull VimEditor editor,
                          @NotNull VimCaret caret,
                          @NotNull ExecutionContext context,
                          @NotNull TextRange range,
                          int count,
                          int dir) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
    }

    // Update the last column before we indent, or we might be retrieving the data for a line that no longer exists
    UserDataManager.setVimLastColumn(((IjVimCaret) caret).getCaret(), InlayHelperKt.getInlayAwareVisualColumn(((IjVimCaret) caret).getCaret()));

    IndentConfig indentConfig = IndentConfig.create(((IjVimEditor) editor).getEditor(), ((IjExecutionContext) context).getContext());

    final int sline = editor.offsetToLogicalPosition(range.getStartOffset()).getLine();
    final VimLogicalPosition endLogicalPosition = editor.offsetToLogicalPosition(range.getEndOffset());
    final int eline =
      endLogicalPosition.getColumn() == 0 ? Math.max(endLogicalPosition.getLine() - 1, 0) : endLogicalPosition.getLine();

    if (range.isMultiple()) {
      final int from = editor.offsetToLogicalPosition(range.getStartOffset()).getColumn();
      if (dir == 1) {
        // Right shift blockwise selection
        final String indent = indentConfig.createIndentByCount(count);

        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor(), l);
          if (len > from) {
            VimLogicalPosition spos = new VimLogicalPosition(l, from, false);
            insertText(editor, caret, spos, indent);
          }
        }
      }
      else {
        // Left shift blockwise selection
        CharSequence chars = ((IjVimEditor) editor).getEditor().getDocument().getCharsSequence();
        for (int l = sline; l <= eline; l++) {
          int len = EditorHelper.getLineLength(((IjVimEditor) editor).getEditor(), l);
          if (len > from) {
            VimLogicalPosition spos = new VimLogicalPosition(l, from, false);
            VimLogicalPosition epos = new VimLogicalPosition(l, from + indentConfig.getTotalIndent(count) - 1, false);
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
        final int soff = injector.getEngineEditorHelper().getLineStartOffset(editor, l);
        final int eoff = injector.getEngineEditorHelper().getLineEndOffset(editor, l, true);
        final int woff = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, l);
        final int col = ((IjVimEditor) editor).getEditor().offsetToVisualPosition(woff).getColumn();
        final int limit = Math.max(0, col + dir * indentConfig.getTotalIndent(count));
        if (col > 0 || soff != eoff) {
          final String indent = indentConfig.createIndentBySize(limit);
          replaceText(editor, soff, woff, indent);
        }
      }
    }

    if (!CommandStateHelper.inInsertMode(((IjVimEditor) editor).getEditor())) {
      if (!range.isMultiple()) {
        injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineWithStartOfLineOption(editor, sline, caret));
      }
      else {
        injector.getMotion().moveCaret(editor, caret, range.getStartOffset());
      }
    }

    UserDataManager.setVimLastColumn(((IjVimCaret) caret).getCaret(), caret.getVisualPosition().getColumn());
  }


  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @return true if able to sort the text, false if not
   */
  public boolean sortRange(@NotNull VimEditor editor, @NotNull LineRange range, @NotNull Comparator<String> lineComparator) {
    final int startLine = range.startLine;
    final int endLine = range.endLine;
    final int count = endLine - startLine + 1;
    if (count < 2) {
      return false;
    }

    final int startOffset = ((IjVimEditor) editor).getEditor().getDocument().getLineStartOffset(startLine);
    final int endOffset = ((IjVimEditor) editor).getEditor().getDocument().getLineEndOffset(endLine);

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
  private boolean sortTextRange(@NotNull VimEditor editor,
                                int start,
                                int end,
                                @NotNull Comparator<String> lineComparator) {
    final String selectedText = ((IjVimEditor) editor).getEditor().getDocument().getText(new TextRangeInterval(start, end));
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
  @Override
  public boolean changeNumberVisualMode(final @NotNull VimEditor editor,
                                        @NotNull VimCaret caret,
                                        @NotNull TextRange selectedRange,
                                        final int count,
                                        boolean avalanche) {
    String nf = ((VimString) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(editor), OptionConstants.nrformatsName, OptionConstants.nrformatsName)).getValue();
    boolean alpha = nf.contains("alpha");
    boolean hex = nf.contains("hex");
    boolean octal = nf.contains("octal");

    @NotNull List<Pair<TextRange, SearchHelper.NumberType>> numberRanges =
      SearchHelper.findNumbersInRange(((IjVimEditor) editor).getEditor(), selectedRange, alpha, hex, octal);

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

    InlayHelperKt.moveToInlayAwareOffset(((IjVimCaret) caret).getCaret(), selectedRange.getStartOffset());
    return true;
  }

  private void exitAllSingleCommandInsertModes(@NotNull VimEditor editor) {
    while (CommandStateHelper.inSingleCommandMode(((IjVimEditor) editor).getEditor())) {
      CommandState.getInstance(editor).popModes();
      if (CommandStateHelper.inInsertMode(((IjVimEditor) editor).getEditor())) {
        CommandState.getInstance(editor).popModes();
      }
    }
  }

  @Override
  public boolean changeNumber(final @NotNull VimEditor editor, @NotNull VimCaret caret, final int count) {
    final String nf = ((VimString) VimPlugin.getOptionService().getOptionValue(new OptionScope.LOCAL(editor), OptionConstants.nrformatsName, OptionConstants.nrformatsName)).getValue();
    final boolean alpha = nf.contains("alpha");
    final boolean hex = nf.contains("hex");
    final boolean octal = nf.contains("octal");

    @Nullable Pair<TextRange, SearchHelper.NumberType> range =
      SearchHelper.findNumberUnderCursor(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), alpha, hex, octal);
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
      InlayHelperKt.moveToInlayAwareOffset(((IjVimCaret) caret).getCaret(), range.getFirst().getStartOffset() + newNumber.length() - 1);
      return true;
    }
  }

  @Override
  public void reset() {
    strokes.clear();
    repeatCharsCount = 0;
    if (lastStrokes != null) {
      lastStrokes.clear();
    }
  }

  @Override
  public void saveStrokes(String newStrokes) {
    char[] chars = newStrokes.toCharArray();
    strokes.add(chars);
  }

  public @Nullable String changeNumberInRange(final @NotNull VimEditor editor,
                                              Pair<TextRange, SearchHelper.NumberType> range,
                                              final int count,
                                              boolean alpha,
                                              boolean hex,
                                              boolean octal) {
    String text = EditorHelper.getText(((IjVimEditor) editor).getEditor(), range.getFirst());
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
      if (!text.toLowerCase().startsWith(HEX_START)) {
        throw new RuntimeException("Hex number should start with 0x: " + text);
      }
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

  @Override
  public void notifyListeners(@NotNull VimEditor editor) {
    insertListeners.forEach(listener -> listener.insertModeStarted(((IjVimEditor)editor).getEditor()));
  }

  @Override
  @TestOnly
  public void resetRepeat() {
    setInsertRepeat(0, 0, false);
  }

  private void updateLastInsertedTextRegister() {
    StringBuilder textToPutRegister = new StringBuilder();
    if (lastStrokes != null) {
      for (Object lastStroke : lastStrokes) {
        if (lastStroke instanceof char[]) {
          final char[] chars = (char[])lastStroke;
          textToPutRegister.append(new String(chars));
        }
      }
    }
    VimPlugin.getRegister().storeTextSpecial(LAST_INSERTED_TEXT_REGISTER, textToPutRegister.toString());
  }


  private static final Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}
