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

package com.maddyhome.idea.vim.diagnostic

interface VimLogger {
  fun isTrace(): Boolean
  fun trace(data: String)

  fun isDebug(): Boolean
  fun debug(data: String)

  fun warn(message: String)
}

fun VimLogger.trace(message: () -> String) {
  if (isTrace()) {
    trace(message())
  }
}

fun VimLogger.debug(message: () -> String) {
  if (isDebug()) {
    debug(message())
  }
}
