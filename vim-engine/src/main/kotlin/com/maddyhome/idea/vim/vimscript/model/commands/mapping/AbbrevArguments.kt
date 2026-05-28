/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

private const val BUFFER_MODIFIER = "<buffer>"

/**
 * The shape of an `:abbrev`-family command argument after the command name.
 *
 *   * `Listing` — empty argument, or a single token (lhs only, with no rhs).
 *   * `Definition` — `lhs SPACE rhs`, with arbitrary trailing rhs text.
 *
 * Either form may be preceded by `<buffer>`, captured in [bufferLocal].
 */
internal sealed interface AbbrevArgument {
  val bufferLocal: Boolean

  data class Listing(override val bufferLocal: Boolean) : AbbrevArgument
  data class Definition(val lhs: String, val rhs: String, override val bufferLocal: Boolean) : AbbrevArgument
}

internal fun parseAbbrevArgument(rawArgument: String): AbbrevArgument {
  val (bufferLocal, remaining) = stripBufferModifier(rawArgument.trim())
  if (remaining.isEmpty() || !remaining.contains(' ')) return AbbrevArgument.Listing(bufferLocal)
  val lhs = remaining.substringBefore(' ')
  val rhs = remaining.substringAfter(' ').trimStart()
  return AbbrevArgument.Definition(lhs, rhs, bufferLocal)
}

/** Returns (bufferLocal, rest) where rest is [argument] with a leading `<buffer>` token consumed if present. */
internal fun stripBufferModifier(argument: String): Pair<Boolean, String> {
  if (!argument.startsWith(BUFFER_MODIFIER)) return false to argument
  return true to argument.substring(BUFFER_MODIFIER.length).trimStart()
}
