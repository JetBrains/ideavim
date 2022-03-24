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
package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.SelectionType
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
    text: String,
  ): List<*>

  fun preprocessText(vimEditor: VimEditor, textRange: TextRange, text: String, transferableData: List<*>): String

  /** Store text into the last register. */
  fun storeText(
    editor: VimEditor,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean

  /**
   * Stores text, character wise, in the given special register
   *
   *
   * This method is intended to support writing to registers when the text cannot be yanked from an editor. This is
   * expected to only be used to update the search and command registers. It will not update named registers.
   *
   *
   * While this method allows setting the unnamed register, this should only be done from tests, and only when it's
   * not possible to yank or cut from the fixture editor. This method will skip additional text processing, and won't
   * update other registers such as the small delete register or reorder the numbered registers. It is much more
   * preferable to yank from the fixture editor.
   */
  fun storeTextSpecial(register: Char, text: String): Boolean
  fun getRegister(r: Char): Register?
  fun getRegisters(): List<Register>
  fun saveRegister(r: Char, register: Register)
  fun startRecording(editor: VimEditor, register: Char): Boolean

  fun getPlaybackRegister(r: Char): Register?
  fun recordText(text: String)
  fun setKeys(register: Char, keys: List<KeyStroke>)
  fun setKeys(register: Char, keys: List<KeyStroke>, type: SelectionType)
  fun finishRecording(editor: VimEditor)

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  val lastRegister: Register?
  val currentRegister: Char
  val defaultRegister: Char
}
