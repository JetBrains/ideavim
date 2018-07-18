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
import com.maddyhome.idea.vim.handler.CaretOrder;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    TextRange range = MotionGroup
      .getMotionRange(editor, editor.getCaretModel().getPrimaryCaret(), context, count, rawCount, argument, true);
    final Command motion = argument.getMotion();
    return motion != null && yankRange(editor, range, SelectionType.fromCommandFlags(motion.getFlags()), true);
  }

  /**
   * This yanks count lines of text
   *
   * @param editor The editor to yank from
   * @param count  The number of lines to yank
   * @return true if able to yank the lines, false if not
   */
  public boolean yankLine(@NotNull Editor editor, int count) {
    final Caret caret = editor.getCaretModel().getPrimaryCaret();
    int start = VimPlugin.getMotion().moveCaretToLineStart(editor, caret);
    int offset = Math.min(VimPlugin.getMotion().moveCaretToLineEndOffset(editor, caret, count - 1, true) + 1,
                          EditorHelper.getFileSize(editor));
    return offset != -1 && yankRange(editor, new TextRange(start, offset), SelectionType.LINE_WISE, false);
  }

  /**
   * This yanks a range of text
   *
   * @param editor The editor to yank from
   * @param range  The range of text to yank
   * @param type   The type of yank
   * @return true if able to yank the range, false if not
   */
  public boolean yankRange(@NotNull Editor editor, @Nullable TextRange range, @NotNull SelectionType type, boolean moveCursor) {
    if (range != null) {

      boolean res = VimPlugin.getRegister().storeText(editor, range, type, false);
      if (moveCursor) {
        MotionGroup.moveCaret(editor, editor.getCaretModel().getPrimaryCaret(), range.normalize().getStartOffset());
      }

      return res;
    }

    return false;
  }

  /**
   * Pastes text from the last register into the editor before the current cursor location.
   *
   * @param editor  The editor to paste into
   * @param context The data context
   * @param count   The number of times to perform the paste
   * @return true if able to paste, false if not
   */
  public boolean putTextBeforeCursor(@NotNull Editor editor,
                                     @NotNull DataContext context,
                                     int count,
                                     boolean indent,
                                     boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null) return false;
    final SelectionType type = register.getType();
    if (type == SelectionType.LINE_WISE && editor.isOneLineMode()) return false;
    final String text = register.getText();

    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      final int startOffset = getStartOffset(editor, caret, type, true);

      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, startOffset));
        continue;
      }

      putText(editor, caret, context, text, type, CommandState.SubMode.NONE, startOffset, count, indent, cursorAfter);
    }

    return true;
  }

  /**
   * Pastes text from the last register into the editor after the current cursor location.
   *
   * @param editor      The editor to paste into
   * @param context     The data context
   * @param count       The number of times to perform the paste
   * @return true if able to paste, false if not
   */
  public boolean putTextAfterCursor(@NotNull Editor editor, @NotNull DataContext context, int count, boolean indent,
                                    boolean cursorAfter) {
    final Register register = VimPlugin.getRegister().getLastRegister();
    if (register == null) return false;
    final SelectionType type = register.getType();
    if (type == SelectionType.LINE_WISE && editor.isOneLineMode()) return false;

    final String text = register.getText();
    for (Caret caret : EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET)) {
      final int startOffset = getStartOffset(editor, caret, type, false);

      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset);
        VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, startOffset));
        continue;
      }

      putText(editor, caret, context, text, type, CommandState.SubMode.NONE, startOffset, count, indent, cursorAfter);
    }
    return true;
  }

  private int getStartOffset(@NotNull Editor editor, @NotNull Caret caret, SelectionType type, boolean beforeCursor) {
    if (beforeCursor) {
      return type == SelectionType.LINE_WISE ? VimPlugin.getMotion().moveCaretToLineStart(editor, caret) : caret.getOffset();
    }

    int startOffset;
    if (type == SelectionType.LINE_WISE) {
      startOffset =
        Math.min(editor.getDocument().getTextLength(), VimPlugin.getMotion().moveCaretToLineEnd(editor, caret) + 1);
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
    if (startOffset > 0 && startOffset > editor.getDocument().getTextLength()) {
      startOffset--;
    }
    return startOffset;
  }

  public boolean putVisualRange(@NotNull Editor editor, @NotNull DataContext context, @NotNull TextRange range, int count, boolean indent,
                                boolean cursorAfter) {
    //TODO: add multiple carets support
    CommandState.SubMode subMode = CommandState.getInstance(editor).getSubMode();
    Register reg = VimPlugin.getRegister().getLastRegister();
    // Without this reset, the deleted text goes into the same register we just pasted from.
    VimPlugin.getRegister().resetRegister();
    if (reg != null) {
      final SelectionType type = reg.getType();
      if (type == SelectionType.LINE_WISE && editor.isOneLineMode()) {
        return false;
      }

      int start = range.getStartOffset();
      int end = range.getEndOffset();
      int endLine = editor.offsetToLogicalPosition(end).line;
      if (logger.isDebugEnabled()) {
        logger.debug("start=" + start);
        logger.debug("end=" + end);
      }

      if (subMode == CommandState.SubMode.VISUAL_LINE) {
        range = new TextRange(range.getStartOffset(),
                              Math.min(range.getEndOffset() + 1, EditorHelper.getFileSize(editor)));
      }

      VimPlugin.getChange().deleteRange(editor, editor.getCaretModel().getPrimaryCaret(), range, SelectionType.fromSubMode(subMode), false);

      editor.getCaretModel().moveToOffset(start);

      int pos = start;
      if (type == SelectionType.LINE_WISE) {
        if (subMode == CommandState.SubMode.VISUAL_BLOCK) {
          pos = editor.getDocument().getLineEndOffset(endLine) + 1;
        }
        else if (subMode != CommandState.SubMode.VISUAL_LINE) {
          editor.getDocument().insertString(start, "\n");
          pos = start + 1;
        }
      }
      else if (type != SelectionType.CHARACTER_WISE) {
        if (subMode == CommandState.SubMode.VISUAL_LINE) {
          editor.getDocument().insertString(start, "\n");
        }
      }

      putText(editor, editor.getCaretModel().getPrimaryCaret(), context, StringUtil.notNullize(reg.getText()), type,
              subMode, pos, count, indent && type == SelectionType.LINE_WISE, cursorAfter);

      return true;
    }

    return false;
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
  public void putText(@NotNull Editor editor,
                      @NotNull Caret caret,
                      @NotNull DataContext context,
                      @NotNull String text,
                      @NotNull SelectionType type,
                      @NotNull CommandState.SubMode mode,
                      int startOffset,
                      int count,
                      boolean indent,
                      boolean cursorAfter) {
    if (mode == CommandState.SubMode.VISUAL_LINE && editor.isOneLineMode()) return;
    if (indent && type != SelectionType.LINE_WISE && mode != CommandState.SubMode.VISUAL_LINE) indent = false;
    if (type == SelectionType.LINE_WISE && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
      text = text + '\n';
    }

    final int endOffset = puTextInternal(editor, caret, context, text, type, mode, startOffset, count, indent);
    moveCaret(editor, caret, type, mode, startOffset, cursorAfter, endOffset);
    VimPlugin.getMark().setChangeMarks(editor, new TextRange(startOffset, endOffset));
  }

  private int puTextInternal(@NotNull Editor editor,
                             @NotNull Caret caret,
                             @NotNull DataContext context,
                             @NotNull String text,
                             @NotNull SelectionType type,
                             @NotNull CommandState.SubMode mode,
                             int startOffset,
                             int count,
                             boolean indent) {
    final int endOffset = type != SelectionType.BLOCK_WISE
                          ? putTextInternal(editor, caret, text, startOffset, count)
                          : putTextInternal(editor, startOffset, text, count, mode, startOffset);

    if (indent) return doIndent(editor, caret, context, startOffset, endOffset);
    return endOffset;
  }

  private int putTextInternal(@NotNull Editor editor,
                              int startOffset,
                              @NotNull String text,
                              int count,
                              @NotNull CommandState.SubMode mode,
                              int endOffset) {
    LogicalPosition start = editor.offsetToLogicalPosition(startOffset);
    int col = mode == CommandState.SubMode.VISUAL_LINE ? 0 : start.column;
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
      for (int i = 0; i < count; i++) {
        String txt = i == 0 ? origSegment : segment;
        VimPlugin.getChange().insertText(editor, insoff, txt);
        endOffset += txt.length();
      }

      if (mode == CommandState.SubMode.VISUAL_LINE) {
        VimPlugin.getChange().insertText(editor, endOffset, "\n");
        endOffset++;
      }
      else {
        if (pad.length() > 0) {
          VimPlugin.getChange().insertText(editor, insoff, pad);
          endOffset += pad.length();
        }
      }

      line++;
    }
    return endOffset;
  }

  private int putTextInternal(@NotNull Editor editor, @NotNull Caret caret, @NotNull String text, int startOffset, int count) {
    MotionGroup.moveCaret(editor, caret, startOffset);
    for (int i = 0; i < count; i++) {
      //TODO: change to stringbuilder?
      VimPlugin.getChange().insertText(editor, caret, text);
    }
    return startOffset + text.length() * count;
  }


  private void moveCaret(@NotNull Editor editor,
                         @NotNull Caret caret,
                         @NotNull SelectionType type,
                         @NotNull CommandState.SubMode mode,
                         int startOffset,
                         boolean cursorAfter,
                         int endOffset) {
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

  private int doIndent(@NotNull Editor editor,
                       @NotNull Caret caret,
                       @NotNull DataContext context,
                       int startOffset,
                       int endOffset) {
    final int startLine = editor.offsetToLogicalPosition(startOffset).line;
    final int endLine = editor.offsetToLogicalPosition(endOffset - 1).line;
    final int startLineOffset = editor.getDocument().getLineStartOffset(startLine);
    final int endLineOffset = editor.getDocument().getLineEndOffset(endLine);

    VimPlugin.getChange().autoIndentRange(editor, caret, context, new TextRange(startLineOffset, endLineOffset));
    return EditorHelper.getLineEndOffset(editor, endLine, true);
  }

  private static final Logger logger = Logger.getInstance(CopyGroup.class.getName());
}
