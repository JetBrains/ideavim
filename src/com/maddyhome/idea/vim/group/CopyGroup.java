/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringTokenizer;

/**
 * This group works with command associated with copying and pasting text
 */
public class CopyGroup extends AbstractActionGroup {
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
  public boolean yankMotion(@NotNull Editor editor, DataContext context, int count, int rawCount, @NotNull Argument argument) {
    TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, true, false);
    return yankRange(editor, range, SelectionType.fromCommandFlags(argument.getMotion().getFlags()), true);
  }

  /**
   * This yanks count lines of text
   *
   * @param editor  The editor to yank from
   * @param count   The number of lines to yank
   * @return true if able to yank the lines, false if not
   */
  public boolean yankLine(@NotNull Editor editor, int count) {
    int start = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
    int offset = Math.min(CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(
      editor, count - 1, true) + 1, EditorHelper.getFileSize(editor));
    if (offset != -1) {
      return yankRange(editor, new TextRange(start, offset), SelectionType.LINE_WISE, false);
    }

    return false;
  }

  /**
   * This yanks a range of text
   *
   * @param editor     The editor to yank from
   * @param range      The range of text to yank
   * @param type       The type of yank
   * @return true if able to yank the range, false if not
   */
  public boolean yankRange(@NotNull Editor editor, @Nullable TextRange range, @NotNull SelectionType type, boolean moveCursor) {
    if (range != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("yanking range: " + range);
      }
      boolean res = CommandGroups.getInstance().getRegister().storeText(editor, range, type, false);
      if (moveCursor) {
        MotionGroup.moveCaret(editor, range.normalize().getStartOffset());
      }

      return res;
    }

    return false;
  }

  /**
   * Pastes text from the last register into the editor before the current cursor location.
   *
   * @param editor      The editor to paste into
   * @param context     The data context
   * @param count       The number of times to perform the paste
   * @return true if able to paste, false if not
   */
  public boolean putTextBeforeCursor(@NotNull Editor editor, @NotNull DataContext context, int count, boolean indent,
                                     boolean cursorAfter) {
    // What register are we getting the text from?
    Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
    if (reg != null) {
      if (reg.getType() == SelectionType.LINE_WISE && editor.isOneLineMode()) {
        return false;
      }

      int pos;
      // If a linewise put the text is inserted before the current line.
      if (reg.getType() == SelectionType.LINE_WISE) {
        pos = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
      }
      else {
        pos = editor.getCaretModel().getOffset();
      }

      putText(editor, context, pos, reg.getText(), reg.getType(), count, indent, cursorAfter, CommandState.SubMode.NONE);

      return true;
    }

    return false;
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
    Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
    if (reg != null) {
      if (reg.getType() == SelectionType.LINE_WISE && editor.isOneLineMode()) {
        return false;
      }

      int pos;
      // If a linewise paste, the text is inserted after the current line.
      if (reg.getType() == SelectionType.LINE_WISE) {
        pos = Math.min(editor.getDocument().getTextLength(),
                       CommandGroups.getInstance().getMotion().moveCaretToLineEnd(editor, true) + 1);
        if (pos > 0 && pos == editor.getDocument().getTextLength() &&
            editor.getDocument().getCharsSequence().charAt(pos - 1) != '\n') {
          editor.getDocument().insertString(pos, "\n");
          pos++;
        }
      }
      else {
        pos = editor.getCaretModel().getOffset() + 1;
      }
      // In case when text is empty this can occur
      if (pos > 0 && pos > editor.getDocument().getTextLength()) {
        pos--;
      }
      putText(editor, context, pos, reg.getText(), reg.getType(), count, indent, cursorAfter, CommandState.SubMode.NONE);

      return true;
    }

    return false;
  }

  public boolean putVisualRange(@NotNull Editor editor, @NotNull DataContext context, @NotNull TextRange range, int count, boolean indent,
                                boolean cursorAfter) {
    CommandState.SubMode subMode = CommandState.getInstance(editor).getSubMode();
    Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
    // Without this reset, the deleted text goes into the same register we just pasted from.
    CommandGroups.getInstance().getRegister().resetRegister();
    if (reg != null) {
      if (reg.getType() == SelectionType.LINE_WISE && editor.isOneLineMode()) {
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

      CommandGroups.getInstance().getChange().deleteRange(editor, range, SelectionType.fromSubMode(subMode), false);

      editor.getCaretModel().moveToOffset(start);

      int pos = start;
      if (reg.getType() == SelectionType.LINE_WISE) {
        if (subMode == CommandState.SubMode.VISUAL_LINE) {
        }
        else if (subMode == CommandState.SubMode.VISUAL_BLOCK) {
          pos = editor.getDocument().getLineEndOffset(endLine) + 1;
        }
        else {
          editor.getDocument().insertString(start, "\n");
          pos = start + 1;
        }
      }
      else if (reg.getType() == SelectionType.BLOCK_WISE) {
      }
      else /* Characterwise */ {
        if (subMode == CommandState.SubMode.VISUAL_LINE) {
          editor.getDocument().insertString(start, "\n");
        }
      }

      putText(editor, context, pos, reg.getText(), reg.getType(), count,
              indent && reg.getType() == SelectionType.LINE_WISE, cursorAfter, subMode);

      return true;
    }

    return false;
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
  public void putText(@NotNull Editor editor, @NotNull DataContext context, int offset, @NotNull String text, @NotNull SelectionType type, int count,
                      boolean indent, boolean cursorAfter, @NotNull CommandState.SubMode mode) {
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
        CommandGroups.getInstance().getChange().insertText(editor, offset, text);
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
          CommandGroups.getInstance().getChange().insertText(editor, EditorHelper.getFileSize(editor, true), "\n");
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
          StringBuffer extra = new StringBuffer(maxlen - segment.length());
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
          CommandGroups.getInstance().getChange().insertText(editor, insoff, txt);
          insertCnt += txt.length();
          endOffset += txt.length();
        }


        if (mode == CommandState.SubMode.VISUAL_LINE) {
          CommandGroups.getInstance().getChange().insertText(editor, endOffset, "\n");
          insertCnt++;
          endOffset++;
        }
        else {
          if (pad.length() > 0) {
            CommandGroups.getInstance().getChange().insertText(editor, insoff, pad);
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
      editor.getSelectionModel().setSelection(startOff, endOff);
      KeyHandler.executeAction("OrigAutoIndentLines", context);
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
        MotionGroup.moveCaret(editor, offset);
        break;
      case 2:
        MotionGroup.moveCaret(editor, endOffset - 1);
        break;
      case 3:
        MotionGroup.moveCaret(editor, offset);
        MotionGroup.moveCaret(editor,
                              CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor));
        break;
      case 4:
        MotionGroup.moveCaret(editor, endOffset + 1);
        break;
      case 5:
        int pos = Math.min(endOffset, EditorHelper.getLineEndForOffset(editor, endOffset - 1) - 1);
        MotionGroup.moveCaret(editor, pos);
        break;
    }

    final MarkGroup markGroup = CommandGroups.getInstance().getMark();
    markGroup.setMark(editor, '[', offset);
    markGroup.setMark(editor, ']', endOffset);

    /*
    // Adjust the cursor position after the paste
    if ((type & Command.FLAG_MOT_LINEWISE) != 0)
    {
        if (cursorAfter)
        {
            int pos = EditorHelper.normalizeOffset(editor, offset + count * text.length(), false);
            MotionGroup.moveCaret(editor, context, pos);
        }
        else
        {
            MotionGroup.moveCaret(editor, context, offset);
            MotionGroup.moveCaret(editor, context,
                CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor));
        }
    }
    else
    {
        MotionGroup.moveCaret(editor, context, offset + count * text.length() - 1);
    }
    */
  }

  private static Logger logger = Logger.getInstance(CopyGroup.class.getName());
}
