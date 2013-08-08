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

package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.range.AbstractRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.EditorHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the set of range values entered as part of an Ex command.
 */
public class Ranges {
  /**
   * Create the empty range list
   */
  public Ranges() {
    ranges = new ArrayList<Range>();
  }

  /**
   * Adds a range to the list
   *
   * @param range The list of ranges to append to the current list
   */
  public void addRange(@NotNull Range[] range) {
    for (Range aRange : range) {
      ranges.add(aRange);
    }
  }

  /**
   * Gets the number of ranges in the list
   *
   * @return The range count
   */
  public int size() {
    return ranges.size();
  }

  /**
   * Sets the default line to be used by this range if no range was actually given by the user. -1 is used to
   * mean the current line.
   *
   * @param line The line or -1 for current line
   */
  public void setDefaultLine(int line) {
    defaultLine = line;
  }

  /**
   * Gets the line of the last range specified in the range list
   *
   * @param editor  The editor to get the line for
   * @param context The data context
   * @return The line number represented by the range
   */
  public int getLine(@NotNull Editor editor, DataContext context) {
    processRange(editor, context);

    return endLine;
  }

  /**
   * Gets the start line number the range represents
   *
   * @param editor  The editor to get the line number for
   * @param context The data context
   * @return The starting line number
   */
  public int getFirstLine(@NotNull Editor editor, DataContext context) {
    processRange(editor, context);

    return startLine;
  }

  /**
   * Gets the count for an Ex command. This is either an explicit count enter at the end of the command or the
   * end of the specified range.
   *
   * @param editor  The editor to get the count for
   * @param context The data context
   * @param count   The count given at the end of the command or -1 if no such count (use end line)
   * @return count if count != -1, else return end line of range
   */
  public int getCount(@NotNull Editor editor, DataContext context, int count) {
    if (count == -1) {
      return getLine(editor, context);
    }
    else {
      return count;
    }
  }

  /**
   * Gets the line range represented by this range. If a count is given, the range is the range end line through
   * count-1 lines. If no count is given (-1), the range is the range given by the user.
   *
   * @param editor  The editor to get the range for
   * @param context The data context
   * @param count   The count given at the end of the command or -1 if no such count
   * @return The line range
   */
  @NotNull
  public LineRange getLineRange(@NotNull Editor editor, DataContext context, int count) {
    processRange(editor, context);
    int end;
    int start;
    if (count == -1) {
      end = endLine;
      start = startLine;
    }
    else {
      start = endLine;
      end = start + count - 1;
    }

    return new LineRange(start, end);
  }

  /**
   * Gets the text range represented by this range. If a count is given, the range is the range end line through
   * count-1 lines. If no count is given (-1), the range is the range given by the user. The text range is based
   * on the line range but this is character based from the start of the first line to the end of the last line.
   *
   * @param editor  The editor to get the range for
   * @param context The data context
   * @param count   The count given at the end of the command or -1 if no such count
   * @return The text range
   */
  @NotNull
  public TextRange getTextRange(@NotNull Editor editor, DataContext context, int count) {
    LineRange lr = getLineRange(editor, context, count);
    int start = EditorHelper.getLineStartOffset(editor, lr.getStartLine());
    int end = EditorHelper.getLineEndOffset(editor, lr.getEndLine(), true) + 1;

    return new TextRange(start, Math.min(end, EditorHelper.getFileSize(editor)));
  }

  /**
   * Helper method to get the text range for the current cursor line
   *
   * @param editor  The editor to get the range for
   * @param context The data context
   * @return The range of the current line
   */
  @NotNull
  public static TextRange getCurrentLineRange(@NotNull Editor editor, DataContext context) {
    Ranges ranges = new Ranges();

    return ranges.getTextRange(editor, context, -1);
  }

  /**
   * Helper method to get the text range for the current file
   *
   * @param editor  The editor to get the range for
   * @param context The data context
   * @return The range of the current file
   */
  @NotNull
  public static TextRange getFileTextRange(@NotNull Editor editor, DataContext context) {
    Ranges ranges = new Ranges();
    ranges.addRange(AbstractRange.createRange("%", 0, false));

    return ranges.getTextRange(editor, context, -1);
  }

  /**
   * Processes the list of ranges and calculates the start and end lines of the range
   *
   * @param editor  The editor to get the lines for
   * @param context The data context
   */
  private void processRange(@NotNull Editor editor, DataContext context) {
    // Already done
    if (done) return;

    // Start with the range being the current line
    startLine = defaultLine == -1 ? editor.getCaretModel().getLogicalPosition().line : defaultLine;
    endLine = startLine;
    boolean lastZero = false;
    // Now process each range, moving the cursor if appropriate
    for (Range range : ranges) {
      startLine = endLine;
      endLine = range.getLine(editor, context, lastZero);
      if (range.isMove()) {
        MotionGroup.moveCaret(editor,
                              CommandGroups.getInstance().getMotion().moveCaretToLine(editor, endLine));
      }
      // Did that last range represent the start of the file?
      lastZero = (endLine < 0);
      count++;
    }

    // If only one range given, make the start and end the same
    if (count == 1) {
      startLine = endLine;
    }

    done = true;
  }

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("Ranges[ranges=").append(ranges);
    res.append("]");

    return res.toString();
  }

  private int startLine = 0;
  private int endLine = 0;
  private int count = 0;
  private int defaultLine = -1;
  private boolean done = false;
  private List<Range> ranges;
}
