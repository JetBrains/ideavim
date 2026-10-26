/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.history.VimHistory

/**
 * The kind of command line, independent of the label shown before the text
 *
 * The label is only a prompt. An input prompt can show any text, e.g. `<` for the surround tag or `function: `, so
 * the behaviour of the command line must be decided by its type and not by its label. See `:help getcmdtype()`.
 */
sealed interface CommandLineType {
  /** The character returned by `getcmdtype()` */
  val cmdType: Char

  /** The history the entered text is saved to */
  val historyType: VimHistory.Type

  /** An Ex command line, started with `:` */
  data object Command : CommandLineType {
    override val cmdType: Char = ':'
    override val historyType: VimHistory.Type = VimHistory.Type.Command
  }

  /** A search pattern, started with `/` ([Direction.FORWARDS]) or `?` ([Direction.BACKWARDS]) */
  data class Search(val direction: Direction) : CommandLineType {
    override val cmdType: Char
      get() = if (direction == Direction.FORWARDS) '/' else '?'
    override val historyType: VimHistory.Type = VimHistory.Type.Search
  }

  /** An expression for the expression register, e.g. `i_CTRL-R_=` */
  data object Expression : CommandLineType {
    override val cmdType: Char = '='
    override val historyType: VimHistory.Type = VimHistory.Type.Expression
  }

  /** Text requested by `input()`, an extension or a modal input. The label is the prompt */
  data object Input : CommandLineType {
    override val cmdType: Char = '@'
    override val historyType: VimHistory.Type = VimHistory.Type.Input
  }

  companion object {
    /**
     * Guesses the type of the command line from its label
     *
     * Only for API that receives a label without a type, e.g. [VimCommandLineService.createSearchPrompt]. Any label
     * that isn't `:`, `/`, `?` or `=` is an input prompt.
     */
    fun fromLabel(label: String): CommandLineType = when (label) {
      ":" -> Command
      "/" -> Search(Direction.FORWARDS)
      "?" -> Search(Direction.BACKWARDS)
      "=" -> Expression
      else -> Input
    }
  }
}
