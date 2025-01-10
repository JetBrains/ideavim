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
 * * It is not clear if this represents the arguments to an operator (while the operator's [Command] also has arguments)
 * * [mode] is the mode _before_ the command is completed, which is not guaranteed to be the same as the mode once the
 *   command completes. There is no indication of this difference, which could lead to confusion
 * * The count is (correctly) the count for the whole command, rather than the operator, or the operator's arguments
 *   (the in-progress motion)
 *
 * @param isOperatorPending Deprecated. The value is used to indicate that a command is operator+motion and was
 * previously used to change the behaviour of the motion (the EOL character is counted in this scenario - see
 * `:help whichwrap`). It is better to register a separate action for [Mode.OP_PENDING] rather than expect a runtime
 * flag for something that can be handled statically.
 * @param count0 The raw count of the entire command. E.g., if the command is `2d3w`, then this count will be `6`, even
 * when this class is passed to the `d` operator action (the count applies to the motion).
 * @param mode Deprecated. The mode of the editor at the time that the [OperatorArguments] is created, which is _before_
 * the command is completed. This was previously used to check for [Mode.OP_PENDING], but is no longer required. Prefer
 * [VimEditor.mode] instead.
 */
data class OperatorArguments
@Deprecated(
  "Use overload without isOperatorPending. Value can be calculated from mode",
  replaceWith = ReplaceWith("OperatorArguments(count0, mode)"),
) constructor(
  // This is used by EasyMotion
  @Deprecated("It is better to register a separate OP_PENDING action than switch on a runtime flag") val isOperatorPending: Boolean,
  val count0: Int,
  @Deprecated("Represents the mode when the OperatorArguments was created, not the current mode. Prefer editor.mode") val mode: Mode,
) {

  /**
   * Create a new instance of [OperatorArguments]
   *
   * @param count0  The 0-based count for the whole command
   * @param mode    Only used for the deprecated [OperatorArguments.mode] property
   */
  @Suppress("DEPRECATION")
  constructor(count0: Int, mode: Mode) : this(mode is Mode.OP_PENDING, count0, mode)

  val count1: Int = count0.coerceAtLeast(1)
}
