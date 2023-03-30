/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.actions.AsyncActionExecutionService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.containers.ContainerUtil;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.*;
import com.maddyhome.idea.vim.common.IndentConfig;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.group.visual.VimSelection;
import com.maddyhome.idea.vim.group.visual.VisualModeHelperKt;
import com.maddyhome.idea.vim.handler.Motion;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.icons.VimIcons;
import com.maddyhome.idea.vim.key.KeyHandlerKeeper;
import com.maddyhome.idea.vim.listener.VimInsertListener;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContextKt;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.api.VimInjectorKt.options;

/**
 * Provides all the insert/replace related functionality
 */
public class ChangeGroup extends VimChangeGroupBase {

  private final List<VimInsertListener> insertListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private long lastShownTime = 0L;


  private final @NotNull EditorMouseListener listener = new EditorMouseListener() {
    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
      Editor editor = event.getEditor();
      if (CommandStateHelper.inInsertMode(editor)) {
        clearStrokes(new IjVimEditor(editor));
      }
    }
  };

  public void editorCreated(Editor editor, @NotNull Disposable disposable) {
    EventFacade.getInstance().addEditorMouseListener(editor, listener, disposable);
  }

  public void editorReleased(Editor editor) {
    EventFacade.getInstance().removeEditorMouseListener(editor, listener);
  }

  @Override
  public void type(@NotNull VimEditor vimEditor, @NotNull ExecutionContext context, char key) {
    Editor editor = ((IjVimEditor) vimEditor).getEditor();
    DataContext ijContext = IjEditorExecutionContextKt.getIj(context);
    final Document doc = ((IjVimEditor) vimEditor).getEditor().getDocument();
    CommandProcessor.getInstance().executeCommand(editor.getProject(), () -> ApplicationManager.getApplication()
                                                    .runWriteAction(() -> KeyHandlerKeeper.getInstance().getOriginalHandler().execute(editor, key, ijContext)), "", doc,
                                                  UndoConfirmationPolicy.DEFAULT, doc);
    injector.getScroll().scrollCaretIntoView(vimEditor);
  }



  @Override
  public @Nullable Pair<@NotNull TextRange, @NotNull SelectionType> getDeleteRangeAndType2(@NotNull VimEditor editor,
                                                                         @NotNull VimCaret caret,
                                                                         @NotNull ExecutionContext context,
                                                                         final @NotNull Argument argument,
                                                                         boolean isChange,
                                                                         @NotNull OperatorArguments operatorArguments) {
    final TextRange range = MotionGroup.getMotionRange2(((IjVimEditor) editor).getEditor(), ((IjVimCaret) caret).getCaret(), ((IjEditorExecutionContext) context).getContext(), argument, operatorArguments);
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
      BufferPosition start = editor.offsetToBufferPosition(range.getStartOffset());
      BufferPosition end = editor.offsetToBufferPosition(range.getEndOffset());
      if (start.getLine() != end.getLine()) {
        int offset1 = range.getStartOffset();
        if (!EngineEditorHelperKt.anyNonWhitespace(editor, offset1, -1)) {
          int offset = range.getEndOffset();
          if (!EngineEditorHelperKt.anyNonWhitespace(editor, offset, 1)) {
            type = SelectionType.LINE_WISE;
          }
        }
      }
    }
    return new Pair<>(range, type);
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
    boolean allowWrap = options(injector, editor).getWhichwrap().contains("~");

    Motion motion = injector.getMotion().getHorizontalMotion(editor, caret, count, true, allowWrap);
    if (motion instanceof Motion.Error) return false;

    changeCase(editor, caret, caret.getOffset().getPoint(), ((Motion.AbsoluteOffset)motion).getOffset(), CharacterHelper.CASE_TOGGLE);

    motion = injector.getMotion().getHorizontalMotion(editor, caret, count, false, allowWrap); // same but without allow end because we can change till end, but can't move caret there
    if (motion instanceof Motion.AbsoluteOffset) {
      caret.moveToOffset(EngineEditorHelperKt.normalizeOffset(editor, ((Motion.AbsoluteOffset)motion).getOffset(), false));
    }
    return true;
  }

  @Override
  public boolean blockInsert(@NotNull VimEditor editor,
                             @NotNull ExecutionContext context,
                             @NotNull TextRange range,
                             boolean append,
                             @NotNull OperatorArguments operatorArguments) {
    final int lines = VimChangeGroupBase.Companion.getLinesCountInVisualBlock(editor, range);
    final BufferPosition startPosition = editor.offsetToBufferPosition(range.getStartOffset());

    boolean visualBlockMode = operatorArguments.getMode() == VimStateMachine.Mode.VISUAL &&
                              operatorArguments.getSubMode() == VimStateMachine.SubMode.VISUAL_BLOCK;
    for (VimCaret caret : editor.carets()) {
      final int line = startPosition.getLine();
      int column = startPosition.getColumn();
      if (!visualBlockMode) {
        column = 0;
      }
      else if (append) {
        column += range.getMaxLength();
        if (caret.getVimLastColumn() == VimMotionGroupBase.LAST_COLUMN) {
          column = VimMotionGroupBase.LAST_COLUMN;
        }
      }

      final int lineLength = EngineEditorHelperKt.lineLength(editor, line);
      if (column < VimMotionGroupBase.LAST_COLUMN && lineLength < column) {
        final String pad = EditorHelper.pad(((IjVimEditor) editor).getEditor(), ((IjEditorExecutionContext) context).getContext(), line, column);
        final int offset = editor.getLineEndOffset(line);
        insertText(editor, caret, offset, pad);
      }

      if (visualBlockMode || !append) {
        InlayHelperKt.moveToInlayAwareLogicalPosition(((IjVimCaret)caret).getCaret(), new LogicalPosition(line, column));
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
      changeCase(editor, caret, starts[i], ends[i], type);
    }
    caret.moveToOffset(range.getStartOffset());
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
  private void changeCase(@NotNull VimEditor editor, @NotNull VimCaret caret, int start, int end, char type) {
    if (start > end) {
      int t = end;
      end = start;
      start = t;
    }
    end = EngineEditorHelperKt.normalizeOffset(editor, end, true);

    CharSequence chars = editor.text();
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < end; i++) {
      sb.append(CharacterHelper.changeCase(chars.charAt(i), type));
    }
    replaceText(editor, caret, start, end, sb.toString());
  }

  private void restoreCursor(@NotNull VimEditor editor, @NotNull VimCaret caret, int startLine) {
    if (!caret.equals(editor.primaryCaret())) {
      ((IjVimEditor) editor).getEditor().getCaretModel().addCaret(
        ((IjVimEditor) editor).getEditor().offsetToVisualPosition(injector.getMotion().moveCaretToLineStartSkipLeading(editor, startLine)), false);
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
    final int firstLine = editor.offsetToBufferPosition(range.getStartOffset()).getLine();
    for (int i = ends.length - 1; i >= 0; i--) {
      final int startOffset = EngineEditorHelperKt.getLineStartForOffset(editor, starts[i]);
      final int offset = ends[i] - (startOffset == ends[i] ? 0 : 1);
      final int endOffset = EngineEditorHelperKt.getLineEndForOffset(editor, offset);
      reformatCode(editor, startOffset, endOffset);
    }
    final int newOffset = injector.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
    caret.moveToOffset(newOffset);
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
    final int startOffset = EngineEditorHelperKt.getLineStartForOffset(editor, range.getStartOffset());
    final int endOffset = EngineEditorHelperKt.getLineEndForOffset(editor, range.getEndOffset());

    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    VisualModeHelperKt.vimSetSystemSelectionSilently(ijEditor.getSelectionModel(), startOffset, endOffset);

    Project project = ijEditor.getProject();
    Function0<Unit> actionExecution = () -> {
      NativeAction joinLinesAction = VimInjectorKt.getInjector().getNativeActionManager().getIndentLines();
      if (joinLinesAction != null) {
        VimInjectorKt.getInjector().getActionExecutor().executeAction(editor, joinLinesAction, context);
      }
      return null;
    };
    Function0<Unit> afterAction = () -> {
      final int firstLine = editor.offsetToBufferPosition(Math.min(startOffset, endOffset)).getLine();
      final int newOffset = injector.getMotion().moveCaretToLineStartSkipLeading(editor, firstLine);
      caret.moveToOffset(newOffset);
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
    int start = caret.getOffset().getPoint();
    int end = injector.getMotion().moveCaretToRelativeLineEnd(editor, caret, lines - 1, true);
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

    // Remember the current caret column
    final int intendedColumn = caret.getVimLastColumn();

    IndentConfig indentConfig = IndentConfig.create(((IjVimEditor) editor).getEditor(), ((IjEditorExecutionContext) context).getContext());

    final int sline = editor.offsetToBufferPosition(range.getStartOffset()).getLine();
    final BufferPosition endLogicalPosition = editor.offsetToBufferPosition(range.getEndOffset());
    final int eline =
      endLogicalPosition.getColumn() == 0 ? Math.max(endLogicalPosition.getLine() - 1, 0) : endLogicalPosition.getLine();

    if (range.isMultiple()) {
      final int from = editor.offsetToBufferPosition(range.getStartOffset()).getColumn();
      if (dir == 1) {
        // Right shift blockwise selection
        final String indent = indentConfig.createIndentByCount(count);

        for (int l = sline; l <= eline; l++) {
          int len = EngineEditorHelperKt.lineLength(editor, l);
          if (len > from) {
            BufferPosition spos = new BufferPosition(l, from, false);
            insertText(editor, caret, spos, indent);
          }
        }
      }
      else {
        // Left shift blockwise selection
        CharSequence chars = editor.text();
        for (int l = sline; l <= eline; l++) {
          int len = EngineEditorHelperKt.lineLength(editor, l);
          if (len > from) {
            BufferPosition spos = new BufferPosition(l, from, false);
            BufferPosition epos = new BufferPosition(l, from + indentConfig.getTotalIndent(count) - 1, false);
            int wsoff = editor.bufferPositionToOffset(spos);
            int weoff = editor.bufferPositionToOffset(epos);
            int pos;
            for (pos = wsoff; pos <= weoff; pos++) {
              if (CharacterHelper.charType(chars.charAt(pos), false) != CharacterHelper.CharacterType.WHITESPACE) {
                break;
              }
            }
            if (pos > wsoff) {
              deleteText(editor, new TextRange(wsoff, pos), null, caret, operatorArguments, true);
            }
          }
        }
      }
    }
    else {
      // Shift non-blockwise selection
      for (int l = sline; l <= eline; l++) {
        final int soff = editor.getLineStartOffset(l);
        final int eoff = EngineEditorHelperKt.getLineEndOffset(editor, l, true);
        final int woff = injector.getMotion().moveCaretToLineStartSkipLeading(editor, l);
        final int col = editor.offsetToVisualPosition(woff).getColumn();
        final int limit = Math.max(0, col + dir * indentConfig.getTotalIndent(count));
        if (col > 0 || soff != eoff) {
          final String indent = indentConfig.createIndentBySize(limit);
          replaceText(editor, caret, soff, woff, indent);
        }
      }
    }

    if (!CommandStateHelper.inInsertMode(((IjVimEditor) editor).getEditor())) {
      if (!range.isMultiple()) {
        // The caret has moved, so reset the intended column before trying to get the expected offset
        VimCaret newCaret = caret.setVimLastColumnAndGetCaret(intendedColumn);
        final int offset = injector.getMotion().moveCaretToLineWithStartOfLineOption(editor, sline, caret);
        newCaret.moveToOffset(offset);
      }
      else {
        caret.moveToOffset(range.getStartOffset());
      }
    }
  }


  /**
   * Sort range of text with a given comparator
   *
   * @param editor         The editor to replace text in
   * @param range          The range to sort
   * @param lineComparator The comparator to use to sort
   * @return true if able to sort the text, false if not
   */
  public boolean sortRange(@NotNull VimEditor editor, @NotNull VimCaret caret, @NotNull LineRange range, @NotNull Comparator<String> lineComparator) {
    final int startLine = range.startLine;
    final int endLine = range.endLine;
    final int count = endLine - startLine + 1;
    if (count < 2) {
      return false;
    }

    final int startOffset = editor.getLineStartOffset(startLine);
    final int endOffset = editor.getLineEndOffset(endLine);

    return sortTextRange(editor, caret, startOffset, endOffset, lineComparator);
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
                                @NotNull VimCaret caret,
                                int start,
                                int end,
                                @NotNull Comparator<String> lineComparator) {
    final String selectedText = ((IjVimEditor) editor).getEditor().getDocument().getText(new TextRangeInterval(start, end));
    final List<String> lines = Lists.newArrayList(Splitter.on("\n").split(selectedText));
    if (lines.size() < 1) {
      return false;
    }
    lines.sort(lineComparator);
    replaceText(editor, caret, start, end, StringUtil.join(lines, "\n"));
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
            .createHtmlTextBalloonBuilder("Wow, nice vim skills!", VimIcons.IDEAVIM,
                                          MessageType.INFO.getTitleForeground(), MessageType.INFO.getPopupBackground(),
                                          null).createBalloon();
          balloon.show(JBPopupFactory.getInstance().guessBestPopupLocation(((IjVimEditor)editor).getEditor()),
                       Balloon.Position.below);
        });
      }
    }

    List<String> nf = options(injector, editor).getNrformats();
    boolean alpha = nf.contains("alpha");
    boolean hex = nf.contains("hex");
    boolean octal = nf.contains("octal");

    @NotNull List<Pair<TextRange, NumberType>> numberRanges =
      SearchHelper.findNumbersInRange(((IjVimEditor) editor).getEditor(), selectedRange, alpha, hex, octal);

    List<String> newNumbers = new ArrayList<>();
    for (int i = 0; i < numberRanges.size(); i++) {
      Pair<TextRange, NumberType> numberRange = numberRanges.get(i);
      int iCount = avalanche ? (i + 1) * count : count;
      String newNumber = changeNumberInRange(editor, numberRange, iCount, alpha, hex, octal);
      newNumbers.add(newNumber);
    }

    for (int i = newNumbers.size() - 1; i >= 0; i--) {
      // Replace text bottom up. In other direction ranges will be desynchronized after inc numbers like 99
      Pair<TextRange, NumberType> rangeToReplace = numberRanges.get(i);
      String newNumber = newNumbers.get(i);
      replaceText(editor, caret, rangeToReplace.getFirst().getStartOffset(), rangeToReplace.getFirst().getEndOffset(), newNumber);
    }

    InlayHelperKt.moveToInlayAwareOffset(((IjVimCaret) caret).getCaret(), selectedRange.getStartOffset());
    return true;
  }


  @Override
  public boolean changeNumber(final @NotNull VimEditor editor, @NotNull VimCaret caret, final int count) {
    final List<String> nf = options(injector, editor).getNrformats();
    final boolean alpha = nf.contains("alpha");
    final boolean hex = nf.contains("hex");
    final boolean octal = nf.contains("octal");

    @Nullable Pair<TextRange, NumberType> range =
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
      replaceText(editor, caret, range.getFirst().getStartOffset(), range.getFirst().getEndOffset(), newNumber);
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

  private @Nullable String changeNumberInRange(final @NotNull VimEditor editor,
                                              Pair<TextRange, NumberType> range,
                                              final int count,
                                              boolean alpha,
                                              boolean hex,
                                              boolean octal) {
    String text = EngineEditorHelperKt.getText(editor, range.getFirst());
    NumberType numberType = range.getSecond();
    if (logger.isDebugEnabled()) {
      logger.debug("found range " + range);
      logger.debug("text=" + text);
    }
    String number = text;
    if (text.length() == 0) {
      return null;
    }

    char ch = text.charAt(0);
    if (hex && NumberType.HEX.equals(numberType)) {
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
    else if (octal && NumberType.OCT.equals(numberType) && text.length() > 1) {
      if (!text.startsWith("0")) throw new RuntimeException("Oct number should start with 0: " + text);
      BigInteger num = new BigInteger(text, 8).add(BigInteger.valueOf(count));

      if (num.compareTo(BigInteger.ZERO) < 0) {
        num = new BigInteger("1777777777777777777777", 8).add(BigInteger.ONE).add(num);
      }
      number = num.toString(8);
      number = "0" + StringsKt.padStart(number, text.length() - 1, '0');
    }
    else if (alpha && NumberType.ALPHA.equals(numberType)) {
      if (!Character.isLetter(ch)) throw new RuntimeException("Not alpha number : " + text);
      ch += count;
      if (Character.isLetter(ch)) {
        number = String.valueOf(ch);
      }
    }
    else if (NumberType.DEC.equals(numberType)) {
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
