package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
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
   * This class is completely static, no instances needed.
   */
  private CaretData() {
  }

  private static final Key<Integer> LAST_COLUMN = new Key<>("lastColumn");
  private static final Key<Integer> VISUAL_START = new Key<>("visualStart");
  private static final Key<Integer> VISUAL_END = new Key<>("visualEnd");
  private static final Key<Integer> VISUAL_OFFSET = new Key<>("visualOffset");
}
