/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.KeyEvent
import kotlin.test.assertEquals

/**
 * The tag stack in Rider, whose navigation to a definition runs on the ReSharper backend
 */
class RiderTagStackTest : IdeaVimRiderTestBase() {

  @AfterEach
  fun resetMode() {
    esc()
  }

  // VIM-4374
  @Test
  fun `ctrl-t returns from ctrl-click`() {
    setText(TEXT)

    // The backend may still be analysing the new text, and then the click does not navigate yet
    val navigated = waitUntil(timeoutMs = 60_000, pollMs = 0) {
      ctrlClickOn("Foo")
      waitUntil(timeoutMs = 3000) { caretLine() == DECLARATION_LINE }
    }
    assertTrue(navigated) { "Ctrl+Click on Foo did not go to its declaration, the caret is on line ${caretLine()}" }

    ctrl(KeyEvent.VK_T)

    assertCaretLine(CALL_LINE)
  }

  // VIM-4374: the backend may run Go to Declaration a second time for the same jump
  @Test
  fun `ctrl-bracket pushes the jump once`() {
    setText(TEXT)

    val navigated = waitUntil(timeoutMs = 60_000, pollMs = 0) {
      ctrl(KeyEvent.VK_CLOSE_BRACKET)
      waitUntil(timeoutMs = 3000) { caretLine() == DECLARATION_LINE }
    }
    assertTrue(navigated) { "<C-]> on Foo did not go to its declaration, the caret is on line ${caretLine()}" }

    ctrl(KeyEvent.VK_T)
    assertCaretLine(CALL_LINE)

    // A second entry for the same jump would bring the caret back to the call
    typeVim("j")
    assertCaretLine(CALL_LINE + 1)
    val offset = caretOffset()
    ctrl(KeyEvent.VK_T)
    // The keys run in order, so once `l` has moved the caret, <C-T> is done
    typeVim("l")
    waitUntil { caretOffset() != offset }
    assertEquals(CALL_LINE + 1, caretLine(), "Caret line after <C-T> at the bottom of the tag stack")
  }

  companion object {
    private const val CALL_LINE = 0
    private const val DECLARATION_LINE = 3

    private val TEXT = """
      |Foo();
      |Bar();
      |
      |static void Foo()
      |{
      |}
      |
      |static void Bar()
      |{
      |}
    """.trimMargin()
  }
}
