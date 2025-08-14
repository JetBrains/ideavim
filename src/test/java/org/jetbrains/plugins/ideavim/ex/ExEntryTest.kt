/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// TODO: Split this class
// This class should handle simple ex entry features, such as starting ex entry, accepting/cancelling, cursor shape etc.
// Individual actions such as c_CTRL-B or c_CTRL-E (beginning/end of line), c_CTRL-R (insert register), insert digraph
// or literal, etc. should have individual test classes in the ideavim.ex.action package
// :cmap should also be tested separately

class ExEntryTest : VimExTestCase() {
  @Test
  fun `test initial text set to empty string`() {
    typeText(":")
    assertExText("")
  }

  @Test
  fun `test initial text set to current line range with count of 1`() {
    typeText("1:")
    assertExText(".")
  }

  @Test
  fun `test initial text set to current line with offset for count greater than 1`() {
    typeText("10:")
    assertExText(".,.+9")
  }

  @Test
  fun `test initial text set to visual marks when invoked in Visual mode`() {
    configureByText("lorem ipsum\nlorem ipsum")
    typeText("V", ":")
    assertExText("'<,'>")
  }

  @Test
  fun `test cancel entry`() {
    assertFalse(options().incsearch)
    typeText(":set incsearch<Esc>")
    assertFalse(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-[>")
    assertFalse(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-C>")
    assertFalse(options().incsearch)
    assertIsDeactivated()
  }

  @Test
  fun `test complete entry`() {
    assertFalse(options().incsearch)
    typeText(":set incsearch<Enter>")
    assertTrue(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
      }
    }

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-J>")
    assertTrue(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().runReadAction {
      VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
    }

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-M>")
    assertTrue(options().incsearch)
    assertIsDeactivated()
  }

  @Test
  fun `test ex entry clears status line`() {
    enterSearch("lorem")
    assertStatusLineMessageContains("Pattern not found: lorem")
    typeText(":")
    assertStatusLineCleared()
  }

  @Test
  fun `test caret shape`() {
    // Show block at end of input (normal)
    // Show vertical bar in insert mode
    // Show horizontal bar in replace mode
    typeText(":")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    typeText("set")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home>")
    assertEquals("VER 25", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home><Insert>")
    assertEquals("HOR 20", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home><Insert><Insert>")
    assertEquals("VER 25", exEntryPanel.entry.caretShape)
  }

  @Test
  fun `test caret shape comes from guicursor`() {
    enterCommand("set guicursor=c:ver50,ci:hor75,cr:block")

    typeText(":")
    assertEquals("VER 50", exEntryPanel.entry.caretShape)

    typeText("set")
    assertEquals("VER 50", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home>")
    assertEquals("HOR 75", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home><Insert>")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeText(":set<Home><Insert><Insert>")
    assertEquals("HOR 75", exEntryPanel.entry.caretShape)
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  @Test
  fun `test command history`() {
    typeText(":set digraph<CR>")
    typeText(":digraph<CR>")
    typeText(":set incsearch<CR>")

    typeText(":<Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("digraph")
    typeText("<Up>")
    assertExText("set digraph")

    deactivateExEntry()

    // TODO: Vim behavior reorders the history even when cancelling history
//        typeExInput(":<Up>")
//        assertExText("set digraph")
//        typeText("<Up>")
//        assertExText("set incsearch")

    typeText(":<S-Up>")
    assertExText("set digraph")
    typeText("<Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("digraph")

    deactivateExEntry()

    typeText(":<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
    typeText("<PageUp>")
    assertExText("set incsearch")
  }

  @TestWithoutNeovim(SkipNeovimReason.CMD)
  @Test
  fun `test matching command history`() {
    typeText(":set digraph<CR>")
    typeText(":digraph<CR>")
    typeText(":set incsearch<CR>")

    typeText(":set<Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("set digraph")

    deactivateExEntry()

    typeText(":set<S-Up>")
    assertExText("set digraph")
    typeText("<S-Up>")
    assertExText("set incsearch")
    typeText("<S-Up>")
    assertExText("digraph")

    deactivateExEntry()

    typeText(":set<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
    typeText("<PageUp>")
    assertExText("set incsearch")
  }

  @Test
  fun `test search history`() {
    typeText("/something cool<CR>")
    typeText("/not cool<CR>")
    typeText("/so cool<CR>")

    typeText("/<Up>")
    assertExText("so cool")
    typeText("<Up>")
    assertExText("not cool")
    typeText("<Up>")
    assertExText("something cool")

    deactivateExEntry()

    typeText("/<S-Up>")
    assertExText("something cool")
    typeText("<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")

    deactivateExEntry()

    typeText("/<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
    typeText("<PageUp>")
    assertExText("so cool")
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  @Test
  fun `test matching search history`() {
    typeText("/something cool<CR>")
    typeText("/not cool<CR>")
    typeText("/so cool<CR>")

    typeText("/so<Up>")
    assertExText("so cool")
    typeText("<Up>")
    assertExText("something cool")

    deactivateExEntry()

    // TODO: Vim behavior reorders the history even when cancelling history
//        typeExInput(":<Up>")
//        assertEquals("set digraph", exEntryPanel.text)
//        typeText("<Up>")
//        assertEquals("set incsearch", exEntryPanel.text)

    typeText("/so<S-Up>")
    assertExText("something cool")
    typeText("<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")

    deactivateExEntry()

    typeText("/so<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
    typeText("<PageUp>")
    assertExText("so cool")
  }


  @Test
  fun `test insert digraph`() {
    typeText(":<C-K>OK")
    assertExText("✓")
    assertExOffset(1)

    deactivateExEntry()

    typeText(":set<Home><C-K>OK")
    assertExText("✓set")
    assertExOffset(1)

    deactivateExEntry()

    typeText(":set<Home><Insert><C-K>OK")
    assertExText("✓et")
    assertExOffset(1)
  }

  @Test
  fun `test prompt while inserting digraph`() {
    typeText(":<C-K>")
    assertRenderedExText("?")
    assertExOffset(0)

    deactivateExEntry()

    typeText(":<C-K>O")
    assertRenderedExText("O")
    assertExOffset(0)

    deactivateExEntry()

    typeText(":set<Home><C-K>")
    assertRenderedExText("?set")
    assertExOffset(0)

    deactivateExEntry()

    typeText(":set<Home><C-K>O")
    assertRenderedExText("Oset")
    assertExOffset(0)
  }

  @Test
  fun `test escape cancels digraph`() {
    typeText(":<C-K><Esc>OK")
    assertIsActive()
    assertExText("OK")

    deactivateExEntry()

    // Note that the docs state that hitting escape stops digraph entry and cancels command line mode. In practice,
    // this isn't true - digraph entry is stopped, but command line mode continues
    typeText(":<C-K>O<Esc>K")
    assertIsActive()
    assertRenderedExText("K")

    deactivateExEntry()
  }

  @Test
  fun `test insert literal character`() {
    typeText(":<C-V>123<C-V>080")
    assertExText("{P")

    deactivateExEntry()

    typeText(":<C-V>o123")
    assertExText("S")

    deactivateExEntry()

    typeText(":<C-V>u00A9")
    assertExText("©")

    deactivateExEntry()

    typeText(":<C-Q>123<C-Q>080")
    assertExText("{P")

    deactivateExEntry()

    typeText(":<C-Q>o123")
    assertExText("S")

    deactivateExEntry()

    typeText(":<C-Q>u00a9")
    assertExText("©")

    deactivateExEntry()

    typeText(":set<Home><C-V>u00A9")
    assertExText("©set")
    assertExOffset(1)
  }

  @Test
  fun `test insert literal control characters`() {
    typeText(":normal I[<C-V><Esc>A]<C-V><Esc>")
    assertExText("normal I[" + Char(27) + "A]" + Char(27))

    deactivateExEntry()

    // CR should be \n but Vim treats that as a NULL char, so we insert \r
    typeText(":nmap p <C-V><CR>")
    assertExText("nmap p \r")

    deactivateExEntry()

    typeText(":nmap p <C-V><C-D>")
    assertExText("nmap p " + Char(4))

    deactivateExEntry()

    typeText(":nmap p <C-V><C-I>")
    assertExText("nmap p \t")

    deactivateExEntry()

    typeText(":nmap p <C-V><C-V>")
    assertExText("nmap p " + Char(22))

    // TODO: IdeaVim handles <C-C> before handling digraphs/literals
//    typeText(":nmap p <C-V><C-C>")
//    assertExText("nmap p " + Char(3))
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test prompt while inserting literal character`() {
    typeText(":<C-V>")
    assertRenderedExText("^")
    assertExOffset(0)

    deactivateExEntry()

    typeText(":<C-V>o")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("1")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("2")
    assertRenderedExText("^")
    assertExOffset(0)

    typeText("3")
    assertRenderedExText("S")
    assertExOffset(1)
  }

  @Test
  fun `test cmap`() {
    typeText(":cmap x z<CR>")
    typeText(":cnoremap w z<CR>")
    typeText(":cmap z y<CR>")
    typeText(":z")
    assertExText("y")
    deactivateExEntry()

    typeText(":x")
    assertExText("y")
    deactivateExEntry()

    typeText(":w")
    assertExText("z")
  }

  @Test
  fun `test cmap Ctrl`() {
    typeText(injector.parser.stringToKeys(":cmap <C-B> b") + injector.parser.parseKeys("<CR>"))
    typeText(":<C-B>")
    assertExText("b")
    deactivateExEntry()

    VimPlugin.getRegister().setKeys('e', injector.parser.parseKeys("hello world"))
    typeText(injector.parser.stringToKeys(":cmap d <C-R>") + injector.parser.parseKeys("<CR>"))
    typeText(":de")
    assertExText("hello world")
  }

  @Test
  fun `test renders control characters`() {
    configureByText("")
    typeText(":normal I<C-V><Esc>A<C-V><Esc>")
    assertRenderedExText("normal I^[A^[")
  }

  @Test
  fun `test renders control characters as unicode number`() {
    configureByText("")
    typeText(":echo <C-V>x80")
    assertRenderedExText("echo <80>")
  }

  private fun assertIsActive() {
    assertExIsActive()
  }

  private fun assertIsDeactivated() {
    assertExIsDeactivated()
  }
}
