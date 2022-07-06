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
package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.injector
import java.util.*
import java.util.stream.Collectors
import javax.swing.KeyStroke

/**
 * COMPATIBILITY-LAYER: Created a helper class
 * Please see: https://jb.gg/zo8n0r
 */
object StringHelper {
  @JvmStatic
  fun parseKeys(string: String): List<KeyStroke> {
    return injector.parser.parseKeys(string)
  }

  @JvmStatic
  fun parseKeys(vararg string: String): List<KeyStroke> {
    return Arrays.stream(string).flatMap { o: String -> injector.parser.parseKeys(o).stream() }
      .collect(Collectors.toList())
  }

  @JvmStatic
  fun isCloseKeyStroke(stroke: KeyStroke): Boolean {
    return stroke.isCloseKeyStroke()
  }
}
