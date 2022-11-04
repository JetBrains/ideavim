/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
