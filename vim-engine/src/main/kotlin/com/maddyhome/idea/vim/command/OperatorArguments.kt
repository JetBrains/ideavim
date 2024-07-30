/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.state.mode.Mode

/**
 * [count0] is a raw count entered by user. May be zero.
 * [count1] is the same count, but 1-based. If [count0] is zero, [count1] is one.
 * The terminology is taken directly from vim.
 * If no count is provided, [count0] defaults to zero.
 */
// TODO: This class should be removed, refactored or at the least renamed
// However, there are plugin compatibilities to consider...
// Reasons:
// * The naming is confusing, as it is used for non-operator commands as well as motions for operators
//   Also, does the class represent the arguments for an operator, or data for the in-progress motion argument for an
//   operator (in which case, it's even more confusing for non-operator commands)
// * The count is (and must be) the count for the whole command rather than for the operator, or for the in-progress
//   motion. It's not clear here what it's for
// * The mode and `isOperatorPending` properties are snapshots of the mode _before_ the command is executed, rather than
//   the current mode. E.g., it will be OP_PENDING even when the current mode has returned to NORMAL or VISUAL. There is
//   no indication that this is the case, or reasons why
data class OperatorArguments(
  val isOperatorPending: Boolean,
  val count0: Int,

  val mode: Mode,
) {
  val count1: Int = count0.coerceAtLeast(1)

  fun withCount0(count0: Int): OperatorArguments = this.copy(count0 = count0)
}
