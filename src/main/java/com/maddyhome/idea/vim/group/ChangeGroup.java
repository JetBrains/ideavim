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
import com.intellij.codeInsight.actions.AsyncActionExecutionService;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualModeHelperKt;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.icons.VimIcons;
import com.maddyhome.idea.vim.key.KeyHandlerKeeper;
import com.maddyhome.idea.vim.listener.VimInsertListener;
import com.maddyhome.idea.vim.newapi.IjExecutionContext;
import com.maddyhome.idea.vim.newapi.IjExecutionContextKt;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.OptionConstants;
import com.maddyhome.idea.vim.options.OptionScope;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.*;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_POS;

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

  private long lastShownTime = 0L;

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

    UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), VimStateMachine.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(((IjVimEditor) editor).getEditor()).createIndentBySize(col));

    if (firstLiner) {
      injector.getMotion().moveCaret(editor, caret, VimPlugin.getMotion().getVerticalMotionOffset(editor, caret, -1));
    }
  }

  /**
   * Inserts a new line below the caret position
   *
   * @param editor The editor to insert into
   * @param caret  The caret to insert after
   * @param col    The column to indent to
   */
  private void insertNewLineBelow(@NotNull VimEditor editor, @NotNull VimCaret caret, int col) {
    if (editor.isOneLineMode()) return;

    caret.moveToOffset(injector.getMotion().moveCaretToLineEnd(editor, caret));
    editor.setVimChangeActionSwitchMode(VimStateMachine.Mode.INSERT);
    insertText(editor, caret, "\n" + IndentConfig.create(((IjVimEditor) editor).getEditor()).createIndentBySize(col));
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

  @Override
  public void type(@NotNull VimEditor vimEditor, @NotNull ExecutionContext context, char key) {
    Editor editor = ((IjVimEditor) vimEditor).getEditor();
    DataContext ijContext = IjExecutionContextKt.getIj(context);
    final Document doc = ((IjVimEditor) vimEditor).getEditor().getDocument();
    CommandProcessor.getInstance().executeCommand(editor.getProject(), () -> ApplicationManager.getApplication()
                                                    .runWriteAction(() -> KeyHandlerKeeper.getInstance().getOriginalHandler().execute(editor, key, ijContext)), "", doc,
                                                  UndoConfirmationPolicy.DEFAULT, doc);
    MotionGroup.scrollCaretIntoView(editor);
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
    final CharSequence chars = editor.text();
    final int offset = caret.getOffset().getPoint();
    int fileSize = ((int)editor.fileSize());
    if (fileSize > 0 && offset < fileSize) {
      final CharacterHelper.CharacterType charType = CharacterHelper.charType(chars.charAt(offset), bigWord);
      if (charType != CharacterHelper.CharacterType.WHITESPACE) {
        final boolean lastWordChar =
          offset >= fileSize - 1 || CharacterHelper.charType(chars.charAt(offset + 1), bigWord) != charType;
        if (wordMotions.contains(id) && lastWordChar && motion.getCount() == 1) {
          final boolean res = deleteCharacter(editor, caret, 1, true, operatorArguments);
          if (res) {
            editor.setVimChangeActionSwitchMode(VimStateMachine.Mode.INSERT);
          }
          return res;
        }
        switch (id) {
          case VIM_MOTION_WORD_RIGHT:
            kludge = true;
            motion.setAction(injector.getActionExecutor().findVimActionOrDie(VIM_MOTION_WORD_END_RIGHT));

            break;
          case VIM_MOTION_BIG_WORD_RIGHT:
            kludge = true;
            motion.setAction(injector.getActionExecutor().findVimActionOrDie(VIM_MOTION_BIG_WORD_END_RIGHT));

            break;
          case VIM_MOTION_CAMEL_RIGHT:
            kludge = true;
            motion.setAction(injector.getActionExecutor().findVimActionOrDie(VIM_MOTION_CAMEL_END_RIGHT));

            break;
        }
      }
    }

    if (kludge) {
      int cnt = operatorArguments.getCount1() * motion.getCount();
      int pos1 = injector.getSearchHelper().findNextWordEnd(chars, offset, fileSize, cnt, bigWord, false);
      int pos2 = injector.getSearchHelper().findNextWordEnd(chars, pos1, fileSize, -cnt, bigWord, false);
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

    if (injector.getOptionService().isSet(OptionScope.GLOBAL.INSTANCE, OptionConstants.experimentalapiName, OptionConstants.experimentalapiName)) {
      Pair<TextRange, SelectionType> deleteRangeAndType =
        getDeleteRangeAndType2(editor, caret, context, argument, true, operatorArguments.withCount0(count0));
      if (deleteRangeAndType == null) return false;
      //ChangeGroupKt.changeRange(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), deleteRangeAndType.getFirst(), deleteRangeAndType.getSecond(), ((IjExecutionContext) context).getContext());
      return true;
    }
    else {
      Pair<TextRange, SelectionType> deleteRangeAndType =
        getDeleteRangeAndType(editor, caret, context, argument, true, operatorArguments.withCount0(count0));
      if (deleteRangeAndType == null) return false;
      return changeRange(editor, caret, deleteRangeAndType.getFirst(), deleteRangeAndType.getSecond(), context,
                         operatorArguments);
    }
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
    final int lines = VimChangeGroupBase.Companion.getLinesCountInVisualBlock(editor, range);
    final VimLogicalPosition startPosition = editor.offsetToLogicalPosition(range.getStartOffset());

    boolean visualBlockMode = operatorArguments.getMode() == VimStateMachine.Mode.VISUAL &&
                              operatorArguments.getSubMode() == VimStateMachine.SubMode.VISUAL_BLOCK;
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
   * @param editor            The editor to change
   * @param caret             The caret to be moved after range deletion
   * @param range             The range to change
   * @param type              The type of the range
   * @param operatorArguments
   * @return true if able to delete the range, false if not
   */
  @Override
  public boolean changeRange(@NotNull VimEditor editor,
                             @NotNull VimCaret caret,
                             @NotNull TextRange range,
                             @NotNull SelectionType type,
                             @Nullable ExecutionContext context,
                             @NotNull OperatorArguments operatorArguments) {
    int col = 0;
    int lines = 0;
    if (type == SelectionType.BLOCK_WISE) {
      lines = VimChangeGroupBase.Companion.getLinesCountInVisualBlock(editor, range);
      col = editor.offsetToLogicalPosition(range.getStartOffset()).getColumn();
      if (caret.getVimLastColumn() == VimMotionGroupBase.LAST_COLUMN) {
        col = VimMotionGroupBase.LAST_COLUMN;
      }
    }
    boolean after = range.getEndOffset() >= editor.fileSize();

    final VimLogicalPosition lp = editor.offsetToLogicalPosition(injector.getMotion().moveCaretToLineStartSkipLeading(editor, caret));

    boolean res = deleteRange(editor, caret, range, type, true, operatorArguments);
    if (res) {
      if (type == SelectionType.LINE_WISE) {
        // Please don't use `getDocument().getText().isEmpty()` because it converts CharSequence into String
        if (editor.fileSize() == 0) {
          insertBeforeCursor(editor, context);
        }
        else if (after && !EngineEditorHelperKt.endsWithNewLine(editor)) {
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
        UserDataManager.setVimChangeActionSwitchMode(((IjVimEditor) editor).getEditor(), VimStateMachine.Mode.INSERT);
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

    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    VisualModeHelperKt.vimSetSystemSelectionSilently(ijEditor.getSelectionModel(), startOffset, endOffset);

    Project project = ijEditor.getProject();
    Function0<Unit> actionExecution = () -> {
      NativeAction joinLinesAction = VimInjectorKt.getInjector().getNativeActionManager().getIndentLines();
      if (joinLinesAction != null) {
        VimInjectorKt.getInjector().getActionExecutor().executeAction(joinLinesAction, context);
      }
      return null;
    };
    Function0<Unit> afterAction = () -> {
      final int firstLine = editor.offsetToLogicalPosition(Math.min(startOffset, endOffset)).getLine();
      final int newOffset = VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
      injector.getMotion().moveCaret(editor, caret, newOffset);
      restoreCursor(editor, caret, ((IjVimCaret)caret).getCaret().getLogicalPosition().line);
      return null;
    };
    if (project != null) {
      AsyncActionExecutionService.Companion.getInstance(project)
        .withExecutionAfterAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES, actionExecution, afterAction);
    } else {
      actionExecution.invoke();
      afterAction.invoke();
    }
  }

  @Override
  public void indentLines(@NotNull VimEditor editor,
                          @NotNull VimCaret caret,
                          @NotNull ExecutionContext context,
                          int lines,
                          int dir,
                          @NotNull OperatorArguments operatorArguments) {
    int start = ((IjVimCaret) caret).getCaret().getOffset();
    int end = VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, lines - 1, true);
    indentRange(editor, caret, context, new TextRange(start, end), 1, dir, operatorArguments);
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
      indentRange(editor, caret, context, range, 1, dir, operatorArguments);
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
  @Override
  public void replaceText(@NotNull VimEditor editor, int start, int end, @NotNull String str) {
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
                          int dir,
                          @NotNull OperatorArguments operatorArguments) {
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
              deleteText(editor, new TextRange(wsoff, pos), null, caret, operatorArguments);
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

    // Just an easter egg
    if (avalanche) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastShownTime > 60_000) {
        lastShownTime = currentTime;
        ApplicationManager.getApplication().invokeLater(() -> {
          final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Wow, nice vim skills!", VimIcons.IDEAVIM, JBColor.background(), null)
            .createBalloon();
          balloon.show(JBPopupFactory.getInstance().guessBestPopupLocation(((IjVimEditor)editor).getEditor()),
                       Balloon.Position.below);
        });
      }
    }

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



  private static final Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}
