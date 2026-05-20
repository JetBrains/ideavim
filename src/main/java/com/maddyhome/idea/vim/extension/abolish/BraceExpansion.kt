/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

/**
 * `foo{a,b,c}bar` parsed as `prefix + {alternatives} + suffix`.
 *
 * Patterns without braces become a literal with a single empty alternative.
 * Empty braces (`anomal{}`) are a slot the other side of a substitution can
 * fill — see [materialiseWith].
 */
internal data class BracePattern(
  val prefix: String,
  val alternatives: List<String>,
  val suffix: String,
  val hasSlot: Boolean,
) {

  fun materialise(): List<String> = alternatives.map { prefix + it + suffix }

  /** Expand using a foreign list of alternatives, falling back to repeating the literal when there is no slot. */
  fun materialiseWith(foreign: List<String>): List<String> {
    if (!hasSlot) return List(foreign.size) { prefix + suffix }
    return foreign.map { prefix + it + suffix }
  }

  fun materialiseCycling(count: Int): List<String> =
    List(count) { i -> prefix + alternatives[i % alternatives.size] + suffix }
}

internal fun parseBraces(pattern: String): BracePattern {
  val openIndex = pattern.indexOf('{')
  val closeIndex = pattern.indexOf('}', startIndex = openIndex + 1)
  if (openIndex < 0 || closeIndex < 0) {
    return BracePattern(prefix = pattern, alternatives = listOf(""), suffix = "", hasSlot = false)
  }

  val prefix = pattern.substring(0, openIndex)
  val suffix = pattern.substring(closeIndex + 1)
  val rawAlternatives = pattern.substring(openIndex + 1, closeIndex)
  val alternatives = if (rawAlternatives.isEmpty()) listOf("") else rawAlternatives.split(',')
  return BracePattern(prefix, alternatives, suffix, hasSlot = true)
}
