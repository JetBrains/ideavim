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

/**
 * Represents an Ex command range
 */
public interface Range {
  /**
   * Get the line number this range represents
   *
   * @param editor   The editor to get the line for
   * @param context  The data context
   * @param lastZero True if the last line set represents before the start of the false
   * @return The zero based logical line in the editor that the range represents
   */
  int getLine(Editor editor, DataContext context, boolean lastZero);

  /**
   * Should the cursor be moved to this range's line?
   *
   * @return True if cursor should be moved, false if not
   */
  boolean isMove();
}
