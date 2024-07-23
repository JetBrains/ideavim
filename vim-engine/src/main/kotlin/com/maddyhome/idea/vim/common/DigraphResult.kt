/*
 * Copyright 2003-2023 The IdeaVim authors
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
  var promptCharacter: Char = 0.toChar()
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
    const val RES_HANDLED: Int = 0
    const val RES_UNHANDLED: Int = 1
    const val RES_DONE: Int = 3
    const val RES_BAD: Int = 4

    @JvmField
    val HANDLED_DIGRAPH: DigraphResult = DigraphResult(RES_HANDLED, '?')

    @JvmField
    val HANDLED_LITERAL: DigraphResult = DigraphResult(RES_HANDLED, '^')

    @JvmField
    val UNHANDLED: DigraphResult = DigraphResult(RES_UNHANDLED)

    @JvmField
    val BAD: DigraphResult = DigraphResult(RES_BAD)

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
