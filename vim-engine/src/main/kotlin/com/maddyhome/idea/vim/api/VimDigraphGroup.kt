/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

interface VimDigraphGroup {
  fun getCharacterForDigraph(ch1: Char, ch2: Char): Char
  fun displayAsciiInfo(editor: VimEditor)
  fun parseCommandLine(editor: VimEditor, args: String): Boolean
  fun showDigraphs(editor: VimEditor, showHeaders: Boolean)
}
