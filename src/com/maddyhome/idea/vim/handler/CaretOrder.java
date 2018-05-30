package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.editor.CaretModel;

/**
 * This represents the order in which carets are given to the handlers.
 */
public enum CaretOrder {
  /**
   * Native order in which carets are given in {@link CaretModel#getAllCarets()}
   */
  NATIVE,

  /**
   * Carets are ordered by offset, increasing
   */
  INCREASING_OFFSET,

  /**
   * Carets are ordered by offset, decreasing
   */
  DECREASING_OFFSET
}
