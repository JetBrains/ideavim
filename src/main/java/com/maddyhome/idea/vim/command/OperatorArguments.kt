package com.maddyhome.idea.vim.command

/**
 * [count0] is a raw count entered by user. May be zero.
 * [count1] is the same count, but 1-based. If [count0] is zero, [count1] is one.
 * The terminology is taken directly from vim.
 */
data class OperatorArguments(
  val isOperatorPending: Boolean,
  val count0: Int,
) {
  val count1: Int = count0.coerceAtLeast(1)

  fun withCount0(count0: Int) = this.copy(count0 = count0)
}
