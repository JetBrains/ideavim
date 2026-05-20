/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

/** Joins atoms in a particular case convention; `recase` is the round-trip with [splitIntoAtoms]. */
internal enum class CaseStyle {
  SNAKE {
    override fun join(atoms: List<String>) = atoms.joinLowercase(separator = "_")
  },
  UPPER_SNAKE {
    override fun join(atoms: List<String>) = atoms.joinUppercase(separator = "_")
  },
  KEBAB {
    override fun join(atoms: List<String>) = atoms.joinLowercase(separator = "-")
  },
  DOT {
    override fun join(atoms: List<String>) = atoms.joinLowercase(separator = ".")
  },
  SPACE {
    override fun join(atoms: List<String>) = atoms.joinLowercase(separator = " ")
  },
  TITLE {
    override fun join(atoms: List<String>) = atoms.joinToString(separator = " ") { it.toTitleAtom() }
  },
  CAMEL {
    override fun join(atoms: List<String>): String {
      if (atoms.isEmpty()) return ""
      val head = atoms.first().lowercase()
      val tail = atoms.drop(1).joinToString(separator = "") { it.toTitleAtom() }
      return head + tail
    }
  },
  PASCAL {
    override fun join(atoms: List<String>) = atoms.joinToString(separator = "") { it.toTitleAtom() }
  },
  ;

  abstract fun join(atoms: List<String>): String

  fun recase(word: String): String = join(splitIntoAtoms(word))
}

private fun List<String>.joinLowercase(separator: String): String =
  joinToString(separator) { it.lowercase() }

private fun List<String>.joinUppercase(separator: String): String =
  joinToString(separator) { it.uppercase() }

internal fun String.toTitleAtom(): String =
  if (isEmpty()) this else this[0].uppercaseChar() + substring(1).lowercase()
