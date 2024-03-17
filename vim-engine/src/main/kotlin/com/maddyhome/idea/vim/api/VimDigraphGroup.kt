/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

public interface VimDigraphGroup {
  public fun getDigraph(ch1: Char, ch2: Char): Char
  public fun displayAsciiInfo(editor: VimEditor)
  public fun parseCommandLine(editor: VimEditor, args: String): Boolean
  public fun showDigraphs(editor: VimEditor)
}
