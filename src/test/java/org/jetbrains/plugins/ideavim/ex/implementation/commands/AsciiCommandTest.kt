/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AsciiCommandTest : VimTestCase() {
  @Test
  fun `test shows ascii value under caret`() {
    configureByText("${c}Hello world")
    enterCommand("ascii")
    assertEquals("<H>  72,  Hex 48,  Oct 110", VimPlugin.getMessage())
  }

  @Test
  fun `test show ascii for space`() {
    configureByText("$c ")
    enterCommand("ascii")
    assertEquals("< >  32,  Hex 20,  Oct 040, Digr SP", VimPlugin.getMessage())
  }

  @Test
  fun `test shows unprintable ascii code`() {
    configureByText("${c}\u0009")
    enterCommand("ascii")
    assertEquals("<^I>  9,  Hex 09,  Oct 011, Digr HT", VimPlugin.getMessage())
  }

  @Test
  fun `test shows unprintable ascii code 2`() {
    configureByText("${c}\u007f")
    enterCommand("ascii")
    assertEquals("<^?>  127,  Hex 7f,  Oct 177, Digr DT", VimPlugin.getMessage())
  }

  @Test
  fun `test shows unprintable ascii code 3`() {
    configureByText("${c}\u0006")
    enterCommand("ascii")
    assertEquals("<^F>  6,  Hex 06,  Oct 006, Digr AK", VimPlugin.getMessage())
  }

  @Test
  fun `test unicode char with 3 hex digits`() {
    configureByText("${c}œ")
    enterCommand("ascii")
    assertEquals("<œ> 339, Hex 0153, Oct 523, Digr oe", VimPlugin.getMessage())
  }

  @Test
  fun `test unicode char with 4 hex digits`() {
    configureByText("✓")
    enterCommand("ascii")
    assertEquals("<✓> 10003, Hex 2713, Oct 23423, Digr OK", VimPlugin.getMessage())
  }

  @Test
  fun `test shows correct digraph for character that can be created from multiple digraphs`() {
    configureByText("¢")  // Can be entered with <C-K>Ct and <C-K>c|, should show Ct as the first digraph
    enterCommand("ascii")
    assertEquals("<¢>  162,  Hex a2,  Oct 242, Digr Ct", VimPlugin.getMessage())
  }

  @Test
  fun `test shows custom digraph`() {
    configureByText("⓪")
    enterCommand("digraph (0 9450")
    enterCommand("ascii")
    assertEquals("<⓪> 9450, Hex 24ea, Oct 22352, Digr (0", VimPlugin.getMessage())
  }

  @Test
  fun `test shows custom digraph with 32-bit Unicode codepoint`() {
    configureByText("🔴")
    enterCommand("digraph cr 128308")
    enterCommand("ascii")
    assertEquals("<🔴> 128308, Hex 1f534, Oct 372464, Digr cr", VimPlugin.getMessage())
  }
}
