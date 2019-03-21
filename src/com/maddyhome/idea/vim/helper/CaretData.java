/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.util.Key;
import com.maddyhome.idea.vim.command.VisualChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used to manipulate caret specific data. Each caret has a user defined map associated with it.
 * These methods provide convenient methods for working with that Vim Plugin specific data.
 */
public class CaretData {

  /**
   * Gets the previous visual operator range on the caret.
   */
  @Nullable
  public static VisualChange getLastVisualOperatorRange(@NotNull Caret caret) {
    return caret.getUserData(VISUAL_OP);
  }

  /**
   * Sets the previous visual operator range on the caret.
   */
  public static void setLastVisualOperatorRange(@NotNull Caret caret, @NotNull VisualChange range) {
    caret.putUserData(VISUAL_OP, range);
  }

  /**
   * Gets the previous last column (set by {@link com.maddyhome.idea.vim.handler.VisualOperatorActionHandler.VisualStartFinishWrapper}).
   */
  public static int getPreviousLastColumn(@NotNull Caret caret) {
    @Nullable Integer ret = caret.getUserData(PREV_LAST_COLUMN);


    if (ret == null) {
      return caret.getLogicalPosition().column;
    }
    else {
      return ret;
    }
  }

  /**
   * Sets the previous last column.
   */
  public static void setPreviousLastColumn(@NotNull Caret caret, int prevLastColumn) {
    caret.putUserData(PREV_LAST_COLUMN, prevLastColumn);
  }

  /**
   * Gets the visual change for current visual operator action.
   */
  @Nullable
  public static VisualChange getVisualChange(@NotNull Caret caret) {
    return caret.getUserData(VISUAL_CHANGE);
  }

  /**
   * Sets the visual change for current visual operator action.
   */
  public static void setVisualChange(@NotNull Caret caret, @Nullable VisualChange visualChange) {
    caret.putUserData(VISUAL_CHANGE, visualChange);
  }

  /**
   * Gets the insertion start for the caret
   */
  public static int getInsertStart(@NotNull Caret caret) {
    Integer ret = caret.getUserData(INSERT_START);

    if (ret == null) {
      return caret.getOffset();
    }
    else {
      return ret;
    }
  }

  /**
   * Set the insertion start for the caret
   */
  public static void setInsertStart(@NotNull Caret caret, int insertStart) {
    caret.putUserData(INSERT_START, insertStart);
  }

  /**
   * Determines whether a caret was in the first line before inserting a new line above.
   */
  public static boolean wasInFirstLine(@NotNull Caret caret) {
    Boolean res = caret.getUserData(WAS_IN_FIRST_LINE);
    return (res != null) && res;
  }

  /**
   * Sets the flag determining that a caret was in the first line before inserting a new line above.
   */
  public static void setWasInFirstLine(@NotNull Caret caret, boolean value) {
    caret.putUserData(WAS_IN_FIRST_LINE, value);
  }

  /**
   * This class is completely static, no instances needed.
   */
  private CaretData() {
  }

  private static final Key<Integer> PREV_LAST_COLUMN = new Key<>("previousLastColumn");
  private static final Key<Integer> INSERT_START = new Key<>("insertStart");
  private static final Key<Boolean> WAS_IN_FIRST_LINE = new Key<>("wasInFirstLine");
  private static final Key<VisualChange> VISUAL_CHANGE = new Key<>("visualChange");
  private static final Key<VisualChange> VISUAL_OP = new Key<>("visualOp");
}
