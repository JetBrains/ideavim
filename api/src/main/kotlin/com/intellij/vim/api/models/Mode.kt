/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.models


/**
 * Represents a Vim editor mode.
 */
enum class Mode {
  /**
   * Normal mode - the default mode where commands and motions can be executed.
   */
  NORMAL,

  /**
   * Operator-pending mode - entered after an operator command is given but before a motion is provided.
   */
  OP_PENDING,

  /**
   * Operator-pending mode with forced characterwise operation.
   */
  OP_PENDING_CHARACTERWISE,

  /**
   * Operator-pending mode with forced linewise operation.
   */
  OP_PENDING_LINEWISE,

  /**
   * Operator-pending mode with forced blockwise operation.
   */
  OP_PENDING_BLOCKWISE,

  /**
   * Normal mode using i_CTRL-O in Insert-mode.
   */
  NORMAL_FROM_INSERT,

  /**
   * Normal mode using i_CTRL-O in Replace-mode.
   */
  NORMAL_FROM_REPLACE,

  /**
   * Normal mode using i_CTRL-O in Virtual-Replace-mode.
   */
  NORMAL_FROM_VIRTUAL_REPLACE,

  /**
   * Visual mode with character-wise selection.
   */
  VISUAL_CHARACTER,

  /**
   * Visual mode with character-wise selection using v_CTRL-O in Select mode.
   */
  VISUAL_CHARACTER_FROM_SELECT,

  /**
   * Visual mode with line-wise selection.
   */
  VISUAL_LINE,

  /**
   * Visual mode with line-wise selection using v_CTRL-O in Select mode.
   */
  VISUAL_LINE_FROM_SELECT,

  /**
   * Visual mode with block-wise selection.
   */
  VISUAL_BLOCK,

  /**
   * Visual mode with block-wise selection using v_CTRL-O in Select mode.
   */
  VISUAL_BLOCK_FROM_SELECT,

  /**
   * Select mode with character-wise selection.
   */
  SELECT_CHARACTER,

  /**
   * Select mode with line-wise selection.
   */
  SELECT_LINE,

  /**
   * Select mode with block-wise selection.
   */
  SELECT_BLOCK,

  /**
   * Insert mode - used for inserting text.
   */
  INSERT,

  /**
   * Replace mode - used for replacing existing text.
   */
  REPLACE,

  /**
   * Command-line mode - used for entering Ex commands.
   */
  COMMAND_LINE;

  /**
   * Returns the TextType associated with this mode, if applicable.
   * Only visual and select modes have a TextType.
   */
  val selectionType: TextType?
    get() = when (this) {
      VISUAL_CHARACTER, VISUAL_CHARACTER_FROM_SELECT, SELECT_CHARACTER -> TextType.CHARACTER_WISE
      VISUAL_LINE, VISUAL_LINE_FROM_SELECT, SELECT_LINE -> TextType.LINE_WISE
      VISUAL_BLOCK, VISUAL_BLOCK_FROM_SELECT, SELECT_BLOCK -> TextType.BLOCK_WISE
      else -> null
    }

  /**
   * Returns true if this mode is a visual mode.
   */
  val isVisual: Boolean
    get() = this == VISUAL_CHARACTER || this == VISUAL_LINE || this == VISUAL_BLOCK ||
      this == VISUAL_CHARACTER_FROM_SELECT || this == VISUAL_LINE_FROM_SELECT || this == VISUAL_BLOCK_FROM_SELECT

  /**
   * Returns true if this mode is a select mode.
   */
  val isSelect: Boolean
    get() = this == SELECT_CHARACTER || this == SELECT_LINE || this == SELECT_BLOCK
}
