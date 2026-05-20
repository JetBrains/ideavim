/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

/**
 * Splits a word into case-style-agnostic atoms so it can be re-joined in any
 * other style. Boundaries: the separators `_ - . space`, the lower→upper
 * transition (`fooBar`), and the end of an acronym (`HTTPRequest`).
 */
internal fun splitIntoAtoms(word: String): List<String> {
  if (word.isEmpty()) return emptyList()

  val atoms = mutableListOf<String>()
  val current = StringBuilder()

  fun flush() {
    if (current.isNotEmpty()) {
      atoms += current.toString()
      current.clear()
    }
  }

  for (index in word.indices) {
    val ch = word[index]
    if (isAtomSeparator(ch)) {
      flush()
      continue
    }
    if (isAtomBoundary(word, index)) {
      flush()
    }
    current.append(ch)
  }
  flush()
  return atoms
}

private fun isAtomSeparator(ch: Char): Boolean = ch == '_' || ch == '-' || ch == '.' || ch == ' '

private fun isAtomBoundary(word: String, index: Int): Boolean {
  if (index == 0) return false
  val prev = word[index - 1]
  val curr = word[index]

  if (prev.isLowerCase() && curr.isUpperCase()) return true

  // Acronym end ("HTTP|Request"): a one-char lookahead distinguishes the end
  // of a run of caps from the middle of one.
  val next = word.getOrNull(index + 1)
  if (prev.isUpperCase() && curr.isUpperCase() && next != null && next.isLowerCase()) return true

  return false
}
