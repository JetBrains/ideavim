/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.util.application
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ui.OutputPanel
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import java.awt.event.MouseEvent
import kotlin.test.assertNull

/**
 * Tests for the 'r' flag of the Vim 'mouse' option as it applies to the more / hit-enter prompt (VIM-2037).
 *
 * The 'mouse' option's 'r' flag enables the mouse "for |hit-enter| and |more-prompt| prompt" (see `:help 'mouse'`).
 * When the mouse is enabled at that prompt, a click acts like pressing a key to continue, so it dismisses the
 * hit-enter prompt and closes the pager. When the mouse is not enabled there (the default 'nvi' does NOT include 'r'),
 * a click on the prompt is ignored and the panel stays open. The 'a' flag enables the mouse for all modes and so
 * includes the prompt.
 *
 * The more / hit-enter prompt is the [OutputPanel] (the `vimMorePanel`), shown as a pager for multi-line command
 * output. A click is simulated by dispatching a Swing [MouseEvent] to the panel component, mirroring the way
 * [MessageAreaTest] drives the panel directly because focus is not available in unit tests.
 *
 * Each test name reads as: mouse=<value>, hit-enter prompt -> click closes / does not close.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.IDEAVIM_API_USED)
class MouseMorePromptOptionTest : VimTestCase("\n") {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    OutputPanel.allowHideEmptyText = true
    configureByText("\n")
  }

  @Test
  fun `mouse=r, hit-enter prompt -- click closes panel`() = assertClickClosesPrompt(mouse = "r", closes = true)

  @Test
  fun `mouse=a, hit-enter prompt -- click closes panel`() = assertClickClosesPrompt(mouse = "a", closes = true)

  @Test
  fun `mouse=nvi (default), hit-enter prompt -- click does not close panel`() =
    assertClickClosesPrompt(mouse = "nvi", closes = false)

  @Test
  fun `mouse= (empty), hit-enter prompt -- click does not close panel`() =
    assertClickClosesPrompt(mouse = "", closes = false)

  @Test
  fun `mouse=nv (no r), hit-enter prompt -- click does not close panel`() =
    assertClickClosesPrompt(mouse = "nv", closes = false)

  /**
   * Sets `mouse=[mouse]`, shows multi-line output to bring up the hit-enter prompt, clicks the panel, then asserts
   * whether the panel closed ([closes] == true) or stayed open.
   */
  private fun assertClickClosesPrompt(mouse: String, closes: Boolean) {
    enterCommand("set mouse=$mouse")

    // Multi-line output is shown via the pager and ends with the hit-enter prompt.
    enterCommand("echo \"Lorem ipsum\\nHello world\"")
    val panel = getOutputPanel()

    clickPanel(panel)

    if (closes) {
      assertNull(
        injector.outputPanel.getCurrentOutputPanel(),
        "Click should close the hit-enter prompt when the mouse is enabled for it (mouse=$mouse)",
      )
    } else {
      assertNotNull(
        injector.outputPanel.getCurrentOutputPanel(),
        "Click should be ignored and the prompt stay open when the mouse is not enabled for it (mouse=$mouse)",
      )
    }
  }

  private fun getOutputPanel(): OutputPanel {
    val panel = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(panel, "Expecting the hit-enter prompt to be visible")
    return panel as OutputPanel
  }

  private fun clickPanel(panel: OutputPanel) {
    application.invokeAndWait {
      val now = System.currentTimeMillis()
      for (id in intArrayOf(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
        panel.dispatchEvent(MouseEvent(panel, id, now, 0, 5, 5, 1, false, MouseEvent.BUTTON1))
      }
    }
  }
}
