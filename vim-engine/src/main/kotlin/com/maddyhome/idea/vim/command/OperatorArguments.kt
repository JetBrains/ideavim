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
data class OperatorArguments(
  val isOperatorPending: Boolean,
  val count0: Int,

  val mode: Mode,
) {
  val count1: Int = count0.coerceAtLeast(1)

  fun withCount0(count0: Int): OperatorArguments = this.copy(count0 = count0)
}
