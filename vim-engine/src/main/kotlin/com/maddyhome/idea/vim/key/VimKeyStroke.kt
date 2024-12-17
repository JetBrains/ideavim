/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

enum class VimModifier(val prefix: String) {
  ALT("a-"),
  CONTROL("c-"),
  CMD("d-"),
  SHIFT("s-"),
  META("m-"),
}

// TODO handle MacOS case when holding a key can produce a digraph-like key. it may require to introduce RELEASED, PRESSED state to handle properly
// TODO make all the constructors private and make some facade:
// 1. It will decide if the passed char a special key or not
// 2. It will decide if Shift + Char a combination or an uppercase letter
sealed interface VimKeyStroke : Comparable<VimKeyStroke> {
  companion object {
    fun getKeyStroke(char: Char): VimKeyStroke {
      TODO()
    }

    fun getKeyStroke(char: Char, modifiers: Set<VimModifier>): VimKeyStroke {
//  return if (modifiers == 0) {
//    VimKeyStroke.getKeyStroke(c)
//  } else if (modifiers == InputEvent.SHIFT_DOWN_MASK && Character.isLetter(c)) {
//    VimKeyStroke.getKeyStroke(Character.toUpperCase(c))
//  } else {
//    VimKeyStroke.getKeyStroke(Character.toUpperCase(c).code, modifiers)
//  }
      TODO() // how can we decide if it is a printable or a special char? same for their constructors. They shouldn't be private
    }
  }
  fun toVimNotation(): String

  /**
   * TODO better name
   * Vim uses ^V keycode instead of <C-V> however still fallbacks to `toVimNotation` when we can't represent KeyStroke in a single char
   */
  // TODO maybe name it pastable string? It's notation that we should be able to copy-paste in editor without changing the keystrokes
  fun toShortVimNotation(): String

  class Printable : VimKeyStroke { // letters and similar stuff
    val char: Char

    private constructor(char: Char) {
      this.char = char
    }

    override fun toVimNotation(): String {
      if (char == ' ') return "<Space>"
      return char.toString()
    }

    override fun toShortVimNotation(): String {
      return char.toString()
    }
  }
  // TODO can I replace it with Printable?
  class Special : VimKeyStroke { // <Esc>, <CR>, <Shift> and similar keys
    val code: Char

    private constructor(code: Char) {
      this.code = code
    }

    override fun toVimNotation(): String {
      TODO()
    }

    override fun toShortVimNotation(): String {
      TODO()
    }
  }

  class Combination : VimKeyStroke {
    val code: Char
    val modifiers: Set<VimModifier>

    constructor(code: Char, modifiers: Set<VimModifier>) {
      this.code = code
      this.modifiers = modifiers
    }

    // If we create Char + Shift it should be uppercase char, not a combination
    override fun toVimNotation(): String {
      TODO()
    }

    override fun toShortVimNotation(): String {
      TODO()
    }
  }

  // For historical reasons keystrokes without modifiers > keystrokes with modifiers
  override fun compareTo(other: VimKeyStroke): Int {
    return when {
      this is Printable && other is Printable -> this.char - other.char
      this is Printable && other is Special -> this.char - other.code
      this is Printable && other is Combination -> 1
      this is Special && other is Printable -> this.code - other.char
      this is Special && other is Special -> this.code - other.code
      this is Special && other is Combination -> 1
      this is Combination && other is Printable -> -1
      this is Combination && other is Special -> -1
      this is Combination && other is Combination -> {
        if (this.code != other.code) {
          this.code - other.code
        } else {
          val modifiers1 = this.modifiers.sorted()
          val modifiers2 = other.modifiers.sorted()
          if (modifiers1.size == modifiers2.size) {
            for (i in 0 until modifiers1.size) {
              val comparison = modifiers1[i].compareTo(modifiers2[i])
              if (comparison != 0) return comparison
            }
            0
          } else {
            other.modifiers.size - this.modifiers.size
          }
        }
      }
      else -> throw RuntimeException("unreachable")
    }
  }
}

// Won't work with emoji
/**
 * Parses string in Vim notation
 * E.g. <C-S-K> -> VimKeyStroke.Combination(K, modifiers = [CONTROL, SHIFT])
 * returns null if the passed string does not contain Vim notation
 */
fun VimKeyStroke.fromVimNotation(vimNotation: String): VimKeyStroke? {
  if (!vimNotation.startsWith('<') || !vimNotation.endsWith('>')) return null
  val lowercase = vimNotation.substring(1, vimNotation.length - 1).lowercase()
  return fromVimNotation(lowercase, mutableSetOf<VimModifier>())
}

private fun VimKeyStroke.fromVimNotation(lower: String, modifiers: MutableSet<VimModifier>): VimKeyStroke? {
  val keyCode = getVimKeyCodeByName(lower)
  val typedChar = getVimTypedKeyName(lower)
  return if (keyCode != null || typedChar != null) {
    VimKeyStroke.getKeyStroke(keyCode ?: typedChar!!)
  } else if (lower.startsWith(VimModifier.CMD.prefix)) {
    modifiers.add(VimModifier.CMD)
    fromVimNotation(lower.substring(VimModifier.CMD.prefix.length), modifiers)
  } else if (lower.startsWith(VimModifier.META.prefix)) {
    // Meta and alt prefixes are the same thing. See the key notation of vim
    modifiers.add(VimModifier.ALT)
    fromVimNotation(lower.substring(VimModifier.META.prefix.length), modifiers)
  } else if (lower.startsWith(VimModifier.ALT.prefix)) {
    modifiers.add(VimModifier.ALT)
    fromVimNotation(lower.substring(VimModifier.ALT.prefix.length), modifiers)
  } else if (lower.startsWith(VimModifier.CONTROL.prefix)) {
    modifiers.add(VimModifier.CONTROL)
    fromVimNotation(lower.substring(VimModifier.CONTROL.prefix.length), modifiers)
  } else if (lower.startsWith(VimModifier.SHIFT.prefix)) {
    modifiers.add(VimModifier.SHIFT)
    fromVimNotation(lower.substring(VimModifier.SHIFT.prefix.length), modifiers)
  } else if (lower.length == 1) {
    VimKeyStroke.getKeyStroke(lower[0], modifiers)
  } else {
    null
  }
}

@Suppress("SpellCheckingInspection")
// TODO isn't tab a typed key?
private fun getVimTypedKeyName(lower: String): Char? {
  return when (lower) {
    "space" -> ' '
    "bar" -> '|'
    "bslash" -> '\\'
    "lt" -> '<'
    else -> null
  }
}

