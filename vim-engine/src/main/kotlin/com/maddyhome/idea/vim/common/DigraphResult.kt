/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

sealed class DigraphResult(
  val codepoint: Int? = null,
  val promptCharacter: Char = 0.toChar(),
) {
  open class Handled(promptCharacter: Char) : DigraphResult(null, promptCharacter)
  data object HandledDigraph : Handled('?')
  data object HandledLiteral : Handled('^')
  class Done(codepoint: Int?) : DigraphResult(codepoint)
  data object Unhandled : DigraphResult()
  data object Bad : DigraphResult()

  companion object {
    fun done(codepoint: Int?): DigraphResult {
      // For some reason, vim does not let to insert char 10 as a digraph; it inserts 10 instead
      return Done(if (codepoint == 10) 0 else codepoint)
    }

    fun handled(promptCharacter: Char) = Handled(promptCharacter)
  }
}
