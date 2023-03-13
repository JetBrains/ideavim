/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

public enum class Direction(private val value: Int) {
  BACKWARDS(-1), FORWARDS(1);

  public fun toInt(): Int = value
  public fun reverse(): Direction = when (this) {
    BACKWARDS -> FORWARDS
    FORWARDS -> BACKWARDS
  }

  public companion object {
    public fun fromInt(value: Int): Direction = when (value) {
      BACKWARDS.value -> BACKWARDS
      FORWARDS.value -> FORWARDS
      else -> FORWARDS
    }
  }
}
