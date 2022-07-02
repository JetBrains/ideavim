/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

/**
 * [count0] is a raw count entered by user. May be zero.
 * [count1] is the same count, but 1-based. If [count0] is zero, [count1] is one.
 * The terminology is taken directly from vim.
 * If no count is provided, [count0] defaults to zero.
 */
data class OperatorArguments(
  val isOperatorPending: Boolean,
  val count0: Int,

  val mode: VimStateMachine.Mode,
  val subMode: VimStateMachine.SubMode,
) {
  val count1: Int = count0.coerceAtLeast(1)

  fun withCount0(count0: Int) = this.copy(count0 = count0)
}
