/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public class Register {
  public var name: Char
  public val type: SelectionType
  public val keys: MutableList<KeyStroke>
  public val transferableData: MutableList<out Any>
  public val rawText: String?

  public constructor(name: Char, type: SelectionType, keys: MutableList<KeyStroke>) {
    this.name = name
    this.type = type
    this.keys = keys
    this.transferableData = mutableListOf()
    this.rawText = text
  }

  public constructor(
    name: Char,
    type: SelectionType,
    text: String,
    transferableData: MutableList<out Any>,
  ) {
    this.name = name
    this.type = type
    this.keys = injector.parser.stringToKeys(text).toMutableList()
    this.transferableData = transferableData
    this.rawText = text
  }

  public constructor(
    name: Char,
    type: SelectionType,
    text: String,
    transferableData: MutableList<out Any>,
    rawText: String,
  ) {
    this.name = name
    this.type = type
    this.keys = injector.parser.stringToKeys(text).toMutableList()
    this.transferableData = transferableData
    this.rawText = rawText
  }

  public val text: String?
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
  public fun addTextAndResetTransferableData(text: String) {
    addKeys(injector.parser.stringToKeys(text))
    transferableData.clear()
  }

  public fun addKeys(keys: List<KeyStroke>) {
    this.keys.addAll(keys)
  }

  public object KeySorter : Comparator<Register> {
    @NonNls
    private const val ORDER = "\"0123456789abcdefghijklmnopqrstuvwxyz-*+.:%#/="

    override fun compare(o1: Register, o2: Register): Int {
      return ORDER.indexOf(o1.name.lowercaseChar()) - ORDER.indexOf(o2.name.lowercaseChar())
    }
  }
}
