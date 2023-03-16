/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import org.jetbrains.annotations.NonNls
import java.util.*
import javax.swing.KeyStroke

public interface VimStringParser {
  /**
   * Fake key for `<Plug>` mappings
   */
  public val plugKeyStroke: KeyStroke

  /**
   * Fake key for `<Action>` mappings
   */
  public val actionKeyStroke: KeyStroke

  /**
   * Parses Vim key notation strings.
   * @see <a href="http://vimdoc.sourceforge.net/htmldoc/intro.html#key-notation">Vim key notation</a>
   *
   * @throws java.lang.IllegalArgumentException if the mapping doesn't make sense for Vim emulation
   */
  public fun parseKeys(@NonNls string: String): List<KeyStroke>

  /**
   * Transforms string of regular and control characters (e.g. "ihello") to list of keystrokes
   */
  public fun stringToKeys(@NonNls string: String): List<KeyStroke>

  /**
   * Transforms a keystroke to a string in Vim key notation.
   * @see <a href="http://vimdoc.sourceforge.net/htmldoc/intro.html#key-notation">Vim key notation</a>
   */
  public fun toKeyNotation(keyStroke: KeyStroke): String

  /**
   * Transforms list of keystrokes to a string in Vim key notation.
   * @see <a href="http://vimdoc.sourceforge.net/htmldoc/intro.html#key-notation">Vim key notation</a>
   */
  public fun toKeyNotation(keyStrokes: List<KeyStroke>): String

  /**
   * Transforms list of keystrokes to a pastable to editor string
   *
   * e.g. "`<C-I>hello<Esc>`" -> "  hello" (<C-I> is a tab character)
   */
  // todo better name
  public fun toPrintableString(keys: List<KeyStroke>): String

  /**
   * This method is used to parse content of double-quoted strings in VimScript.
   * @see <a href="http://vimdoc.sourceforge.net/htmldoc/eval.html#expr-string">:help string</a>
   */
  public fun parseVimScriptString(string: String): String
}

public fun key(string: String): KeyStroke {
  return injector.parser.parseKeys(string).single()
}
