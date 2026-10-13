/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ui

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.ui.OutputPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Single-line output is cleared after the 'messagesopt' "wait", counted from the last time its text was refreshed
 *
 * The panel is never showing in unit tests, so the timer that it would start when shown is started here instead.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.IDEAVIM_API_USED)
class OutputPanelClearTimerTest : VimTestCase() {

  private var clearTimer: Job? = null

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    clearTimer?.cancel()
    super.tearDown(testInfo)
  }

  @Test
  fun `single-line output is cleared after the wait`() {
    showMessageWithWait("hello", WAIT_MS)

    assertTrue(spinUntil(WAIT_MS * 5) { currentPanel() == null }, "Expected the output to be cleared after the wait")
  }

  @Test
  fun `single-line output is kept open without a wait`() {
    showMessageWithWait("hello", 0)

    spinUntil(WAIT_MS) { currentPanel() == null }
    assertNotNull(currentPanel(), "Expected wait:0 to keep single-line output open")
  }

  @Test
  fun `refreshing the text restarts the wait`() {
    assertWaitRestartedBy { injector.messages.showMessage(fixture.editor.vim, "/needle") }
  }

  @Test
  fun `setting the status text restarts the wait`() {
    assertWaitRestartedBy { currentPanel()!!.statusText = "[2/3]" }
  }

  private fun assertWaitRestartedBy(refresh: () -> Unit) {
    showMessageWithWait("/needle", WAIT_MS)
    spinUntil(WAIT_MS / 2) { false }
    assertNotNull(currentPanel(), "The output was cleared before it could be refreshed")

    val refreshedAt = System.currentTimeMillis()
    onEdt(refresh)
    assertTrue(spinUntil(WAIT_MS * 5) { currentPanel() == null }, "Expected the output to be cleared after the wait")

    val openAfterRefresh = System.currentTimeMillis() - refreshedAt
    assertTrue(
      openAfterRefresh >= WAIT_MS,
      "Expected the full wait after the refresh, but cleared after $openAfterRefresh ms",
    )
  }

  private fun showMessageWithWait(message: String, waitMs: Long) {
    configureByText("lorem ipsum")
    enterCommand("set messagesopt=hit-enter,history:500,wait:$waitMs")
    onEdt { injector.messages.showMessage(fixture.editor.vim, message) }
    val panel = currentPanel()
    assertNotNull(panel)
    assertTrue(panel.isSingleLine)
    clearTimer = CoroutineScope(Dispatchers.EDT).launch { panel.runClearTimer() }
  }

  private fun currentPanel(): OutputPanel? = injector.outputPanel.getCurrentOutputPanel() as OutputPanel?

  private fun onEdt(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait(action)

  /** Lets the EDT run the timer until [condition] is met or [timeoutMs] passes */
  private fun spinUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
    var result = false
    onEdt {
      val end = System.currentTimeMillis() + timeoutMs
      while (System.currentTimeMillis() < end) {
        if (condition()) {
          result = true
          return@onEdt
        }
        Thread.sleep(5)
        IdeEventQueue.getInstance().flushQueue()
      }
    }
    return result
  }

  private companion object {
    const val WAIT_MS = 300L
  }
}
