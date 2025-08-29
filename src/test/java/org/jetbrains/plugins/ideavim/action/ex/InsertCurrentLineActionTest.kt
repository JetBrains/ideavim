/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class InsertCurrentLineActionTest : VimExTestCase() {
  override fun configureByText() {
    configureByText("""
      |  Lorem ipsum dolor sit amet,
      |  consectetur ${c}adipiscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
  }

  @Test
  fun `test insert current line`() {
    typeText(":<C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test shows prompt after CTRL-R`() {
    typeText(":<C-R>")
    assertRenderedExText("\"")
  }

  @Test
  fun `test insert current line after existing text`() {
    typeText(":set <C-R><C-L>")
    assertExText("set   consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line before existing text`() {
    typeText(":set <Home><C-R><C-L>")
    assertExText("  consectetur adipiscing elitset ")
  }

  @Test
  fun `test insert current line in overstrike mode replaces text`() {
    typeText(":<Ins>set<Home><C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line on blank line`() {
    typeText("jj", ":<C-R><C-L>")
    assertExText("")
  }

  @Test
  fun `test insert current line replays text as if typed without applying mapping`() {
    enterCommand("cmap c q")
    typeText(":<C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line replays special characters as though typed - backspace`() {
    configureByText("Hello\bWorld${c}")
    typeText(":<C-R><C-L>")
    assertExText("HellWorld")
  }

  @Test
  fun `test insert current line with backspace can end command line and continue processing text`() {
    configureByText("Hello\b\b\b\b\b\biSomething else")
    typeText(":<C-R><C-L>")
    assertExIsDeactivated()
    assertState("Hello\b\b\b\b\b\biSomething elseSomething else")
    assertState(Mode.INSERT)
  }

  @Test
  fun `test insert current line replays special characters as though typed - go to start of line`() {
    configureByText("Hello\u0002 world")
    typeText(":<C-R><C-L>")
    assertExText(" world${c}Hello")
  }

  @Test
  fun `test insert current line replays special characters as though typed - delete to start of line`() {
    configureByText("Hello\u0015 world")
    typeText(":<C-R><C-L>")
    assertExText(" world")
  }

  @Test
  fun `test insert current line replays special characters as though typed - enter digraph`() {
    configureByText("Hello\u000bOK world")
    typeText(":<C-R><C-L>")
    assertExText("Hello✓ world")
  }

  // Blah blah blah. No need to test all control characters

  // According to the docs, any shortcut that cancels the command line is inserted literally - <C-C>, <Esc>, <CR>
  // In practice, this includes the synonyms <C-M> and <C-[>
  @VimBehaviorDiffers("hello^C world")
  @Test
  fun `test insert current line inserts CTRL-C literally`() {
    // The command line doesn't like `<C-V><C-C>` - it just treats it as a `<C-C>` and cancels the command line
    configureByText("hello\u0003 world")
    typeText(":<C-R><C-L>")
//    assertRenderedExText("hello^C world")
    assertRenderedExText("hello")
  }

  @Test
  fun `test insert current line inserts Escape literally`() {
    configureByText("hello\u001b world")
    typeText(":<C-R><C-L>")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert current line inserts CTRL-OpenBracket literally`() {
    // I don't think it's possible to represent <C-[> differently to <Esc>
    configureByText("hello\u001b world")
    typeText(":<C-R><C-L>")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert current line with embedded CR is just two lines`() {
    configureByText("hello\r world")
    typeText(":<C-R><C-L>")
    assertRenderedExText(" world")
  }

  @VimBehaviorDiffers("hello^L world")
  @Test
  fun `test insert current line inserts non-shortcut control character literally instead of replaying`() {
    // Vim documents c_CTRL-R as inserting text as if typed; it appears to insert non-shortcut control chars literally
    configureByText("hello\u000c world")
    typeText(":<C-R><C-L>")
//    assertRenderedExText("hello^L world")
    assertRenderedExText("hello world")
  }

  @Test
  fun `test inserts current line at offset of end of incsearch range`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommand("set incsearch")
    typeText("/orci ma<C-R><C-L>")
    assertExText("orci ma  Sed in orci mauris.")
  }

  @Test
  fun `test inserts current line at offset of end of incsearch range across multiple lines`() {
    configureByText("""
      |  Lorem ipsum, dolor, sit amet,
      |  consectetur adip${c}iscing elit
      |  Sed in orci mauris.
      |
      |  Cras id tellus in ex imperdiet egestas.
    """.trimMargin())
    enterCommand("set incsearch")
    typeText("/elit\\n.*ma<C-R><C-L>")
    assertExText("elit\\n.*ma  Sed in orci mauris.")
  }
}
