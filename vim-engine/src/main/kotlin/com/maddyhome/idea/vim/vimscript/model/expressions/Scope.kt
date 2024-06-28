/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

enum class Scope(val c: String) {

  BUFFER_VARIABLE("b"),
  WINDOW_VARIABLE("w"),
  TABPAGE_VARIABLE("t"),
  GLOBAL_VARIABLE("g"),
  LOCAL_VARIABLE("l"),
  SCRIPT_VARIABLE("s"),
  FUNCTION_VARIABLE("a"),
  VIM_VARIABLE("v"),
  ;

  companion object {
    fun getByValue(s: String): Scope? {
      return entries.firstOrNull { it.c == s }
    }
  }

  override fun toString(): String {
    return "$c:"
  }
}
