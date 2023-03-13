/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

import javax.swing.KeyStroke

public class DigraphResult {
  public val result: Int
  public val stroke: KeyStroke?
  private var promptCharacter: Char = 0.toChar()

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

  public companion object {
    public const val RES_HANDLED: Int = 0
    public const val RES_UNHANDLED: Int = 1
    public const val RES_DONE: Int = 3
    public const val RES_BAD: Int = 4

    @JvmField
    public val HANDLED_DIGRAPH: DigraphResult = DigraphResult(RES_HANDLED, '?')

    @JvmField
    public val HANDLED_LITERAL: DigraphResult = DigraphResult(RES_HANDLED, '^')

    @JvmField
    public val UNHANDLED: DigraphResult = DigraphResult(RES_UNHANDLED)

    @JvmField
    public val BAD: DigraphResult = DigraphResult(RES_BAD)

    @JvmStatic
    public fun done(stroke: KeyStroke?): DigraphResult {
      // for some reason vim does not let to insert char 10 as a digraph, it inserts 10 instead
      return if (stroke == null || stroke.keyCode != 10) {
        DigraphResult(stroke)
      } else {
        DigraphResult(KeyStroke.getKeyStroke(0.toChar()))
      }
    }

    @JvmStatic
    public fun handled(promptCharacter: Char): DigraphResult {
      return DigraphResult(RES_HANDLED, promptCharacter)
    }
  }
}
