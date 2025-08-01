/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

interface VimDigraphGroup {
  /**
   * Returns the codepoint of a character matching the given digraph characters, or `null` if no matching digraph exists
   *
   * Note that a [Char] can only handle 16-bit Unicode characters. To support wide characters (e.g. 128388 maps to 🔴),
   * this function retuns a codepoint.
   */
  fun getCharacterForDigraph(ch1: Char, ch2: Char): Int
  fun displayAsciiInfo(editor: VimEditor)
  fun parseCommandLine(editor: VimEditor, args: String): Boolean
  fun showDigraphs(editor: VimEditor, showHeaders: Boolean)
  fun clearCustomDigraphs()
}
