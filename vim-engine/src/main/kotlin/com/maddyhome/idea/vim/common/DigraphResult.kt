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
package com.maddyhome.idea.vim.common

import javax.swing.KeyStroke

class DigraphResult {
  val result: Int
  val stroke: KeyStroke?
  var promptCharacter = 0.toChar()
    private set

  private constructor(result: Int) {
    this.result = result
    stroke = null
  }

  private constructor(result: Int, promptCharacter: Char) {
    this.result = result
    this.promptCharacter = promptCharacter
    stroke = null
  }

  private constructor(stroke: KeyStroke?) {
    result = RES_DONE
    this.stroke = stroke
  }

  companion object {
    const val RES_HANDLED = 0
    const val RES_UNHANDLED = 1
    const val RES_DONE = 3
    const val RES_BAD = 4

    @JvmField
    val HANDLED_DIGRAPH = DigraphResult(RES_HANDLED, '?')

    @JvmField
    val HANDLED_LITERAL = DigraphResult(RES_HANDLED, '^')

    @JvmField
    val UNHANDLED = DigraphResult(RES_UNHANDLED)

    @JvmField
    val BAD = DigraphResult(RES_BAD)

    @JvmStatic
    fun done(stroke: KeyStroke?): DigraphResult {
      // for some reason vim does not let to insert char 10 as a digraph, it inserts 10 instead
      return if (stroke == null || stroke.keyCode != 10) {
        DigraphResult(stroke)
      } else {
        DigraphResult(KeyStroke.getKeyStroke(0.toChar()))
      }
    }

    @JvmStatic
    fun handled(promptCharacter: Char): DigraphResult {
      return DigraphResult(RES_HANDLED, promptCharacter)
    }
  }
}
