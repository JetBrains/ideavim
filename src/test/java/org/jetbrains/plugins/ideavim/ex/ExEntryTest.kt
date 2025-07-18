/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.ex.ExDocument
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

// TODO: Split this class
// This class should handle simple ex entry features, such as starting ex entry, accepting/cancelling, cursor shape etc.
// Individual actions such as c_CTRL-B or c_CTRL-E (beginning/end of line), c_CTRL-R (insert register), insert digraph
// or literal, etc. should have individual test classes in the ideavim.ex.action package
// :cmap should also be tested separately

@Suppress("SpellCheckingInspection")
class ExEntryTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test initial text set to empty string`() {
    typeExInput(":")
    assertExText("")
  }

  @Test
  fun `test initial text set to current line range with count of 1`() {
    typeExInput("1:")
    assertExText(".")
  }

  @Test
  fun `test initial text set to current line with offset for count greater than 1`() {
    typeExInput("10:")
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
    typeExInput(":set incsearch<Esc>")
    assertFalse(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeExInput(":set incsearch<C-[>")
    assertFalse(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()

    assertFalse(options().incsearch)
    typeExInput(":set incsearch<C-C>")
    assertFalse(options().incsearch)
    assertIsDeactivated()
  }

  @Test
  fun `test complete entry`() {
    assertFalse(options().incsearch)
    typeExInput(":set incsearch<Enter>")
    assertTrue(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
      }
    }

    assertFalse(options().incsearch)
    typeExInput(":set incsearch<C-J>")
    assertTrue(options().incsearch)
    assertIsDeactivated()

    deactivateExEntry()
    ApplicationManager.getApplication().runReadAction {
      VimPlugin.getOptionGroup().resetAllOptions(fixture.editor.vim)
    }

    assertFalse(options().incsearch)
    typeExInput(":set incsearch<C-M>")
    assertTrue(options().incsearch)
    assertIsDeactivated()
  }

  @Test
  fun `test ex entry clears status line`() {
    enterSearch("lorem")
    assertStatusLineMessageContains("Pattern not found: lorem")
    typeExInput(":")
    assertStatusLineCleared()
  }

  @Test
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

  @Test
  fun `test caret shape comes from guicursor`() {
    enterCommand("set guicursor=c:ver50,ci:hor75,cr:block")

    typeExInput(":")
    assertEquals("VER 50", exEntryPanel.entry.caretShape)

    typeText("set")
    assertEquals("VER 50", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home>")
    assertEquals("HOR 75", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home><Insert>")
    assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

    deactivateExEntry()
    typeExInput(":set<Home><Insert><Insert>")
    assertEquals("HOR 75", exEntryPanel.entry.caretShape)
  }

  @Test
  fun `test move caret to beginning of line`() {
    typeExInput(":set incsearch<C-B>")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set incsearch<Home>")
    assertExOffset(0)
  }

  @Test
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

  @Test
  fun `test backspace deletes character in front of caret`() {
    typeExInput(":set incsearch<BS>")
    assertExText("set incsearc")

    typeText("<C-H>")
    assertExText("set incsear")
  }

  @Test
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

  @Test
  fun `test delete deletes character under caret`() {
    typeExInput(":set<Left>")
    typeText("<Del>")
    assertExText("se")
  }

  @Test
  fun `test delete at end of string deletes character to left of caret`() {
    typeExInput(":set")
    typeText("<Del>")
    assertExText("se")
  }

  @Test
  fun `test delete with no text cancels entry`() {
    typeExInput(":set")
    typeText("<Del><Del><Del><Del>")
    assertIsDeactivated()
  }

  @Test
  fun `test delete word before caret`() {
    typeExInput(":set incsearch<C-W>")
    assertExText("set ")

    deactivateExEntry()

    typeExInput(":set incsearch<Left><Left><Left>")
    typeText("<C-W>")
    assertExText("set rch")
  }

  @Test
  fun `test delete to start of line`() {
    typeExInput(":set incsearch<C-U>")
    assertExText("")

    deactivateExEntry()

    typeExInput(":set incsearch<Left><Left><Left><C-U>")
    assertExText("rch")
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  @Test
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
    assertExText("set digraph")
    typeText("<Up>")
    assertExText("set incsearch")
    typeText("<Up>")
    assertExText("digraph")

    deactivateExEntry()

    typeExInput(":<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
    typeText("<PageUp>")
    assertExText("set incsearch")
  }

  @TestWithoutNeovim(SkipNeovimReason.CMD)
  @Test
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
    assertExText("set digraph")
    typeText("<S-Up>")
    assertExText("set incsearch")
    typeText("<S-Up>")
    assertExText("digraph")

    deactivateExEntry()

    typeExInput(":set<PageUp>")
    assertExText("digraph")
    typeText("<PageUp>")
    assertExText("set digraph")
    typeText("<PageUp>")
    assertExText("set incsearch")
  }

  @Test
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
    assertExText("something cool")
    typeText("<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")

    deactivateExEntry()

    typeExInput("/<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
    typeText("<PageUp>")
    assertExText("so cool")
  }

  @VimBehaviorDiffers(description = "Vim reorders history even when cancelling entry")
  @Test
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
    assertExText("something cool")
    typeText("<S-Up>")
    assertExText("so cool")
    typeText("<S-Up>")
    assertExText("not cool")

    deactivateExEntry()

    typeExInput("/so<PageUp>")
    assertExText("not cool")
    typeText("<PageUp>")
    assertExText("something cool")
    typeText("<PageUp>")
    assertExText("so cool")
  }

  @Test
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

  @Test
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
  @Test
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

  @Test
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

  @Test
  fun `test prompt while inserting digraph`() {
    typeExInput(":<C-K>")
    assertRenderedExText("?")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":<C-K>O")
    assertRenderedExText("O")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set<Home><C-K>")
    assertRenderedExText("?set")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":set<Home><C-K>O")
    assertRenderedExText("Oset")
    assertExOffset(0)
  }

  @Test
  fun `test escape cancels digraph`() {
    typeExInput(":<C-K><Esc>OK")
    assertIsActive()
    assertExText("OK")

    deactivateExEntry()

    // Note that the docs state that hitting escape stops digraph entry and cancels command line mode. In practice,
    // this isn't true - digraph entry is stopped, but command line mode continues
    typeExInput(":<C-K>O<Esc>K")
    assertIsActive()
    assertRenderedExText("K")

    deactivateExEntry()
  }

  @Test
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

  @Test
  fun `test insert literal control characters`() {
    typeExInput(":normal I[<C-V><Esc>A]<C-V><Esc>")
    assertExText("normal I[" + Char(27) + "A]" + Char(27))

    deactivateExEntry()

    // CR should be \n but Vim treats that as a NULL char, so we insert \r
    typeExInput(":nmap p <C-V><CR>")
    assertExText("nmap p \r")

    deactivateExEntry()

    typeExInput(":nmap p <C-V><C-D>")
    assertExText("nmap p " + Char(4))

    deactivateExEntry()

    typeExInput(":nmap p <C-V><C-I>")
    assertExText("nmap p \t")

    deactivateExEntry()

    typeExInput(":nmap p <C-V><C-V>")
    assertExText("nmap p " + Char(22))

    // TODO: IdeaVim handles <C-C> before handling digraphs/literals
//    typeExInput(":nmap p <C-V><C-C>")
//    assertExText("nmap p " + Char(3))
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test prompt while inserting literal character`() {
    typeExInput(":<C-V>")
    assertRenderedExText("^")
    assertExOffset(0)

    deactivateExEntry()

    typeExInput(":<C-V>o")
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
  fun `test insert register`() {
    VimPlugin.getRegister().setKeys('c', injector.parser.parseKeys("hello world"))
    VimPlugin.getRegister().setKeys('5', injector.parser.parseKeys("greetings programs"))

    typeExInput(":<C-R>c")
    assertExText("hello world")

    deactivateExEntry()

    typeExInput(":<C-R>5")
    assertExText("greetings programs")

    deactivateExEntry()

    typeExInput(":set<Home><C-R>c")
    assertExText("hello worldset")
    assertExOffset(11) // Just before 'set'
  }

  @Test
  fun `test insert multi-line register`() {
    // parseKeys parses <CR> in a way that Register#getText doesn't like
    val keys = mutableListOf<KeyStroke>()
    keys.addAll(injector.parser.parseKeys("hello<CR>world"))
    VimPlugin.getRegister().setKeys('c', keys)

    typeExInput(":<C-R>c")
    assertExText("hello\u000Dworld")
  }

  // TODO: Test other special registers, if/when supported
  // E.g. '.' '%' '#', etc.

  @Test
  fun `test insert last command`() {
    typeExInput(":set incsearch<CR>")
    typeExInput(":<C-R>:")
    assertExText("set incsearch")
  }

  @Test
  fun `test insert last search command`() {
    typeExInput("/hello<CR>")
    typeExInput(":<C-R>/")
    assertExText("hello")
  }

  @Test
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

  @Test
  fun `test cmap Ctrl`() {
    typeText(injector.parser.stringToKeys(":cmap <C-B> b") + injector.parser.parseKeys("<CR>"))
    typeExInput(":<C-B>")
    assertExText("b")
    deactivateExEntry()

    VimPlugin.getRegister().setKeys('e', injector.parser.parseKeys("hello world"))
    typeText(injector.parser.stringToKeys(":cmap d <C-R>") + injector.parser.parseKeys("<CR>"))
    typeExInput(":de")
    assertExText("hello world")
  }

  @Test
  @TestFor(issues = ["VIM-3506"])
  fun `test quote when awaiting for register during insert`() {
    injector.registerGroup.setKeys('w', injector.parser.parseKeys("world"))
    configureByText("")
    typeText(":hello <C-R>")
    assertRenderedExText("hello \"")
    typeText("w")
    assertRenderedExText("hello world")
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

  private fun typeExInput(text: String) {
    assertTrue(
      Regex("""\d*[:/?].*""").matches(text),
      "Ex command must start with '[count]:', '[count]/' or '[count]?'",
    )

    val keys = mutableListOf<KeyStroke>()
    injector.parser.parseKeys(text).forEach {
      // <Left> doesn't work correctly in tests. The DefaultEditorKit.NextVisualPositionAction action is correctly
      // called, but fails to move the caret correctly because the text component has never been painted
      if (it.keyCode == KeyEvent.VK_LEFT && it.modifiers == 0) {
        if (keys.isNotEmpty()) {
          typeText(keys)
          keys.clear()
        }

        exEntryPanel.entry.caret.dot--
      } else {
        keys.add(it)
      }
    }
    if (keys.isNotEmpty()) {
      typeText(keys)
    }
  }

  private fun deactivateExEntry() {
    // We don't need to reset text, that's handled by #active
    if (exEntryPanel.isActive) {
      typeText("<C-C>")
    }
  }

  private fun assertRenderedExText(expected: String) {
    // Get the text directly from the text field. This DOES include prompts or rendered control characters
    assertEquals(expected, exEntryPanel.getRenderedText())
  }

  private fun assertExText(expected: String) {
    // Get the text directly from the text field. This does NOT include prompts or rendered control characters
    assertEquals(expected, exEntryPanel.text)
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
    get() = ExEntryPanel.getOrCreateInstance()

  private val caret
    get() = exEntryPanel.entry.caret
}
