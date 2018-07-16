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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.StringTokenizer;

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
    if (motion == null) {
      return false;
    }

    final ArrayList<Integer> starts = new ArrayList<>();
    final ArrayList<Integer> ends = new ArrayList<>();
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final TextRange range = MotionGroup.getMotionRange(editor, caret, context, count, rawCount, argument, true);
      if (range == null) {
        continue;
      }
      starts.add(range.getStartOffset());
      ends.add(range.getEndOffset());
    }

    final int[] ss = new int[starts.size()];
    final int[] es = new int[ends.size()];
    for (int i = 0; i < starts.size(); i++) {
      ss[i] = starts.get(i);
      es[i] = ends.get(i);
    }

    final SelectionType type = SelectionType.fromCommandFlags(motion.getFlags());
    if (type == SelectionType.LINE_WISE) {
      for (int i = 0; i < es.length - 1; i++) {
        --es[i];
      }
      return yankRange(editor, new TextRange(ss, es), type, true);
    }
    return yankRange(editor, new TextRange(ss, es), SelectionType.BLOCK_WISE, true);
  }

  /**
   * This yanks count lines of text
   *
   * @param editor The editor to yank from
   * @param count  The number of lines to yank
   * @return true if able to yank the lines, false if not
   */
  public boolean yankLine(@NotNull Editor editor, int count) {
    final ArrayList<Integer> starts = new ArrayList<>();
    final ArrayList<Integer> ends = new ArrayList<>();

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final int start = VimPlugin.getMotion().moveCaretToLineStart(editor, caret);
      final int end = Math.min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1,
                               EditorHelper.getFileSize(editor));
      if (end != -1) {
        starts.add(start);
        ends.add(end);
      }
    }

    final int[] ss = new int[starts.size()];
    final int[] es = new int[ends.size()];
    for (int i = 0; i < starts.size(); i++) {
      ss[i] = starts.get(i);
      es[i] = ends.get(i);
      if (i != starts.size() - 1) {
        --es[i];
      }
    }

    return yankRange(editor, new TextRange(ss, es), SelectionType.LINE_WISE, false);
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
    // TODO: Add multiple carets support
    if (range != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("yanking range: " + range);
      }
      boolean res = VimPlugin.getRegister().storeText(editor, range, type, false);
      if (moveCursor) {
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), range.normalize().getStartOffset());
      }

      return res;
    }

    return false;
  }

  public boolean putTextBeforeCursor(@NotNull Editor editor, @NotNull Caret caret, int count, boolean indent,
                                     boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null)
      return false;
    final SelectionType selectionType = register.getType();
    if (selectionType == SelectionType.LINE_WISE && editor.isOneLineMode())
      return false;

    final int startOffset = selectionType == SelectionType.LINE_WISE ?
        VimPlugin.getMotion().moveCaretToLineStart(editor, caret) :
        caret.getOffset();

    final String text = indent ?
        StringUtil.notNullize(register.getText()) :
        removeLeadingSpaces(StringUtil.notNullize(register.getText()));
    final int endOffset = selectionType == SelectionType.BLOCK_WISE ?
        putTextBlockwise(editor, caret, text, count, startOffset) :
        putText(editor, caret, text, count, startOffset);

    moveCursorToOffset(editor, caret, selectionType, startOffset, endOffset, cursorAfter);

    return true;
  }

  public boolean putTextAfterCursor(@NotNull Editor editor, @NotNull Caret caret, int count, boolean indent,
                                    boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null)
      return false;
    final SelectionType selectionType = register.getType();
    if (selectionType == SelectionType.LINE_WISE && editor.isOneLineMode())
      return false;

    int startOffset;
    if (selectionType == SelectionType.LINE_WISE) {
      startOffset = Math.min(editor.getDocument().getTextLength(), VimPlugin.getMotion().moveCaretToLineEnd(editor, caret) + 1);
      if (startOffset > 0 &&
          startOffset == editor.getDocument().getTextLength() &&
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
    // In case when text is empty this can occur
    if (startOffset > 0 && startOffset > editor.getDocument().getTextLength()) {
      startOffset--;
    }

    final String text = indent ?
        StringUtil.notNullize(register.getText()) :
        removeLeadingSpaces(StringUtil.notNullize(register.getText()));
    final int endOffset = selectionType == SelectionType.BLOCK_WISE ?
        putTextBlockwise(editor, caret, text, count, startOffset) :
        putText(editor, caret, text, count, startOffset);

    moveCursorToOffset(editor, caret, selectionType, startOffset, endOffset, cursorAfter);

    return true;
  }

  public boolean putVisualRange(@NotNull Editor editor, @NotNull Caret caret, @NotNull TextRange range, int count,
                                boolean indent, boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null) {
      return false;
    }
    final SelectionType selectionType = register.getType();
    if (selectionType == SelectionType.LINE_WISE && editor.isOneLineMode()) {
      return false;
    }

    final CommandState.SubMode subMode = CommandState.getInstance(editor).getSubMode();
    if (subMode == CommandState.SubMode.VISUAL_LINE) {
      range = new TextRange(range.getStartOffset(), Math.min(range.getEndOffset() + 1, EditorHelper.getFileSize(editor)));
    }

    VimPlugin.getChange().deleteRange(editor, caret, range, SelectionType.fromSubMode(subMode), false);

    int startOffset = range.getStartOffset();
    if (selectionType == SelectionType.LINE_WISE) {
      if (subMode == CommandState.SubMode.VISUAL_BLOCK) {
        startOffset = editor.getDocument().getLineEndOffset(range.getEndOffset()) + 1;
      }
      else if (subMode != CommandState.SubMode.VISUAL_LINE) {
        editor.getDocument().insertString(startOffset, "\n");
        ++startOffset;
      }
    }
    else if (selectionType != SelectionType.CHARACTER_WISE) {
      if (subMode == CommandState.SubMode.VISUAL_LINE) {
        editor.getDocument().insertString(startOffset, "\n");
      }
    }

    final String text = indent ?
        StringUtil.notNullize(register.getText()) :
        removeLeadingSpaces(StringUtil.notNullize(register.getText()));
    final int endOffset = selectionType == SelectionType.BLOCK_WISE ?
        putTextBlockwise(editor, caret, text, count, startOffset) :
        putText(editor, caret, text, count, startOffset);

    //TODO: store range for all carets?
    VimPlugin.getRegister().storeText(editor, new TextRange(startOffset, startOffset + (endOffset - startOffset) / count),
        selectionType, false);

    moveCursorToOffset(editor, caret, selectionType, startOffset, endOffset, cursorAfter);

    return true;
  }

  @NotNull
  private String removeLeadingSpaces(@NotNull String s) {
    final StringBuilder ret = new StringBuilder(s.length());
    int idx = 0;
    while (idx < s.length() && Character.isWhitespace(s.charAt(idx))) {
      if (s.charAt(idx) != ' ') {
        ret.append(s.charAt(idx));
      }
      ++idx;
    }
    return ret.append(s.substring(idx)).toString();
  }


  private int putText(@NotNull Editor editor, @NotNull Caret caret, @NotNull String text, int count, int startOffset) {
    MotionGroup.moveCaret(editor, caret, startOffset);

    final int textLength = text.length() * count;
    StringBuilder sb = new StringBuilder(textLength);
    for (int i = 0; i < count; i++) {
      sb.append(text);
    }

    VimPlugin.getChange().insertText(editor, caret, sb.toString());
    final int endOffset = startOffset + textLength;
    VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, endOffset));

    return endOffset;
  }

  private int putTextBlockwise(@NotNull Editor editor, @NotNull Caret caret, @NotNull String text, int count,
                               int startOffset) {
    final LogicalPosition start = editor.offsetToLogicalPosition(startOffset);
    final int col = start.column;
    int line = start.line;

    int lines = 1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        lines++;
      }
    }

    if (line + lines >= EditorHelper.getLineCount(editor)) {
      for (int i = 0; i < line + lines - EditorHelper.getLineCount(editor); i++) {
        VimPlugin.getChange().insertText(editor, EditorHelper.getFileSize(editor, true), "\n");
      }
    }

    StringTokenizer tokenizer = new StringTokenizer(text, "\n");
    int maxlen = 0;
    while (tokenizer.hasMoreTokens()) {
      final String segment = tokenizer.nextToken();
      maxlen = Math.max(maxlen, segment.length());
    }

    tokenizer = new StringTokenizer(text, "\n");
    int endOffset = 0;
    while (tokenizer.hasMoreTokens()) {
      String segment = tokenizer.nextToken();
      String origSegment = segment;

      if (segment.length() < maxlen) {
        StringBuilder sb = new StringBuilder(segment);
        for (int i = segment.length(); i < maxlen; i++) {
          sb.append(' ');
        }
        segment = sb.toString();

        if (col != 0 && col < EditorHelper.getLineLength(editor, line)) {
          origSegment = segment;
        }
      }

      final String pad = EditorHelper.pad(editor, line, col);

      final int insertingOffset = editor.logicalPositionToOffset(new LogicalPosition(line, col));
      endOffset = insertingOffset;
      MotionGroup.moveCaret(editor, caret, insertingOffset);
      VimPlugin.getChange().insertText(editor, caret, origSegment);
      endOffset += origSegment.length();
      for (int i = 1; i < count; i++) {
        MotionGroup.moveCaret(editor, caret, insertingOffset);
        VimPlugin.getChange().insertText(editor, caret, segment);
        endOffset += segment.length();
      }

      if (pad.length() > 0) {
        VimPlugin.getChange().insertText(editor, insertingOffset, pad);
        endOffset += pad.length();
      }

      line++;
    }

    return endOffset;
  }

  private void moveCursorToOffset(@NotNull Editor editor, @NotNull Caret caret, @NotNull SelectionType selectionType,
                                  int startOffset, int endOffset, boolean cursorAfter) {
    switch (selectionType) {
      case LINE_WISE:
        if (cursorAfter) {
          MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStart(editor, caret));
        }
        else {
          MotionGroup.moveCaret(editor, caret, startOffset);
          MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret));
        }
        break;
      case CHARACTER_WISE:
        if (cursorAfter) {
          MotionGroup.moveCaret(editor, caret, endOffset);
        }
        else {
          MotionGroup.moveCaret(editor, caret, endOffset - 1);
        }
        break;
      case BLOCK_WISE:
        if (cursorAfter) {
          MotionGroup.moveCaret(editor, caret, endOffset);
        }
        else {
          MotionGroup.moveCaret(editor, caret, startOffset);
        }
        break;
    }
  }

  /**
   * This performs the actual insert of the paste
   *
   * @param editor      The editor to paste into
   * @param context     The data context
   * @param offset      The location within the file to paste the text
   * @param text        The text to paste
   * @param type        The type of paste
   * @param count       The number of times to paste the text
   * @param indent      True if pasted lines should be autoindented, false if not
   * @param cursorAfter If true move cursor to just after pasted text
   * @param mode        The type of hightlight prior to the put.
   */
  public void putText(@NotNull Editor editor, @NotNull DataContext context, int offset, @NotNull String text,
                      @NotNull SelectionType type, int count, boolean indent, boolean cursorAfter,
                      @NotNull CommandState.SubMode mode) {
    if (logger.isDebugEnabled()) {
      logger.debug("offset=" + offset);
      logger.debug("type=" + type);
      logger.debug("mode=" + mode);
    }

    if (mode == CommandState.SubMode.VISUAL_LINE && editor.isOneLineMode()) {
      return;
    }

    // Don't indent if this there isn't anything about a linewise selection or register
    if (indent && type != SelectionType.LINE_WISE && mode != CommandState.SubMode.VISUAL_LINE) {
      indent = false;
    }

    if (type == SelectionType.LINE_WISE && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
      text = text + '\n';
    }

    int insertCnt = 0;
    int endOffset = offset;
    if (type != SelectionType.BLOCK_WISE) {
      for (int i = 0; i < count; i++) {
        VimPlugin.getChange().insertText(editor, offset, text);
        insertCnt += text.length();
        endOffset += text.length();
      }
    }
    else {
      LogicalPosition start = editor.offsetToLogicalPosition(offset);
      int col = mode == CommandState.SubMode.VISUAL_LINE ? 0 : start.column;
      int line = start.line;
      if (logger.isDebugEnabled()) {
        logger.debug("col=" + col + ", line=" + line);
      }
      int lines = 1;
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) == '\n') {
          lines++;
        }
      }

      if (line + lines >= EditorHelper.getLineCount(editor)) {
        for (int i = 0; i < line + lines - EditorHelper.getLineCount(editor); i++) {
          VimPlugin.getChange().insertText(editor, EditorHelper.getFileSize(editor, true), "\n");
          insertCnt++;
        }
      }

      StringTokenizer parser = new StringTokenizer(text, "\n");
      int maxlen = 0;
      while (parser.hasMoreTokens()) {
        String segment = parser.nextToken();
        maxlen = Math.max(maxlen, segment.length());
      }

      parser = new StringTokenizer(text, "\n");
      while (parser.hasMoreTokens()) {
        String segment = parser.nextToken();
        String origSegment = segment;
        if (segment.length() < maxlen) {
          logger.debug("short line");
          StringBuilder extra = new StringBuilder(maxlen - segment.length());
          for (int i = segment.length(); i < maxlen; i++) {
            extra.append(' ');
          }
          segment = segment + extra.toString();
          if (col != 0 && col < EditorHelper.getLineLength(editor, line)) {
            origSegment = segment;
          }
        }
        String pad = EditorHelper.pad(editor, line, col);

        int insoff = editor.logicalPositionToOffset(new LogicalPosition(line, col));
        endOffset = insoff;
        if (logger.isDebugEnabled()) {
          logger.debug("segment='" + segment + "'");
          logger.debug("origSegment='" + origSegment + "'");
          logger.debug("insoff=" + insoff);
        }
        for (int i = 0; i < count; i++) {
          String txt = i == 0 ? origSegment : segment;
          VimPlugin.getChange().insertText(editor, insoff, txt);
          insertCnt += txt.length();
          endOffset += txt.length();
        }

        if (mode == CommandState.SubMode.VISUAL_LINE) {
          VimPlugin.getChange().insertText(editor, endOffset, "\n");
          insertCnt++;
          endOffset++;
        }
        else {
          if (pad.length() > 0) {
            VimPlugin.getChange().insertText(editor, insoff, pad);
            insertCnt += pad.length();
            endOffset += pad.length();
          }
        }

        line++;
      }
    }

    LogicalPosition slp = editor.offsetToLogicalPosition(offset);
    /*
    int adjust = 0;
    if ((type & Command.FLAG_MOT_LINEWISE) != 0)
    {
        adjust = -1;
    }
    */
    LogicalPosition elp = editor.offsetToLogicalPosition(endOffset - 1);
    if (logger.isDebugEnabled()) {
      logger.debug("slp.line=" + slp.line);
      logger.debug("elp.line=" + elp.line);
    }
    if (indent) {
      int startOff = editor.getDocument().getLineStartOffset(slp.line);
      int endOff = editor.getDocument().getLineEndOffset(elp.line);
      VimPlugin.getChange().autoIndentRange(editor, editor.getCaretModel().getPrimaryCaret(), context, new TextRange(startOff, endOff));
    }
    /*
    boolean indented = false;
    for (int i = slp.line; indent && i <= elp.line; i++)
    {
        MotionGroup.moveCaret(editor, context, editor.logicalPositionToOffset(new LogicalPosition(i, 0)));
        KeyHandler.executeAction("OrigAutoIndentLines", context);
        indented = true;
    }
    */
    if (logger.isDebugEnabled()) {
      logger.debug("insertCnt=" + insertCnt);
    }
    if (indent) {
      endOffset = EditorHelper.getLineEndOffset(editor, elp.line, true);
      insertCnt = endOffset - offset;
      if (logger.isDebugEnabled()) {
        logger.debug("insertCnt=" + insertCnt);
      }
    }

    int cursorMode;
    if (type == SelectionType.BLOCK_WISE) {
      if (mode == CommandState.SubMode.VISUAL_LINE) {
        cursorMode = cursorAfter ? 4 : 1;
      }
      else {
        cursorMode = cursorAfter ? 5 : 1;
      }
    }
    else if (type == SelectionType.LINE_WISE) {
      if (mode == CommandState.SubMode.VISUAL_LINE) {
        cursorMode = cursorAfter ? 4 : 3;
      }
      else {
        cursorMode = cursorAfter ? 4 : 3;
      }
    }
    else /* Characterwise */ {
      if (mode == CommandState.SubMode.VISUAL_LINE) {
        cursorMode = cursorAfter ? 4 : 1;
      }
      else {
        cursorMode = cursorAfter ? 5 : 2;
      }
    }

    switch (cursorMode) {
      case 1:
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
        break;
      case 2:
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), endOffset - 1);
        break;
      case 3:
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), offset);
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), VimPlugin.getMotion()
            .moveCaretToLineStartSkipLeading(editor, editor.getCaretModel().getPrimaryCaret()));
        break;
      case 4:
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), endOffset + 1);
        break;
      case 5:
        int pos = Math.min(endOffset, EditorHelper.getLineEndForOffset(editor, endOffset - 1) - 1);
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), pos);
        break;
    }

    VimPlugin.getMark().setChangeMarks(editor, new TextRange(offset, endOffset));
  }

  private static final Logger logger = Logger.getInstance(CopyGroup.class.getName());
}
