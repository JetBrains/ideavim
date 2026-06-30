/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.fixtures.EditorMouseFixture
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests for the Vim 'mouse' option (VIM-2037).
 *
 * The 'mouse' option controls in which modes the mouse is enabled. Each flag enables the mouse for a mode:
 * 'n' (Normal), 'v' (Visual), 'i' (Insert), and 'a' (all modes). When the mouse is enabled for the current
 * mode, clicking in the editor moves the caret. When it is not enabled, the click must not move the caret.
 *
 * Mouse clicks are simulated with the platform [EditorMouseFixture], which dispatches real Swing mouse events
 * to the editor component, exercising IdeaVim's registered mouse listeners exactly as a user click would.
 *
 * Each test name reads as: mouse=<value>, <mode> mode -> caret moves / does not move.
 */
class MouseOptionTest : VimTestCase() {
  private companion object {
    // Two lines so a click on line 1 is clearly distinguishable from the initial caret on line 0.
    const val TEXT = "Lorem ipsum dolor sit amet,\nconsectetur adipiscing elit"
  }

  // --- Normal mode -----------------------------------------------------------------------------

  @Test
  fun `mouse= (empty), normal mode -- caret does not move`() = assertClick(mouse = "", enterMode = "", moves = false)

  @Test
  fun `mouse=n, normal mode -- caret moves`() = assertClick(mouse = "n", enterMode = "", moves = true)

  @Test
  fun `mouse=v, normal mode -- caret does not move`() = assertClick(mouse = "v", enterMode = "", moves = false)

  @Test
  fun `mouse=i, normal mode -- caret does not move`() = assertClick(mouse = "i", enterMode = "", moves = false)

  @Test
  fun `mouse=nvi, normal mode -- caret moves`() = assertClick(mouse = "nvi", enterMode = "", moves = true)

  @Test
  fun `mouse=a, normal mode -- caret moves`() = assertClick(mouse = "a", enterMode = "", moves = true)

  // --- Insert mode -----------------------------------------------------------------------------

  @Test
  fun `mouse= (empty), insert mode -- caret does not move`() = assertClick(mouse = "", enterMode = "i", moves = false)

  @Test
  fun `mouse=n, insert mode -- caret does not move`() = assertClick(mouse = "n", enterMode = "i", moves = false)

  @Test
  fun `mouse=v, insert mode -- caret does not move`() = assertClick(mouse = "v", enterMode = "i", moves = false)

  @Test
  fun `mouse=i, insert mode -- caret moves`() = assertClick(mouse = "i", enterMode = "i", moves = true)

  @Test
  fun `mouse=nvi, insert mode -- caret moves`() = assertClick(mouse = "nvi", enterMode = "i", moves = true)

  @Test
  fun `mouse=a, insert mode -- caret moves`() = assertClick(mouse = "a", enterMode = "i", moves = true)

  // --- Visual mode -----------------------------------------------------------------------------

  @Test
  fun `mouse= (empty), visual mode -- caret does not move`() = assertClick(mouse = "", enterMode = "v", moves = false)

  @Test
  fun `mouse=n, visual mode -- caret does not move`() = assertClick(mouse = "n", enterMode = "v", moves = false)

  @Test
  fun `mouse=v, visual mode -- caret moves`() = assertClick(mouse = "v", enterMode = "v", moves = true)

  @Test
  fun `mouse=i, visual mode -- caret does not move`() = assertClick(mouse = "i", enterMode = "v", moves = false)

  @Test
  fun `mouse=nvi, visual mode -- caret moves`() = assertClick(mouse = "nvi", enterMode = "v", moves = true)

  @Test
  fun `mouse=a, visual mode -- caret moves`() = assertClick(mouse = "a", enterMode = "v", moves = true)

  // --- Visual line-wise mode -------------------------------------------------------------------
  // The 'v' flag governs all Visual variants (character-, line-, and block-wise), not just character-wise.

  @Test
  fun `mouse=n, visual-line mode -- caret does not move`() = assertClick(mouse = "n", enterMode = "V", moves = false)

  @Test
  fun `mouse=v, visual-line mode -- caret moves`() = assertClick(mouse = "v", enterMode = "V", moves = true)

  @Test
  fun `mouse=a, visual-line mode -- caret moves`() = assertClick(mouse = "a", enterMode = "V", moves = true)

  // --- Visual block-wise mode ------------------------------------------------------------------

  @Test
  fun `mouse=n, visual-block mode -- caret does not move`() = assertClick(mouse = "n", enterMode = "<C-V>", moves = false)

  @Test
  fun `mouse=v, visual-block mode -- caret moves`() = assertClick(mouse = "v", enterMode = "<C-V>", moves = true)

  @Test
  fun `mouse=a, visual-block mode -- caret moves`() = assertClick(mouse = "a", enterMode = "<C-V>", moves = true)

  @Test
  fun `mouse=v, select mode -- caret moves`() = assertClick(mouse = "v", enterMode = "gh", moves = true)

  @Test
  fun `mouse=a, select mode -- caret moves`() = assertClick(mouse = "a", enterMode = "gh", moves = true)

  @Test
  fun `mouse=v, select-line mode -- caret moves`() = assertClick(mouse = "v", enterMode = "gH", moves = true)

  @Test
  fun `mouse=a, select-line mode -- caret moves`() = assertClick(mouse = "a", enterMode = "gH", moves = true)

  @Test
  fun `mouse=v, select-block mode -- caret moves`() = assertClick(mouse = "v", enterMode = "g<C-h>", moves = true)

  @Test
  fun `mouse=a, select-block mode -- caret moves`() = assertClick(mouse = "a", enterMode = "g<C-h>", moves = true)

  // --- Replace mode ----------------------------------------------------------------------------
  // Replace mode is part of the Insert family, so it is governed by the 'i' flag.

  @Test
  fun `mouse=n, replace mode -- caret does not move`() = assertClick(mouse = "n", enterMode = "R", moves = false)

  @Test
  fun `mouse=i, replace mode -- caret moves`() = assertClick(mouse = "i", enterMode = "R", moves = true)

  @Test
  fun `mouse=a, replace mode -- caret moves`() = assertClick(mouse = "a", enterMode = "R", moves = true)

  // --- Command-line mode -----------------------------------------------------------------------
  // Command-line mode is governed by the 'c' flag. Note 'a' includes it, but the default 'nvi' does NOT.

  @Test
  fun `mouse= (empty), command-line mode -- caret does not move`() = assertClick(mouse = "", enterMode = ":", moves = false)

  @Test
  fun `mouse=n, command-line mode -- caret does not move`() = assertClick(mouse = "n", enterMode = ":", moves = false)

  @Test
  fun `mouse=nvi (default), command-line mode -- caret does not move`() = assertClick(mouse = "nvi", enterMode = ":", moves = false)

  @Test
  fun `mouse=c, command-line mode -- caret moves`() = assertClick(mouse = "c", enterMode = ":", moves = true)

  @Test
  fun `mouse=a, command-line mode -- caret moves`() = assertClick(mouse = "a", enterMode = ":", moves = true)

  /**
   * Sets `mouse=[mouse]`, enters the mode by typing [enterMode] (empty string stays in Normal), then clicks on
   * the second line and asserts whether the caret offset changed.
   */
  private fun assertClick(mouse: String, enterMode: String, moves: Boolean) {
    configureByText(TEXT)
    enterCommand("set mouse=$mouse")

    if (enterMode.isNotEmpty()) typeText(enterMode)

    val before = readOffset()

    // Click on the second line, away from the initial caret on the first line.
    ApplicationManager.getApplication().invokeAndWait {
      EditorMouseFixture(fixture.editor as EditorImpl).clickAt(1, 10)
    }

    val after = readOffset()

    if (moves) {
      assertNotEquals(before, after, "Caret should move on click when mouse is enabled (mouse=$mouse)")
    } else {
      assertEquals(before, after, "Caret should NOT move on click when mouse is disabled (mouse=$mouse)")
    }
  }

  private fun readOffset(): Int {
    var offset = 0
    ApplicationManager.getApplication().invokeAndWait {
      offset = fixture.editor.caretModel.primaryCaret.offset
    }
    return offset
  }
}
