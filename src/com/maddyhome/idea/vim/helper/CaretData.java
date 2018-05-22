package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.group.MotionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used to manipulate caret specific data. Each caret has a user defined map associated with it.
 * These methods provide convenient methods for working with that Vim Plugin specific data.
 */
public class CaretData {
  /**
   * This gets the last column the caret was in.
   *
   * @param caret The caret to get the last column for
   * @return Returns the last column as set by {@link #setLastColumn} or the current caret column
   */
  public static int getLastColumn(@NotNull Caret caret) {
    Integer col = caret.getUserData(LAST_COLUMN);
    if (col == null) {
      return caret.getVisualPosition().column;
    }
    else {
      return col;
    }
  }

  /**
   * Sets the last column for this caret in this editor
   *
   * @param col    The column
   * @param caret  The caret
   * @param editor The editor
   */
  public static void setLastColumn(@NotNull Editor editor, @NotNull Caret caret, int col) {
    boolean previousWasDollar = getLastColumn(caret) >= MotionGroup.LAST_COLUMN;
    boolean currentIsDollar = col >= MotionGroup.LAST_COLUMN;

    if (!CommandState.inVisualBlockMode(editor)) {
      caret.putUserData(LAST_COLUMN, col);
    }
    else {
      editor.getCaretModel().getPrimaryCaret().putUserData(LAST_COLUMN, col);
    }

    if (previousWasDollar != currentIsDollar && CommandState.inVisualBlockMode(editor)) {
      VimPlugin.getMotion().updateBlockSelection(editor);
    }
  }

  /**
   * Gets the visual block start for the caret.
   */
  public static int getVisualStart(@NotNull Caret caret) {
    Integer visualStart = caret.getUserData(VISUAL_START);

    if (visualStart == null) {
      return caret.getOffset();
    }
    else {
      return visualStart;
    }
  }

  /**
   * Sets the visual block start for the caret.
   */
  public static void setVisualStart(@NotNull Caret caret, int visualStart) {
    caret.putUserData(VISUAL_START, visualStart);
  }

  /**
   * Gets the visual block end for the caret.
   */
  public static int getVisualEnd(@NotNull Caret caret) {
    Integer visualEnd = caret.getUserData(VISUAL_END);

    if (visualEnd == null) {
      return caret.getOffset();
    }
    else {
      return visualEnd;
    }
  }

  /**
   * Sets the visual block end for the caret.
   */
  public static void setVisualEnd(@NotNull Caret caret, int visualEnd) {
    caret.putUserData(VISUAL_END, visualEnd);
  }

  /**
   * Gets the visual offset for the caret.
   */
  public static int getVisualOffset(@NotNull Caret caret) {
    Integer visualOffset = caret.getUserData(VISUAL_OFFSET);

    if (visualOffset == null) {
      return caret.getOffset();
    }
    else {
      return visualOffset;
    }
  }

  /**
   * Sets the visual offset for the caret.
   */
  public static void setVisualOffset(@NotNull Caret caret, int visualOffset) {
    caret.putUserData(VISUAL_OFFSET, visualOffset);
  }

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
   * Gets the previous last column (set by {@link com.maddyhome.idea.vim.handler.VisualOperatorActionHandler.VisualStartFinishRunnable}).
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
   * Gets the text range for current visual operator action.
   */
  @Nullable
  public static TextRange getVisualTextRange(@NotNull Caret caret) {
    return caret.getUserData(VISUAL_TEXT_RANGE);
  }

  /**
   * Sets the text range for current visual operator action.
   */
  public static void setVisualTextRange(@NotNull Caret caret, @Nullable TextRange range) {
    caret.putUserData(VISUAL_TEXT_RANGE, range);
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

  private static final Key<Integer> LAST_COLUMN = new Key<>("lastColumn");
  private static final Key<Integer> VISUAL_START = new Key<>("visualStart");
  private static final Key<Integer> VISUAL_END = new Key<>("visualEnd");
  private static final Key<Integer> VISUAL_OFFSET = new Key<>("visualOffset");
  private static final Key<Integer> PREV_LAST_COLUMN = new Key<>("previousLastColumn");
  private static final Key<Integer> INSERT_START = new Key<>("insertStart");
  private static final Key<Boolean> WAS_IN_FIRST_LINE = new Key<>("wasInFirstLine");
  private static final Key<VisualChange> VISUAL_CHANGE = new Key<>("visualChange");
  private static final Key<VisualChange> VISUAL_OP = new Key<>("visualOp");
  private static final Key<TextRange> VISUAL_TEXT_RANGE = new Key<>("visualTextRange");
}
