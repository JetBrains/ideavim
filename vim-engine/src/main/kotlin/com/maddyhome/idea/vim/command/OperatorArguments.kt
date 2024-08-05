/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.Mode

/**
 * Represents arguments used when executing a command - either an action, operator or motion
 *
 * TODO: Remove, rename or otherwise refactor this class
 *
 * Problems with this class:
 * * The name is misleading, as it is used when executing motions that do not have an operator, as well as when
 *   executing the operator itself. Or even when executing actions that are neither operators nor motions
 * * [mode] is not necessarily the same as [VimEditor.mode], but it's not clear how or why
 * * The count is (and must be) the count for the whole command, rather than the operator, or for the in-progress
 *   motion. This is not it's not clear in this class
 *
 * @param isOperatorPending `true` if the editor is in [Mode.OP_PENDING] at the time of creating this class.
 * @param count0 The raw count of the entire command. E.g., if the command is `2d3w`, then this count will be `6`, even
 *        if when this class is passed to the `d` operator action (the count applies to the motion).
 * @param mode The current mode of the editor. This is not guaranteed to match [VimEditor.mode], but it is unclear why.
 */
data class OperatorArguments
@Deprecated(
  "Use overload without isOperatorPending. Value can be calculated from mode",
  replaceWith = ReplaceWith("OperatorArguments(count0, mode)")
) constructor(
  val isOperatorPending: Boolean,
  val count0: Int,
  val mode: Mode,
) {

  /**
   * Create a new instance of [OperatorArguments]
   *
   * @param count0  The 0-based count for the whole command
   * @param mode    Not the same as [VimEditor.mode]!
   */
  @Suppress("DEPRECATION")
  constructor(count0: Int, mode: Mode) : this(mode is Mode.OP_PENDING, count0, mode)

  val count1: Int = count0.coerceAtLeast(1)
}
