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
package com.maddyhome.idea.vim.common

import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class Register {
  var name: Char
  val type: SelectionType
  val keys: MutableList<KeyStroke>
  val transferableData: MutableList<out TextBlockTransferableData>
  val rawText: String?

  constructor(name: Char, type: SelectionType, keys: MutableList<KeyStroke>) {
    this.name = name
    this.type = type
    this.keys = keys
    this.transferableData = mutableListOf()
    this.rawText = text
  }

  constructor(name: Char, type: SelectionType, text: String, transferableData: MutableList<out TextBlockTransferableData>) {
    this.name = name
    this.type = type
    this.keys = StringHelper.stringToKeys(text)
    this.transferableData = transferableData
    this.rawText = text
  }

  constructor(name: Char, type: SelectionType, text: String, transferableData: MutableList<out TextBlockTransferableData>, rawText: String) {
    this.name = name
    this.type = type
    this.keys = StringHelper.stringToKeys(text)
    this.transferableData = transferableData
    this.rawText = rawText
  }

  val text: String?
    get() {
      val builder = StringBuilder()
      for (key in keys) {
        val c = key.keyChar
        if (c == KeyEvent.CHAR_UNDEFINED) {
          return null
        }
        builder.append(c)
      }
      return builder.toString()
    }

  /**
   * Append the supplied text to any existing text.
   */
  fun addTextAndResetTransferableData(text: String) {
    addKeys(StringHelper.stringToKeys(text))
    transferableData.clear()
  }

  fun addKeys(keys: List<KeyStroke>) {
    this.keys.addAll(keys)
  }

  object KeySorter : Comparator<Register> {
    @NonNls private const val ORDER = "\"0123456789abcdefghijklmnopqrstuvwxyz-*+.:%#/="

    override fun compare(o1: Register, o2: Register): Int {
      return ORDER.indexOf(o1.name.toLowerCase()) - ORDER.indexOf(o2.name.toLowerCase())
    }
  }
}
