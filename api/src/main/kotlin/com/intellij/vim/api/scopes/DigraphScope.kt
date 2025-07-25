/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope for functions that provide working with digraphs.
 */
@VimPluginDsl
interface DigraphScope {
  /**
   * Gets the character for a digraph.
   *
   * In Vim, this is equivalent to entering CTRL-K followed by the two characters in insert mode.
   * Example: CTRL-K a: produces 'ä'
   *
   * @param ch1 The first character of the digraph
   * @param ch2 The second character of the digraph
   * @return The Unicode codepoint of the character represented by the digraph, or the codepoint of ch2 if no digraph is found
   */
  fun getCharacter(ch1: Char, ch2: Char): Int

  /**
   * Adds a custom digraph.
   *
   * In Vim, this is equivalent to the `:digraph` command with arguments.
   * Example: `:digraph a: 228` adds a digraph where 'a:' produces 'ä'
   *
   * @param ch1 The first character of the digraph
   * @param ch2 The second character of the digraph
   * @param codepoint The Unicode codepoint of the character to associate with the digraph
   */
  fun add(ch1: Char, ch2: Char, codepoint: Int)

  /**
   * Clears all custom digraphs.
   *
   * Note: Vim does not provide a built-in command to clear custom digraphs.
   */
  fun clearCustomDigraphs()
}