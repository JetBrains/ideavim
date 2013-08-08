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

package com.maddyhome.idea.vim.ex.range;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base for all Ex command ranges
 */
public abstract class AbstractRange implements Range {
  /**
   * Factory method used to create an appropriate range based on the range text
   *
   * @param str    The range text
   * @param offset Any offset specified after the range
   * @param move   True if cursor should be moved to range line
   * @return The ranges appropriate to the text
   */
  @Nullable
  public static Range[] createRange(@NotNull String str, int offset, boolean move) {
    // Current line
    if (str.equals(".") || str.length() == 0) {
      return new Range[]{new LineNumberRange(offset, move)};
    }
    // All lines
    else if (str.equals("%")) {
      return new Range[]{
        new LineNumberRange(0, 0, move),
        new LineNumberRange(LineNumberRange.LAST_LINE, offset, move)
      };
    }
    // Last line
    else if (str.equals("$")) {
      return new Range[]{new LineNumberRange(LineNumberRange.LAST_LINE, offset, move)};
    }
    // Mark like
    else if (str.startsWith("'") && str.length() == 2) {
      return new Range[]{new MarkRange(str.charAt(1), offset, move)};
    }
    // Pattern
    else if (str.startsWith("/") || str.startsWith("\\/") || str.startsWith("\\&")) {
      return new Range[]{new SearchRange(str, offset, move)};
    }
    // Pattern
    else if (str.startsWith("?") || str.startsWith("\\?")) {
      return new Range[]{new SearchRange(str, offset, move)};
    }
    // Specific line number (1 based)
    else {
      try {
        int line = Integer.parseInt(str) - 1;

        return new Range[]{new LineNumberRange(line, offset, move)};
      }
      catch (NumberFormatException e) {
        // Ignore - we'll send back bad range later.
      }
    }

    // User entered an invalid range.
    return null;
  }

  /**
   * Create the range
   *
   * @param offset The line offset
   * @param move   True if cursor moved
   */
  public AbstractRange(int offset, boolean move) {
    this.offset = offset;
    this.move = move;
  }

  /**
   * Gets the line offset
   *
   * @return The line offset
   */
  protected int getOffset() {
    return offset;
  }

  /**
   * Should the cursor move
   *
   * @return True if cursor should move, false if not
   */
  public boolean isMove() {
    return move;
  }

  /**
   * Gets the line number (0 based) specificied by this range. Includes the offset.
   *
   * @param editor   The editor to get the line for
   * @param context  The data context
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if unable to get the line number
   */
  public int getLine(Editor editor, DataContext context, boolean lastZero) {
    int line = getRangeLine(editor, context, lastZero);

    return line + offset;
  }

  @NotNull
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("AbstractRange");
    sb.append("{offset=").append(offset);
    sb.append(", move=").append(move);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Gets the line number specified by this range without regard to any offset.
   *
   * @param editor   The editor to get the line for
   * @param context  The data context
   * @param lastZero True if last line was set to start of file
   * @return The zero based line number, -1 if inable to get the line number
   */
  protected abstract int getRangeLine(Editor editor, DataContext context, boolean lastZero);

  protected int offset;
  protected boolean move;
}
