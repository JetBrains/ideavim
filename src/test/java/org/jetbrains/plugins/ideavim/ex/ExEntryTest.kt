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
    assertExIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-[>")
    assertFalse(options().incsearch)
    assertExIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-C>")
    assertFalse(options().incsearch)
    assertExIsDeactivated()
  }

  @Test
  fun `test complete entry`() {
    assertFalse(options().incsearch)
    typeText(":set incsearch<Enter>")
    assertTrue(options().incsearch)
    assertExIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
      }
    }

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-J>")
    assertTrue(options().incsearch)
    assertExIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().runReadAction {
      VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
    }

    assertFalse(options().incsearch)
    typeText(":set incsearch<C-M>")
    assertTrue(options().incsearch)
    assertExIsDeactivated()
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

  @Test
  fun `test saves command history when cancelling command line`() {
    typeText(":set digraph<Esc>")
    typeText(":<Up>")

    assertExText("set digraph")
  }

  @Test
  fun `test saves search history when cancelling command line`() {
    typeText("/something cool<Esc>")
    typeText("<Up>")

    assertExText("something cool")
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
}
