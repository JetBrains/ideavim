/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.expressions

enum class Scope(val c: String) {

  BUFFER_VARIABLE("b"),
  WINDOW_VARIABLE("w"),
  TABPAGE_VARIABLE("t"),
  GLOBAL_VARIABLE("g"),
  LOCAL_VARIABLE("l"),
  SCRIPT_VARIABLE("s"),
  FUNCTION_VARIABLE("a"),
  VIM_VARIABLE("v");

  companion object {
    fun getByValue(s: String): Scope {
      return values().first { it.c == s }
    }
  }
}
