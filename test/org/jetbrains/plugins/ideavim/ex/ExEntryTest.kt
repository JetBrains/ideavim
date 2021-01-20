/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.ui.ex.ExDocument
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ExEntryTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    configureByText("\n")
  }

  fun `test cancel entry`() {
    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<Esc>")
    assertFalse(OptionsManager.incsearch.isSet)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<C-[>")
    assertFalse(OptionsManager.incsearch.isSet)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<C-C>")
    assertFalse(OptionsManager.incsearch.isSet)
    assertIsDeactivated()
  }

  fun `test complete entry`() {
    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<Enter>")
    assertTrue(OptionsManager.incsearch.isSet)
    assertIsDeactivated()

    deactivateExEntry()
    OptionsManager.resetAllOptions()

    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<C-J>")
    assertTrue(OptionsManager.incsearch.isSet)
    assertIsDeactivated()

    deactivateExEntry()
    OptionsManager.resetAllOptions()

    assertFalse(OptionsManager.incsearch.isSet)
    typeExInput(":set incsearch<C-M>")
    assertTrue(OptionsManager.incsearch.isSet)
    assertIsDeactivated()
  }

  fun `test caret shape`() {
    // Show block at end of input (normal)
    // Show vertical bar in insert mode
    // Show horizontal bar in replace mode
    typeExInput(":")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    typeText("set")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home>")
    assertEquals("VER 25", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home><Insert>")
    assertEquals("HOR 20", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home><Insert><Insert>")
    assertEquals("VER 25", exEntryPanel.entry.caretShape)
  }

  fun `test move caret to beginning of line`() {
    typeExInput(":set incsearch<C-B>")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set incsearch<Home>")
    assertExOffset(0)
  }

  fun `test move caret to end of line`() {
    typeExInput(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<C-E>")
    assertExOffset(13)

    deactivateExEntry()
    typeExInput(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<End>")
    assertExOffset(13)
  }

  fun `test backspace deletes character in front of caret`() {
    typeExInput(":set incsearch<BS>")
    assertExText("set incsearc")

    typeText("<C-H>")
    assertExText("set incsear")
  }

  fun `test backspace character in front of caret cancels entry`() {
    typeExInput(":<BS>")
    assertIsDeactivated()

    deactivateExEntry()

    typeExInput(":set<BS><BS><BS><BS>")
    assertIsDeactivated()

    deactivateExEntry()

    typeExInput(":<C-H>")
    assertIsDeactivated()

    deactivateExEntry()

    // Don't deactivate if there is still text to the right of the caret
    typeExInput(":set<C-B>")
    assertExOffset(0)
    typeText("<BS>")
    assertIsActive()
  }

  fun `test delete deletes character under caret`() {
    typeExInput(":set<Left>")
    typeText("<Del>")
    assertExText("se")
  }

  fun `test delete at end of string deletes character to left of caret`() {
    typeExInput(":set")
    typeText("<Del>")
    assertExText("se")
  }

  fun `test delete with no text cancels entry`() {
    typeExInput(":set")
    typeText("<Del><Del><Del><Del>")
    assertIsDeactivated()
  }

  fun `test delete word before caret`() {
    typeExInput(":set incsearch<C-W>")
    assertExText("set ")

    deactivateExEntry()

    typeExInput(":set incsearch<Left><Left><Left>")
    typeText("<C-W>")
    assertExText("set rch")
  }

  fun `test delete to start of line`() {
    typeExInput(":set incsearch<C-U>")
    assertExText("")

    deactivateExEntry()

    typeExInput(":set incsearch<Left><Left><Left><C-U>")
    assertExText("rch")
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  fun `test command history`() {
    typeExInput(":set digraph<CR>")
    typeExInput(":digraph<CR>")
    typeExInput(":set incsearch<CR>")

    typeExInput(":<Up>")
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

    typeExInput(":<S-Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("digraph")
    typeText("<Up>")
    assertExText("set digraph")

    deactivateExEntry()

    typeExInput(":<PageUp>")
    assertExText("set incsearch")
    typeText("<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
  }

  fun `test matching command history`() {
    typeExInput(":set digraph<CR>")
    typeExInput(":digraph<CR>")
    typeExInput(":set incsearch<CR>")

    typeExInput(":set<Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("set digraph")

    deactivateExEntry()

    typeExInput(":set<S-Up>")
    assertExText("set incsearch")
    typeText("<S-Up>")
    assertExText("digraph")
    typeText("<S-Up>")
    assertExText("set digraph")

    deactivateExEntry()

    typeExInput(":set<PageUp>")
    assertExText("set incsearch")
    typeText("<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
  }

  fun `test search history`() {
    typeExInput("/something cool<CR>")
    typeExInput("/not cool<CR>")
    typeExInput("/so cool<CR>")

    typeExInput("/<Up>")
    assertExText("so cool")
    typeText("<Up>")
    assertExText("not cool")
    typeText("<Up>")
    assertExText("something cool")

    deactivateExEntry()

    typeExInput("/<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")
    typeText("<S-Up>")
    assertExText("something cool")

    deactivateExEntry()

    typeExInput("/<PageUp>")
    assertExText("so cool")
    typeText("<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  fun `test matching search history`() {
    typeExInput("/something cool<CR>")
    typeExInput("/not cool<CR>")
    typeExInput("/so cool<CR>")

    typeExInput("/so<Up>")
    assertExText("so cool")
    typeText("<Up>")
    assertExText("something cool")

    deactivateExEntry()

    // TODO: Vim behavior reorders the history even when cancelling history
//        typeExInput(":<Up>")
//        assertEquals("set digraph", exEntryPanel.text)
//        typeText("<Up>")
//        assertEquals("set incsearch", exEntryPanel.text)

    typeExInput("/so<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")
    typeText("<S-Up>")
    assertExText("something cool")

    deactivateExEntry()

    typeExInput("/so<PageUp>")
    assertExText("so cool")
    typeText("<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
  }

  fun `test toggle insert replace`() {
    val exDocument = exEntryPanel.entry.document as ExDocument
    assertFalse(exDocument.isOverwrite)
    typeExInput(":set<C-B>digraph")
    assertExText("digraphset")

    deactivateExEntry()

    typeExInput(":set<C-B><Insert>digraph")
    assertTrue(exDocument.isOverwrite)
    assertExText("digraph")

    typeText("<Insert><C-B>set ")
    assertFalse(exDocument.isOverwrite)
    assertExText("set digraph")
  }

  fun `test move caret one WORD left`() {
    typeExInput(":set incsearch<S-Left>")
    assertExOffset(4)
    typeText("<S-Left>")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set incsearch<C-Left>")
    assertExOffset(4)
    typeText("<C-Left>")
    assertExOffset(0)
  }

  @VimBehaviorDiffers(description = "Moving one word right positions caret at end of previous word")
  fun `test move caret one WORD right`() {
    typeExInput(":set incsearch")
    caret.dot = 0
    typeText("<S-Right>")
    // TODO: Vim moves caret to "set| ", while we move it to "set |"
    assertExOffset(4)

    typeText("<S-Right>")
    assertExOffset(13)

    caret.dot = 0
    typeText("<C-Right>")
    // TODO: Vim moves caret to "set| ", while we move it to "set |"
    assertExOffset(4)

    typeText("<C-Right>")
    assertExOffset(13)
  }

  fun `test insert digraph`() {
    typeExInput(":<C-K>OK")
    assertExText("✓")
    assertExOffset(1)

    deactivateExEntry()

    typeExInput(":set<Home><C-K>OK")
    assertExText("✓set")
    assertExOffset(1)

    deactivateExEntry()

    typeExInput(":set<Home><Insert><C-K>OK")
    assertExText("✓et")
    assertExOffset(1)
  }

  fun `test prompt while inserting digraph`() {
    typeExInput(":<C-K>")
    assertExText("?")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":<C-K>O")
    assertExText("O")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set<Home><C-K>")
    assertExText("?set")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set<Home><C-K>O")
    assertExText("Oset")
    assertExOffset(0)
  }

  fun `test escape cancels digraph`() {
    typeExInput(":<C-K><Esc>OK")
    assertIsActive()
    assertExText("OK")

    deactivateExEntry()

    // Note that the docs state that hitting escape stops digraph entry and cancels command line mode. In practice,
    // this isn't true - digraph entry is stopped, but command line mode continues
    typeExInput(":<C-K>O<Esc>K")
    assertIsActive()
    assertEquals("K", exEntryPanel.text)

    deactivateExEntry()
  }

  // TODO: Test inserting control characters, if/when supported

  fun `test insert literal character`() {
    typeExInput(":<C-V>123<C-V>080")
    assertExText("{P")

    deactivateExEntry()

    typeExInput(":<C-V>o123")
    assertExText("S")

    deactivateExEntry()

    typeExInput(":<C-V>u00A9")
    assertExText("©")

    deactivateExEntry()

    typeExInput(":<C-Q>123<C-Q>080")
    assertExText("{P")

    deactivateExEntry()

    typeExInput(":<C-Q>o123")
    assertExText("S")

    deactivateExEntry()

    typeExInput(":<C-Q>u00a9")
    assertExText("©")

    deactivateExEntry()

    typeExInput(":set<Home><C-V>u00A9")
    assertExText("©set")
    assertExOffset(1)
  }

  fun `test prompt while inserting literal character`() {
    typeExInput(":<C-V>")
    assertExText("^")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":<C-V>o")
    assertExText("^")
    assertExOffset(0)

    typeText("1")
    assertExText("^")
    assertExOffset(0)

    typeText("2")
    assertExText("^")
    assertExOffset(0)

    typeText("3")
    assertExText("S")
    assertExOffset(1)
  }

  fun `test insert register`() {
    VimPlugin.getRegister().setKeys('c', StringHelper.parseKeys("hello world"))
    VimPlugin.getRegister().setKeys('5', StringHelper.parseKeys("greetings programs"))

    typeExInput(":<C-R>c")
    assertExText("hello world")

    deactivateExEntry()

    typeExInput(":<C-R>5")
    assertExText("greetings programs")

    deactivateExEntry()

    typeExInput(":set<Home><C-R>c")
    assertExText("hello worldset")
    assertExOffset(11)  // Just before 'set'

    // TODO: Test caret feedback
    // Vim shows " after hitting <C-R>
  }

  fun `test insert multi-line register`() {
    // parseKeys parses <CR> in a way that Register#getText doesn't like
    val keys = mutableListOf<KeyStroke>()
    keys.addAll(StringHelper.parseKeys("hello"))
    keys.add(KeyStroke.getKeyStroke('\n'))
    keys.addAll(StringHelper.parseKeys("world"))
    VimPlugin.getRegister().setKeys('c', keys)

    typeExInput(":<C-R>c")
    assertExText("hello world")
  }

  // TODO: Test other special registers, if/when supported
  // E.g. '.' '%' '#', etc.

  fun `test insert last command`() {
    typeExInput(":set incsearch<CR>")
    typeExInput(":<C-R>:")
    assertExText("set incsearch")
  }

  fun `test insert last search command`() {
    typeExInput("/hello<CR>")
    typeExInput(":<C-R>/")
    assertExText("hello")
  }

  fun `test cmap`() {
    typeExInput(":cmap x z<CR>")
    typeExInput(":cnoremap w z<CR>")
    typeExInput(":cmap z y<CR>")
    typeExInput(":z")
    assertExText("y")
    deactivateExEntry()

    typeExInput(":x")
    assertExText("y")
    deactivateExEntry()

    typeExInput(":w")
    assertExText("z")
  }

  fun `test cmap Ctrl`() {
    typeExInput(":cmap \\<C-B> b<CR>")
    typeExInput(":<C-B>")
    assertExText("b")
    deactivateExEntry()

    VimPlugin.getRegister().setKeys('e', StringHelper.parseKeys("hello world"))
    typeExInput(":cmap d \\<C-R><CR>")
    typeExInput(":de")
    assertExText("hello world")
  }

  private fun typeExInput(text: String) {
    assertTrue("Ex command must start with ':', '/' or '?'",
      text.startsWith(":") || text.startsWith('/') || text.startsWith('?'))

    val keys = mutableListOf<KeyStroke>()
    StringHelper.parseKeys(text).forEach {
      // <Left> doesn't work correctly in tests. The DefaultEditorKit.NextVisualPositionAction action is correctly
      // called, but fails to move the caret correctly because the text component has never been painted
      if (it.keyCode == KeyEvent.VK_LEFT && it.modifiers == 0) {
        if (keys.count() > 0) {
          typeText(keys)
          keys.clear()
        }

        exEntryPanel.entry.caret.dot--
      } else {
        keys.add(it)
      }
    }
    if (keys.count() > 0)
      typeText(keys)
  }

  private fun typeText(text: String) {
    typeText(StringHelper.parseKeys(text))
  }

  private fun deactivateExEntry() {
    // We don't need to reset text, that's handled by #active
    if (exEntryPanel.isActive)
      typeText("<C-C>")
  }

  @Suppress("DEPRECATION")
  private fun assertExText(expected: String) {
    // Get the text directly from the text field. This will include any "prompt" chars for e.g. digraphs
    assertEquals(expected, exEntryPanel.entry.text)
  }

  private fun assertIsActive() {
    assertTrue(exEntryPanel.isActive)
  }

  private fun assertIsDeactivated() {
    assertFalse(exEntryPanel.isActive)
  }

  private fun assertExOffset(expected: Int) {
    assertEquals(expected, caret.dot)
  }

  private val exEntryPanel
    get() = ExEntryPanel.getInstance()

  private val caret
    get() = exEntryPanel.entry.caret
}
