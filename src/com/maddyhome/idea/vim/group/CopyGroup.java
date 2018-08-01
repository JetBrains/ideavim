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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This group works with command associated with copying and pasting text
 */
public class CopyGroup {
  /**
   * Creates the group
   */
  public CopyGroup() {
  }

  /**
   * This yanks the text moved over by the motion command argument.
   *
   * @param editor   The editor to yank from
   * @param context  The data context
   * @param count    The number of times to yank
   * @param rawCount The actual count entered by the user
   * @param argument The motion command argument
   * @return true if able to yank the text, false if not
   */
  public boolean yankMotion(@NotNull Editor editor, DataContext context, int count, int rawCount,
                            @NotNull Argument argument) {
    final Command motion = argument.getMotion();
    if (motion == null) return false;

    final CaretModel caretModel = editor.getCaretModel();
    final List<Pair.NonNull<Integer, Integer>> ranges = new ArrayList<>(caretModel.getCaretCount());
    final Map<Caret, Integer> startOffsets = new HashMap<>(caretModel.getCaretCount());
    for (Caret caret : caretModel.getAllCarets()) {
      final TextRange motionRange = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, true);
      if (motionRange == null) continue;

      assert motionRange.size() == 1;
      ranges.add(Pair.createNonNull(motionRange.getStartOffset(), motionRange.getEndOffset()));
      startOffsets.put(caret, motionRange.normalize().getStartOffset());
    }

    final SelectionType type = SelectionType.fromCommandFlags(motion.getFlags());
    final TextRange range = getTextRange(ranges, type);

    final SelectionType selectionType =
      type == SelectionType.CHARACTER_WISE && range.isMultiple() ? SelectionType.BLOCK_WISE : type;
    return yankRange(editor, range, selectionType, startOffsets);
  }

  /**
   * This yanks count lines of text
   *
   * @param editor The editor to yank from
   * @param count  The number of lines to yank
   * @return true if able to yank the lines, false if not
   */
  public boolean yankLine(@NotNull Editor editor, int count) {
    final CaretModel caretModel = editor.getCaretModel();
    final List<Pair.NonNull<Integer, Integer>> ranges = new ArrayList<>(caretModel.getCaretCount());
    for (Caret caret : caretModel.getAllCarets()) {
      final int start = VimPlugin.getMotion().moveCaretToLineStart(editor, caret);
      final int end = Math.min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1,
                               EditorHelper.getFileSize(editor));

      if (end == -1) continue;

      ranges.add(Pair.createNonNull(start, end));
    }

    final TextRange range = getTextRange(ranges, SelectionType.LINE_WISE);
    return yankRange(editor, range, SelectionType.LINE_WISE, null);
  }

  /**
   * This yanks a range of text
   *
   * @param editor The editor to yank from
   * @param range  The range of text to yank
   * @param type   The type of yank
   * @return true if able to yank the range, false if not
   */
  public boolean yankRange(@NotNull Editor editor, @Nullable TextRange range, @NotNull SelectionType type,
                           boolean moveCursor) {
    if (range == null) return false;

    final SelectionType selectionType =
      type == SelectionType.CHARACTER_WISE && range.isMultiple() ? SelectionType.BLOCK_WISE : type;

    final CaretModel caretModel = editor.getCaretModel();
    final int[] rangeStartOffsets = range.getStartOffsets();
    final int[] rangeEndOffsets = range.getEndOffsets();
    if (selectionType == SelectionType.LINE_WISE) {
      final List<Pair.NonNull<Integer, Integer>> ranges = new ArrayList<>(caretModel.getCaretCount());
      for (int i = 0; i < caretModel.getCaretCount(); i++) {
        ranges.add(Pair.createNonNull(EditorHelper.getLineStartForOffset(editor, rangeStartOffsets[i]),
                                      EditorHelper.getLineEndForOffset(editor, rangeEndOffsets[i]) + 1));
      }
      range = getTextRange(ranges, selectionType);
    }

    if (moveCursor) {
      final Map<Caret, Integer> startOffsets = new HashMap<>(caretModel.getCaretCount());
      if (type == SelectionType.BLOCK_WISE) {
        startOffsets.put(caretModel.getPrimaryCaret(), range.normalize().getStartOffset());
      }
      else {
        final List<Caret> carets = caretModel.getAllCarets();
        for (int i = 0; i < carets.size(); i++) {
          startOffsets.put(carets.get(i),
                           new TextRange(rangeStartOffsets[i], rangeEndOffsets[i]).normalize().getStartOffset());
        }
      }

      return yankRange(editor, range, selectionType, startOffsets);
    }
    else {
      return yankRange(editor, range, selectionType, null);
    }
  }

  /**
   * Pastes text from the last register into the editor.
   *
   * @param editor  The editor to paste into
   * @param context The data context
   * @param count   The number of times to perform the paste
   * @return true if able to paste, false if not
   */
  public boolean putText(@NotNull Editor editor, @NotNull DataContext context, int count, boolean indent,
                         boolean cursorAfter, boolean beforeCursor) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null) return false;
    final SelectionType selectionType = register.getType();
    if (selectionType == SelectionType.LINE_WISE && editor.isOneLineMode()) return false;

    final String text = register.getText();
    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, beforeCursor ? CaretOrder.INCREASING_OFFSET
                                                                                      : CaretOrder.DECREASING_OFFSET);
    for (Caret caret : carets) {
      final int startOffset = getStartOffset(editor, caret, selectionType, beforeCursor);

      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, startOffset));
        continue;
      }

      putText(editor, caret, context, text, selectionType, CommandState.SubMode.NONE, startOffset, count, indent,
              cursorAfter);
    }

    return true;
  }

  public boolean putVisualRange(@NotNull Editor editor, @NotNull DataContext context, @NotNull TextRange range,
                                int count, boolean indent, boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    VimPlugin.getRegister().resetRegister();
    if (register == null) return false;
    final SelectionType type = register.getType();
    if (type == SelectionType.LINE_WISE && editor.isOneLineMode()) return false;

    final CaretModel caretModel = editor.getCaretModel();

    final ArrayList<Pair.NonNull<Integer, Integer>> ranges = new ArrayList<>(caretModel.getCaretCount());
    final List<Integer> endLines = new ArrayList<>(caretModel.getCaretCount());

    for (int i = 0; i < range.size(); i++) {
      final int start = range.getStartOffsets()[i];
      final int end = range.getEndOffsets()[i];
      ranges.add(Pair.createNonNull(start, end));
      endLines.add(editor.offsetToLogicalPosition(end).line);
    }

    final CommandState.SubMode subMode = CommandState.getInstance(editor).getSubMode();
    if (subMode == CommandState.SubMode.VISUAL_LINE) {
      final int[] starts = new int[caretModel.getCaretCount()];
      final int[] ends = new int[caretModel.getCaretCount()];
      for (int i = 0; i < ranges.size(); i++) {
        final Pair.NonNull<Integer, Integer> subRange = ranges.get(i);
        starts[i] = subRange.first;
        ends[i] = Math.min(subRange.second + 1, EditorHelper.getFileSize(editor));
      }
      range = new TextRange(starts, ends);
    }

    final List<Caret> carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET);
    for (int i = 0; i < carets.size(); i++) {
      final Caret caret = carets.get(i);
      final int index = carets.size() - i - 1;
      VimPlugin.getChange().deleteRange(editor, caret,
                                        new TextRange(range.getStartOffsets()[index], range.getEndOffsets()[index]),
                                        SelectionType.fromSubMode(subMode), false);

      final int start = ranges.get(index).first;
      caret.moveToOffset(start);

      int startOffset = start;
      if (type == SelectionType.LINE_WISE) {
        if (subMode == CommandState.SubMode.VISUAL_BLOCK) {
          startOffset = editor.getDocument().getLineEndOffset(endLines.get(index)) + 1;
        }
        else if (subMode != CommandState.SubMode.VISUAL_LINE) {
          editor.getDocument().insertString(start, "\n");
          startOffset = start + 1;
        }
      }
      else if (type == SelectionType.CHARACTER_WISE) {
        if (subMode == CommandState.SubMode.VISUAL_LINE) {
          editor.getDocument().insertString(start, "\n");
        }
      }

      final String text = register.getText();
      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, startOffset));
        continue;
      }

      putText(editor, caret, context, text, type, subMode, startOffset, count,
              indent && type == SelectionType.LINE_WISE, cursorAfter);
    }

    return true;
  }

  /**
   * This performs the actual insert of the paste
   *
   * @param editor      The editor to paste into
   * @param context     The data context
   * @param startOffset The location within the file to paste the text
   * @param text        The text to paste
   * @param type        The type of paste
   * @param count       The number of times to paste the text
   * @param indent      True if pasted lines should be autoindented, false if not
   * @param cursorAfter If true move cursor to just after pasted text
   * @param mode        The type of highlight prior to the put.
   * @param caret       The caret to insert to
   */
  public void putText(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, @NotNull String text,
                      @NotNull SelectionType type, @NotNull CommandState.SubMode mode, int startOffset, int count,
                      boolean indent, boolean cursorAfter) {
    if (mode == CommandState.SubMode.VISUAL_LINE && editor.isOneLineMode()) return;
    if (indent && type != SelectionType.LINE_WISE && mode != CommandState.SubMode.VISUAL_LINE) indent = false;
    if (type == SelectionType.LINE_WISE && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
      text = text + '\n';
    }

    final int endOffset = putTextInternal(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter);
    VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, endOffset));
  }

  private int putTextInternal(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                              @NotNull String text, @NotNull SelectionType type, @NotNull CommandState.SubMode mode,
                              int startOffset, int count, boolean indent, boolean cursorAfter) {
    switch (type) {
      case CHARACTER_WISE:
        return putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter);
      case LINE_WISE:
        return putTextLinewise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter);
      default:
        return putTextBlockwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter);
    }
  }

  private int putTextLinewise(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                              @NotNull String text, @NotNull SelectionType type, @NotNull CommandState.SubMode mode,
                              int startOffset, int count, boolean indent, boolean cursorAfter) {
    final CaretModel caretModel = editor.getCaretModel();
    final ArrayList<Caret> overlappedCarets = new ArrayList<>(caretModel.getCaretCount());
    for (Caret possiblyOverlappedCaret : caretModel.getAllCarets()) {
      if (possiblyOverlappedCaret.getOffset() != startOffset || possiblyOverlappedCaret == caret) continue;

      MotionGroup.moveCaret(editor, possiblyOverlappedCaret,
                            VimPlugin.getMotion().moveCaretHorizontal(editor, possiblyOverlappedCaret, 1, true));
      overlappedCarets.add(possiblyOverlappedCaret);
    }

    final int endOffset = putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent,
                                               cursorAfter);

    for (Caret overlappedCaret : overlappedCarets) {
      MotionGroup.moveCaret(editor, overlappedCaret,
                            VimPlugin.getMotion().moveCaretHorizontal(editor, overlappedCaret, -1, true));
    }

    return endOffset;
  }

  private int putTextBlockwise(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                               @NotNull String text, @NotNull SelectionType type, @NotNull CommandState.SubMode mode,
                               int startOffset, int count, boolean indent, boolean cursorAfter) {
    final LogicalPosition startPosition = editor.offsetToLogicalPosition(startOffset);
    final int currentColumn = mode == CommandState.SubMode.VISUAL_LINE ? 0 : startPosition.column;
    int currentLine = startPosition.line;

    final int lineCount = StringUtil.getLineBreakCount(text) + 1;
    if (currentLine + lineCount >= EditorHelper.getLineCount(editor)) {
      final int limit = currentLine + lineCount - EditorHelper.getLineCount(editor);
      for (int i = 0; i < limit; i++) {
        MotionGroup.moveCaret(editor, caret, EditorHelper.getFileSize(editor, true));
        VimPlugin.getChange().insertText(editor, caret, "\n");
      }
    }

    final int maxLen = getMaxSegmentLength(text);
    final StringTokenizer tokenizer = new StringTokenizer(text, "\n");
    int endOffset = startOffset;
    while (tokenizer.hasMoreTokens()) {
      String segment = tokenizer.nextToken();
      String origSegment = segment;

      if (segment.length() < maxLen) {
        segment += StringUtil.repeat(" ", maxLen - segment.length());

        if (currentColumn != 0 && currentColumn < EditorHelper.getLineLength(editor, currentLine)) {
          origSegment = segment;
        }
      }

      final String pad = EditorHelper.pad(editor, context, currentLine, currentColumn);

      final int insertOffset = editor.logicalPositionToOffset(new LogicalPosition(currentLine, currentColumn));
      MotionGroup.moveCaret(editor, caret, insertOffset);
      final String insertedText = origSegment + StringUtil.repeat(segment, count - 1);
      VimPlugin.getChange().insertText(editor, caret, insertedText);
      endOffset += insertedText.length();

      if (mode == CommandState.SubMode.VISUAL_LINE) {
        MotionGroup.moveCaret(editor, caret, endOffset);
        VimPlugin.getChange().insertText(editor, caret, "\n");
        ++endOffset;
      }
      else {
        if (pad.length() > 0) {
          MotionGroup.moveCaret(editor, caret, insertOffset);
          VimPlugin.getChange().insertText(editor, caret, pad);
          endOffset += pad.length();
        }
      }

      ++currentLine;
    }

    if (indent) endOffset = doIndent(editor, caret, context, startOffset, endOffset);
    moveCaret(editor, caret, type, mode, startOffset, endOffset, cursorAfter);

    return endOffset;
  }

  private int putTextCharacterwise(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context,
                                   @NotNull String text, @NotNull SelectionType type,
                                   @NotNull CommandState.SubMode mode, int startOffset, int count, boolean indent,
                                   boolean cursorAfter) {
    MotionGroup.moveCaret(editor, caret, startOffset);
    final String insertedText = StringUtil.repeat(text, count);
    VimPlugin.getChange().insertText(editor, caret, insertedText);

    final int endOffset = indent ? doIndent(editor, caret, context, startOffset, startOffset + insertedText.length())
                                 : startOffset + insertedText.length();
    moveCaret(editor, caret, type, mode, startOffset, endOffset, cursorAfter);

    return endOffset;
  }

  private int getStartOffset(@NotNull Editor editor, @NotNull Caret caret, SelectionType type, boolean beforeCursor) {
    if (beforeCursor) {
      return type == SelectionType.LINE_WISE ? VimPlugin.getMotion().moveCaretToLineStart(editor, caret)
                                             : caret.getOffset();
    }

    int startOffset;
    if (type == SelectionType.LINE_WISE) {
      startOffset = Math.min(editor.getDocument().getTextLength(),
                             VimPlugin.getMotion().moveCaretToLineEnd(editor, caret) + 1);
      if (startOffset > 0 && startOffset == editor.getDocument().getTextLength() &&
          editor.getDocument().getCharsSequence().charAt(startOffset - 1) != '\n') {
        editor.getDocument().insertString(startOffset, "\n");
        startOffset++;
      }
    }
    else {
      startOffset = caret.getOffset();
      if (!EditorHelper.isLineEmpty(editor, caret.getLogicalPosition().line, false)) {
        startOffset++;
      }
    }

    if (startOffset > 0 && startOffset > editor.getDocument().getTextLength()) return startOffset - 1;

    return startOffset;
  }

  private void moveCaret(@NotNull Editor editor, @NotNull Caret caret, @NotNull SelectionType type,
                         @NotNull CommandState.SubMode mode, int startOffset, int endOffset, boolean cursorAfter) {
    int cursorMode;
    switch (type) {
      case BLOCK_WISE:
        if (mode == CommandState.SubMode.VISUAL_LINE) {
          cursorMode = cursorAfter ? 4 : 1;
        }
        else {
          cursorMode = cursorAfter ? 5 : 1;
        }
        break;
      case LINE_WISE:
        cursorMode = cursorAfter ? 4 : 3;
        break;
      default:
        if (mode == CommandState.SubMode.VISUAL_LINE) {
          cursorMode = cursorAfter ? 4 : 1;
        }
        else {
          cursorMode = cursorAfter ? 5 : 2;
        }
        break;
    }

    switch (cursorMode) {
      case 1:
        MotionGroup.moveCaret(editor, caret, startOffset);
        break;
      case 2:
        MotionGroup.moveCaret(editor, caret, endOffset - 1);
        break;
      case 3:
        MotionGroup.moveCaret(editor, caret, startOffset);
        MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));
        break;
      case 4:
        MotionGroup.moveCaret(editor, caret, endOffset + 1);
        break;
      case 5:
        int pos = Math.min(endOffset, EditorHelper.getLineEndForOffset(editor, endOffset - 1) - 1);
        MotionGroup.moveCaret(editor, caret, pos);
        break;
    }
  }

  private int doIndent(@NotNull Editor editor, @NotNull Caret caret, @NotNull DataContext context, int startOffset,
                       int endOffset) {
    final int startLine = editor.offsetToLogicalPosition(startOffset).line;
    final int endLine = editor.offsetToLogicalPosition(endOffset - 1).line;
    final int startLineOffset = editor.getDocument().getLineStartOffset(startLine);
    final int endLineOffset = editor.getDocument().getLineEndOffset(endLine);

    VimPlugin.getChange().autoIndentRange(editor, caret, context, new TextRange(startLineOffset, endLineOffset));
    return EditorHelper.getLineEndOffset(editor, endLine, true);
  }

  private int getMaxSegmentLength(@NotNull String text) {
    final StringTokenizer tokenizer = new StringTokenizer(text, "\n");
    int maxLen = 0;
    while (tokenizer.hasMoreTokens()) {
      final String s = tokenizer.nextToken();
      maxLen = Math.max(s.length(), maxLen);
    }
    return maxLen;
  }

  @Contract("_, _ -> new")
  @NotNull
  private TextRange getTextRange(@NotNull List<Pair.NonNull<Integer, Integer>> ranges, @NotNull SelectionType type) {
    final int size = ranges.size();
    final int[] starts = new int[size];
    final int[] ends = new int[size];

    if (type == SelectionType.LINE_WISE) {
      starts[size - 1] = ranges.get(size - 1).first;
      ends[size - 1] = ranges.get(size - 1).second;
      for (int i = 0; i < size - 1; i++) {
        final Pair.NonNull<Integer, Integer> range = ranges.get(i);
        starts[i] = range.first;
        ends[i] = range.second - 1;
      }
    }
    else {
      for (int i = 0; i < size; i++) {
        final Pair.NonNull<Integer, Integer> range = ranges.get(i);
        starts[i] = range.first;
        ends[i] = range.second;
      }
    }

    return new TextRange(starts, ends);
  }

  private boolean yankRange(@NotNull Editor editor, @NotNull TextRange range, @NotNull SelectionType type,
                            @Nullable Map<Caret, Integer> startOffsets) {
    if (startOffsets != null) startOffsets.forEach((caret, offset) -> MotionGroup.moveCaret(editor, caret, offset));

    return VimPlugin.getRegister().storeText(editor, range, type, false);
  }
}
