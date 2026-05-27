/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

/**
 * The shape of an `:abbrev`-family command argument after the command name.
 *
 *   * `Listing` — empty argument, or a single token (lhs only, with no rhs).
 *   * `Definition` — `lhs SPACE rhs`, with arbitrary trailing rhs text.
 */
internal sealed interface AbbrevArgument {
  data object Listing : AbbrevArgument
  data class Definition(val lhs: String, val rhs: String) : AbbrevArgument
}

internal fun parseAbbrevArgument(rawArgument: String): AbbrevArgument {
  val trimmed = rawArgument.trim()
  if (trimmed.isEmpty() || !trimmed.contains(' ')) return AbbrevArgument.Listing
  val lhs = trimmed.substringBefore(' ')
  val rhs = trimmed.substringAfter(' ').trimStart()
  return AbbrevArgument.Definition(lhs, rhs)
}
