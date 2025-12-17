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
    fun getByValue(s: String) = entries.firstOrNull { it.c == s }

    fun split(scopedName: String): Pair<Scope?, String> {
      val colonIndex = scopedName.indexOf(":")
      if (colonIndex == -1) {
        return Pair(null, scopedName)
      }
      val scopeString = scopedName.substring(0, colonIndex)
      val nameString = scopedName.substring(colonIndex + 1)
      return Pair(getByValue(scopeString), nameString)
    }
  }

  override fun toString() = "$c:"
}

fun Scope?.format(name: String) = if (this != null) this.toString() + name else name
