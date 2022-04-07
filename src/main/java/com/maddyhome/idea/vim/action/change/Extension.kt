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

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.extension.VimExtensionHandler
import javax.swing.KeyStroke

object Extension {
  var lastExtensionHandler: VimExtensionHandler? = null

  private val keyStrokes = mutableListOf<KeyStroke>()
  private val strings = mutableListOf<String>()

  private var keystrokePointer = 0
  private var stringPointer = 0

  fun addKeystroke(key: KeyStroke) = keyStrokes.add(key)
  fun addString(key: String) = strings.add(key)

  fun consumeKeystroke(): KeyStroke? {
    if (keystrokePointer in keyStrokes.indices) {
      keystrokePointer += 1
      return keyStrokes[keystrokePointer - 1]
    }
    return null
  }

  fun consumeString(): String? {
    if (stringPointer in strings.indices) {
      stringPointer += 1
      return strings[stringPointer - 1]
    }
    return null
  }

  fun reset() {
    keystrokePointer = 0
    stringPointer = 0
  }

  fun clean() {
    keyStrokes.clear()
    strings.clear()
    keystrokePointer = 0
    stringPointer = 0
  }
}
