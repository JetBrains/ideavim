/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

import javax.swing.KeyStroke

sealed class DigraphResult(
  val stroke: KeyStroke? = null,
  val promptCharacter: Char = 0.toChar(),
) {
  open class Handled(promptCharacter: Char) : DigraphResult(null, promptCharacter)
  data object HandledDigraph : Handled('?')
  data object HandledLiteral : Handled('^')
  class Done(stroke: KeyStroke?) : DigraphResult(stroke)
  data object Unhandled : DigraphResult()
  data object Bad : DigraphResult()

  companion object {
    fun done(stroke: KeyStroke?): DigraphResult {
      // For some reason, vim does not let to insert char 10 as a digraph; it inserts 10 instead
      return if (stroke == null || stroke.keyCode != 10) {
        Done(stroke)
      } else {
        Done(KeyStroke.getKeyStroke(0.toChar()))
      }
    }

    fun handled(promptCharacter: Char) = Handled(promptCharacter)
  }
}
