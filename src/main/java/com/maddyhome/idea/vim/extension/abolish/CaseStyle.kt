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

internal fun mixedcase(word: String): String {
  val camel = camelcase(word)
  return if (camel.isEmpty()) camel else camel[0].uppercaseChar() + camel.substring(1)
}

/**
 * tpope's `s:camelcase`: dashes become underscores, then either
 *  - no separator and at least one lowercase letter → only the first character
 *    is lowercased, the rest is left exactly as typed; or
 *  - otherwise → lowercase everything, but drop each `_` and uppercase the
 *    character that followed it.
 */
private fun camelcase(word: String): String {
  val normalized = word.replace('-', '_')
  if (!normalized.contains('_') && normalized.any { it.isLowerCase() }) {
    return normalized[0].lowercaseChar() + normalized.substring(1)
  }
  val result = StringBuilder(normalized.length)
  var afterUnderscore = false
  for (ch in normalized) {
    when {
      ch == '_' -> afterUnderscore = true
      afterUnderscore -> {
        result.append(ch.uppercaseChar())
        afterUnderscore = false
      }

      else -> result.append(ch.lowercaseChar())
    }
  }
  return result.toString()
}
