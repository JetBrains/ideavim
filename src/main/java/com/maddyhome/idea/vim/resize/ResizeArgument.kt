/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.resize

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.InvalidCommandException

/**
 * A parsed `:resize` argument. See [ResizeCommand] for the textual forms each case maps to.
 */
internal sealed interface ResizeArgument {

  /** `:resize` with no argument - maximise the window height. */
  object Maximize : ResizeArgument

  /** `:resize {n}` - set the height to an absolute number of rows. */
  data class Absolute(val rows: Int) : ResizeArgument

  /** `:resize +{n}` / `:resize -{n}` - change the height by a signed number of rows. */
  data class Relative(val rows: Int) : ResizeArgument

  companion object {
    @Throws(ExException::class)
    fun parse(argument: String): ResizeArgument {
      val trimmed = argument.trim()
      if (trimmed.isEmpty()) return Maximize

      val rows = trimmed.removePrefix("+").toIntOrNull()
        ?: throw InvalidCommandException("E488: Trailing characters: $trimmed", null)

      val relative = trimmed.startsWith("+") || trimmed.startsWith("-")
      return if (relative) Relative(rows) else Absolute(rows)
    }
  }
}
