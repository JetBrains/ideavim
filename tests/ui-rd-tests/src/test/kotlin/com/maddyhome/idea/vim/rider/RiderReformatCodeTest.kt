/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * ReformatCode run by IdeaVim. A separate class with its own Rider: running it from a mapping used to freeze Rider (the
 * EDT parked in `ActionManagerImpl.tryToExecute`), which would break every later test
 */
class RiderReformatCodeTest : IdeaVimRiderTestBase() {

  // VIM-3826 was reported with a mapping like this
  override fun ideaVimRcContent(): String = """
    |let mapleader = " "
    |nmap <Leader>f <Action>(ReformatCode)
  """.trimMargin()

  @AfterEach
  fun resetMode() {
    esc()
  }

  @Test
  fun `reformat code with an ex command`() {
    setText(UNFORMATTED_TEXT)

    exCommand("action ReformatCode")

    assertText(FORMATTED_TEXT)
  }

  // VIM-3826
  @Test
  fun `reformat code with an Action mapping`() {
    setText(UNFORMATTED_TEXT)

    typeVim(" f")

    assertText(FORMATTED_TEXT)
  }

  companion object {
    private val FORMATTED_TEXT = """
      |// See https://aka.ms/new-console-template for more information
      |
      |Console.WriteLine("Hello, World!");
    """.trimMargin()

    private val UNFORMATTED_TEXT = FORMATTED_TEXT.replace("Console", "    Console")
  }
}
