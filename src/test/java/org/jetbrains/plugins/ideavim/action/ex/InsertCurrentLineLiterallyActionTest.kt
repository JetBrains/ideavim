package org.jetbrains.plugins.ideavim.action.ex

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class InsertCurrentLineLiterallyActionTest : VimExTestCase() {
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
  fun `test insert current line literally with CTRL-R`() {
    typeText(":<C-R><C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line literally with CTRL-O`() {
    typeText(":<C-R><C-O><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test shows prompt after CTRL-R`() {
    typeText(":<C-R>")
    assertRenderedExText("\"")
  }

  @Test
  fun `test shows prompt after CTRL-R_CTRL-R`() {
    typeText(":<C-R><C-R>")
    assertRenderedExText("\"")
  }

  @Test
  fun `test shows prompt after CTRL-R_CTRL-O`() {
    typeText(":<C-R><C-O>")
    assertRenderedExText("\"")
  }

  @Test
  fun `test insert current line literally after existing text`() {
    typeText(":set <C-R><C-R><C-L>")
    assertExText("set   consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line literally before existing text`() {
    typeText(":set <Home><C-R><C-O><C-L>")
    assertExText("  consectetur adipiscing elitset ")
  }

  @Test
  fun `test insert current line literally in overstrike mode replaces text`() {
    typeText(":<Ins>set<Home><C-R><C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line literally on blank line`() {
    typeText("jj", ":<C-R><C-R><C-L>")
    assertExText("")
  }

  @Test
  fun `test insert current line literally does not apply mapping`() {
    enterCommand("cmap c q")
    typeText(":<C-R><C-R><C-L>")
    assertExText("  consectetur adipiscing elit")
  }

  @Test
  fun `test insert current line literally does not replay special characters as though typed - backspace`() {
    configureByText("Hello\bWorld${c}")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("Hello^HWorld")
  }

  @Test
  fun `test insert current line literally with backspace does not end command line`() {
    configureByText("Hello\b\b\b\b\b\biSomething else")
    typeText(":<C-R><C-R><C-L>")
    assertExIsActive()
    assertRenderedExText("Hello^H^H^H^H^H^HiSomething else")
    assertState(Mode.CMD_LINE(Mode.NORMAL()))
  }

  @Test
  fun `test insert current line literally does not replay special characters as though typed - go to start of line`() {
    configureByText("Hello\u0002 world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("Hello^B world")
  }

  @Test
  fun `test insert current line literally does not replay special characters as though typed - delete to start of line`() {
    configureByText("Hello\u0015 world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("Hello^U world")
  }

  @Test
  fun `test insert current line literally does not replay special characters as though typed - enter digraph`() {
    configureByText("Hello\u000bOK world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("Hello^KOK world")
  }

  // Blah blah blah. No need to test all control characters

  // According to the docs, any shortcut that cancels the command line is inserted literally - <C-C>, <Esc>, <CR>
  // In practice, this includes the synonyms <C-M> and <C-[>
  @VimBehaviorDiffers("hello^C world")
  @Test
  fun `test insert current line literally inserts CTRL-C literally`() {
    // The command line doesn't like `<C-V><C-C>` - it just treats it as a `<C-C>` and cancels the command line
    configureByText("hello\u0003 world")
    typeText(":<C-R><C-R><C-L>")
//    assertRenderedExText("hello^C world")
    assertRenderedExText("hello")
  }

  @Test
  fun `test insert current line literally inserts Escape literally`() {
    configureByText("hello\u001b world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert current line literally inserts CTRL-OpenBracket literally`() {
    // I don't think it's possible to represent <C-[> differently to <Esc>
    configureByText("hello\u001b world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("hello^[ world")
  }

  @Test
  fun `test insert current line literally inserts non-shortcut control character literally instead of replaying`() {
    // Vim documents c_CTRL-R as inserting text as if typed; it appears to insert non-shortcut control chars literally
    configureByText("hello\u000c world")
    typeText(":<C-R><C-R><C-L>")
    assertRenderedExText("hello^L world")
  }
}
