/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api


/**
 * Represents a Vim editor mode.
 */
sealed interface Mode {
  /**
   * The mode to return to when exiting the current mode.
   * This is used to determine which mode to switch to after completing an operation.
   */
  val returnTo: Mode

  /**
   * Normal mode - the default mode where commands and motions can be executed.
   *
   * @property originalMode The mode to return to when exiting this mode, if specified.
   */
  data class NORMAL(private val originalMode: Mode? = null) : Mode {
    override val returnTo: Mode
      get() = originalMode ?: this
  }

  /**
   * Operator-pending mode - entered after an operator command is given but before a motion is provided.
   *
   * @property returnTo The mode to return to after the operation is complete.
   */
  data class OP_PENDING(override val returnTo: Mode) : Mode

  /**
   * Visual mode - used for selecting text.
   *
   * @property selectionType The type of selection (character, line, or block).
   * @property returnTo The mode to return to when exiting visual mode.
   */
  data class VISUAL(val selectionType: TextType, override val returnTo: Mode = NORMAL()) : Mode

  /**
   * Select mode - similar to visual mode but with different behavior.
   *
   * @property selectionType The type of selection (character, line, or block).
   * @property returnTo The mode to return to when exiting select mode.
   */
  data class SELECT(val selectionType: TextType, override val returnTo: Mode = NORMAL()) : Mode

  /**
   * Insert mode - used for inserting text.
   */
  object INSERT : Mode {
    override val returnTo: Mode = NORMAL()
  }

  /**
   * Replace mode - used for replacing existing text.
   */
  object REPLACE : Mode {
    override val returnTo: Mode = NORMAL()
  }

  /**
   * Command-line mode - used for entering Ex commands.
   *
   * @property returnTo The mode to return to after executing the command.
   */
  data class CMD_LINE(override val returnTo: Mode) : Mode
}
