/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

/**
 * Vim's very-magic case-sensitive alternation for matching any of the variant
 * keys. Sorted longest-first so `box` cannot shadow `boxes`.
 */
internal fun buildVimAlternationPattern(keys: Set<String>): String {
  val alternation = keys.sortedByDescending(String::length).joinToString(separator = "|", transform = ::vimEscape)
  return "\\v\\C%($alternation)"
}

private val VIM_REGEX_SPECIALS = setOf(']', '[', '\\', '/', '.', '*', '+', '?', '~', '%', '(', ')', '&', '|')

private fun vimEscape(input: String): String = buildString {
  input.forEach { c ->
    if (c in VIM_REGEX_SPECIALS) append('\\')
    append(c)
  }
}
