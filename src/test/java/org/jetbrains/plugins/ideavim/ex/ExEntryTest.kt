/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LoggedErrorProcessor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.OnlyThrowLoggedErrorProcessor
import org.jetbrains.plugins.ideavim.action.ex.VimExTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

  // VIM-4115: closing the command line alongside fullReset() must clear editor mode and the
  // KeyHandler's commandLineCommandBuilder, not just deactivate the panel. Without close(), the
  // KeyHandler singleton retains the CMD_LINE builder across plugin disable/enable and the next
  // Esc NPEs in CommandKeyConsumer.
  @Test
  fun `test VIM-4115 close before fullReset clears all command line state`() {
    typeText(":set incsearch")
    assertExIsActive()
    assertTrue(fixture.editor.vim.mode is Mode.CMD_LINE)
    assertNotNull(KeyHandler.getInstance().keyHandlerState.commandLineCommandBuilder)

    ApplicationManager.getApplication().invokeAndWait {
      val commandLine = injector.commandLine
      commandLine.getActiveCommandLine()?.close(refocusOwningEditor = true, resetCaret = false)
      commandLine.fullReset()
    }

    assertExIsDeactivated()
    assertFalse(fixture.editor.vim.mode is Mode.CMD_LINE)
    assertNull(KeyHandler.getInstance().keyHandlerState.commandLineCommandBuilder)
  }

  // VIM-4115: if some other path still desyncs command-line state (panel gone but
  // commandLineCommandBuilder set with the CMD_LINE trie), Esc in the editor must not NPE. The
  // defensive branch logs an error and clears the leftover builder.
  @Test
  fun `test VIM-4115 escape with stale command line builder does not crash`() {
    typeText(":set incsearch")
    assertExIsActive()

    // Reproduce the pre-fix plugin-disable state: panel gone, builder and mode left behind. Use
    // INSERT (not NORMAL) so EditorResetConsumer won't claim Esc and the key actually reaches
    // CommandKeyConsumer where the crash lives.
    ApplicationManager.getApplication().invokeAndWait {
      injector.commandLine.fullReset()
      fixture.editor.vim.mode = Mode.INSERT
    }
    assertNotNull(KeyHandler.getInstance().keyHandlerState.commandLineCommandBuilder)

    // The defensive path logs an error; rethrow it so we can assert no NPE slips through.
    try {
      LoggedErrorProcessor.executeWith<Throwable>(OnlyThrowLoggedErrorProcessor) {
        assertDoesNotThrow { typeText("<Esc>") }
      }
    } catch (e: Throwable) {
      val message = generateSequence(e) { it.cause }.mapNotNull { it.message }.joinToString(" / ")
      assertTrue(message.contains("VIM-4115"), "Expected VIM-4115 logger.error, got: $message")
    }

    assertNull(KeyHandler.getInstance().keyHandlerState.commandLineCommandBuilder)
  }
}
