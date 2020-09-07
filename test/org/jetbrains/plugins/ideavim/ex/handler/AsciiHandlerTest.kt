package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase

class AsciiHandlerTest : VimTestCase() {
  fun `test shows ascii value under caret`() {
    configureByText("${c}Hello world")
    enterCommand("ascii")
    assertEquals("<H>  72,  Hex 48,  Oct 110", VimPlugin.getMessage())
  }

  fun `test show ascii for space`() {
    configureByText("${c} ")
    enterCommand("ascii")
    assertEquals("< >  32,  Hex 20,  Oct 040, Digr SP", VimPlugin.getMessage())
  }

  fun `test shows unprintable ascii code`() {
    configureByText("${c}\u0009")
    enterCommand("ascii")
    assertEquals("<^I>  9,  Hex 09,  Oct 011, Digr HT", VimPlugin.getMessage())
  }

  fun `test shows unprintable ascii code 2`() {
    configureByText("${c}\u007f")
    enterCommand("ascii")
    assertEquals("<^?>  127,  Hex 7f,  Oct 177, Digr DT", VimPlugin.getMessage())
  }

  fun `test shows unprintable ascii code 3`() {
    configureByText("${c}\u0006")
    enterCommand("ascii")
    assertEquals("<^F>  6,  Hex 06,  Oct 006, Digr AK", VimPlugin.getMessage())
  }

  fun `test unicode char with 3 hex digits`() {
    configureByText("${c}œ")
    enterCommand("ascii")
    assertEquals("<œ> 339, Hex 0153, Oct 523, Digr oe", VimPlugin.getMessage())
  }

  fun `test unicode char with 4 hex digits`() {
    configureByText("✓")
    enterCommand("ascii")
    assertEquals("<✓> 10003, Hex 2713, Oct 23423, Digr OK", VimPlugin.getMessage())
  }
}
