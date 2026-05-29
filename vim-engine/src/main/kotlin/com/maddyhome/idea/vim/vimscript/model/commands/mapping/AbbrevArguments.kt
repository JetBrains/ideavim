/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.AbbreviationEntry

private const val BUFFER_MODIFIER = "<buffer>"
private const val EXPRESSION_MODIFIER = "<expr>"

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
  data class Definition(val lhs: String, val rhs: String, override val bufferLocal: Boolean, val expression: Boolean) :
    AbbrevArgument {

    fun toEntry(
      modes: Set<MappingMode>,
      recursive: Boolean,
    ): AbbreviationEntry {
      return AbbreviationEntry(lhs, rhs, modes, recursive, expression)
    }
  }
}

internal fun parseAbbrevArgument(rawArgument: String): AbbrevArgument {
  val parsedArgument = parseArgument(rawArgument.trim())
  val remaining = parsedArgument.rest
  if (remaining.isEmpty() || !remaining.contains(' ')) return AbbrevArgument.Listing(parsedArgument.bufferLocal)
  val lhs = remaining.substringBefore(' ')
  val rhs = remaining.substringAfter(' ').trimStart()
  return AbbrevArgument.Definition(lhs, rhs, parsedArgument.bufferLocal, parsedArgument.expression)
}

internal fun parseArgument(argument: String): ParsedArgument {
  var bufferLocal = false
  var expression = false
  var rest = argument
  while (true) {
    val (matchedBuffer, afterBuffer) = stripModifier(rest, BUFFER_MODIFIER)
    val (matchedExpression, afterExpression) = stripModifier(afterBuffer, EXPRESSION_MODIFIER)
    if (!matchedBuffer && !matchedExpression) break
    bufferLocal = bufferLocal || matchedBuffer
    expression = expression || matchedExpression
    rest = afterExpression
  }
  return ParsedArgument(bufferLocal, expression, rest)
}

internal fun stripModifier(argument: String, modifier: String): Pair<Boolean, String> {
  if (!argument.startsWith(modifier)) return false to argument
  return true to argument.substring(modifier.length).trimStart()
}

internal data class ParsedArgument(
  val bufferLocal: Boolean,
  val expression: Boolean,
  val rest: String,
)