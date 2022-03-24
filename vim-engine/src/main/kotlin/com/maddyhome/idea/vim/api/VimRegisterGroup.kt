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
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import javax.swing.KeyStroke

interface VimRegisterGroup {
  fun isValid(reg: Char): Boolean
  fun selectRegister(reg: Char): Boolean
  fun resetRegister()
  fun resetRegisters()
  fun recordKeyStroke(key: KeyStroke)
  fun isRegisterWritable(): Boolean
  fun getTransferableData(
    vimEditor: VimEditor,
    textRange: TextRange,
    text: String
  ): List<*>

  fun preprocessText(vimEditor: VimEditor, textRange: TextRange, text: String, transferableData: List<*>): String

  val currentRegister: Char
  val defaultRegister: Char
}
