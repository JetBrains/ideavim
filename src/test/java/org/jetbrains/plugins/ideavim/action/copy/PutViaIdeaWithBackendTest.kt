/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.maddyhome.idea.vim.group.copy.BackendPasteAwaiter
import com.maddyhome.idea.vim.group.copy.backendPasteAwaiterEP
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithPrimaryClipboard
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.assertEquals

/**
 * The frontend of split mode, where the clipboard is put back after the backend paste.
 */
@TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
class PutViaIdeaWithBackendTest : VimTestCase() {
  private val backend = FakeBackend()

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    ExtensionTestUtil.maskExtensions(backendPasteAwaiterEP, listOf(backend), fixture.testRootDisposable)
    CopyPasteManager.getInstance().setContents(StringSelection("EXTERNAL"))
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    backend.completeAll()
    super.tearDown(testInfo)
  }

  @Test
  fun `test clipboard is put back after the backend paste`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "\"aP")
    assertEquals("alpha", clipboardText())

    backend.completeAll()
    assertEquals("EXTERNAL", clipboardText())
    assertState("alph${c}aalpha beta")
  }

  @Test
  fun `test clipboard is put back after the backend paste of the last put`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "w", "\"byiw", "\"ap", "\"bp")
    assertEquals("beta", clipboardText())

    backend.complete(0)
    assertEquals("beta", clipboardText())
    backend.complete(1)
    assertEquals("EXTERNAL", clipboardText())
  }

  @Test
  fun `test repeated put keeps the register in the clipboard until the last backend paste`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "\"ap", "\"ap")

    backend.complete(0)
    assertEquals("alpha", clipboardText())
    backend.complete(1)
    assertEquals("EXTERNAL", clipboardText())
  }

  @Test
  fun `test clipboard register is read from the content put back`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "w", "\"aP", "\"+p")

    backend.completeAll()
    assertState("alpha alphaEXTERNA${c}Lbeta")
    assertEquals("EXTERNAL", clipboardText())
  }

  @Test
  fun `test unnamedplus put after a put from another register`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=unnamedplus,ideaput")
    typeText("\"ayiw", "w", "\"aP", "p")

    backend.completeAll()
    assertState("alpha alphaEXTERNA${c}Lbeta")
    assertEquals("EXTERNAL", clipboardText())
  }

  @TestWithPrimaryClipboard
  fun `test clipboard register is read from the content put back with primary selection`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "w", "\"aP", "\"+p")

    backend.completeAll()
    assertState("alpha alphaEXTERNA${c}Lbeta")
    assertEquals("EXTERNAL", clipboardText())
  }

  @TestWithPrimaryClipboard
  fun `test unnamedplus put after a put from another register with primary selection`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=unnamedplus,ideaput")
    typeText("\"ayiw", "w", "\"aP", "p")

    backend.completeAll()
    assertState("alpha alphaEXTERNA${c}Lbeta")
    assertEquals("EXTERNAL", clipboardText())
  }

  @Test
  fun `test yank of the same text during the backend paste is kept in the clipboard`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=unnamed,ideaput")
    typeText("\"ayiw", "w", "\"aP", "0yiw")

    backend.completeAll()
    assertEquals("alpha", clipboardText())
  }

  @Test
  fun `test clipboard register is read from the content put back after a put not pasted on the backend`() {
    configureByText("${c}alpha beta")
    enterCommand("set clipboard=ideaput")
    typeText("\"ayiw", "w", "\"byiw")
    backend.pasteOnFrontendOnly(1)
    typeText("\"aP", "\"bp", "\"+p")

    backend.completeAll()
    assertState("alpha alphabetaEXTERNA${c}Lbeta")
  }

  private fun clipboardText(): String? =
    CopyPasteManager.getInstance().contents?.getTransferData(DataFlavor.stringFlavor) as? String

  private class FakeBackend : BackendPasteAwaiter {
    private val pastes = mutableListOf<CompletableDeferred<Unit>>()
    private val frontendOnlyPastes = mutableSetOf<Int>()
    private var pasteCount = 0

    fun pasteOnFrontendOnly(index: Int) {
      frontendOnlyPastes += index
    }

    override fun paste(context: DataContext, paste: (DataContext) -> Unit): (suspend () -> Unit)? {
      paste(context)
      if (pasteCount++ in frontendOnlyPastes) return null
      val backendPaste = CompletableDeferred<Unit>()
      pastes += backendPaste
      return { backendPaste.await() }
    }

    fun complete(index: Int) {
      pastes[index].complete(Unit)
      ApplicationManager.getApplication().invokeAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
    }

    fun completeAll() {
      pastes.indices.forEach(::complete)
    }
  }
}
