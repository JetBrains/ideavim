/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class VimExTestCase : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText()
  }

  protected open fun configureByText() {
    configureByText("\n")
  }

  protected fun assertExText(expected: String) {
    // Get the text directly from the text field. This does NOT include prompts or rendered control characters
    if (expected.contains(c)) {
      val actual = exEntryPanel.text.substring(0, exEntryPanel.caret.offset) + c + exEntryPanel.text.substring(exEntryPanel.caret.offset)
      assertEquals(expected, actual)
    }
    else {
      assertEquals(expected, exEntryPanel.text)
    }
  }

  protected fun assertRenderedExText(expected: String) {
    // Get the text directly from the text field. This DOES include prompts or rendered control characters
    assertEquals(expected, exEntryPanel.getRenderedText())
  }

  protected fun assertExOffset(expected: Int) {
    assertEquals(expected, exEntryPanel.entry.caret.dot)
  }

  protected fun assertExIsActive() {
    assertTrue(exEntryPanel.isActive)
  }

  protected fun assertExIsDeactivated() {
    assertFalse(exEntryPanel.isActive)
  }

  protected fun deactivateExEntry() {
    // We don't need to reset text, that's handled by #active
    if (exEntryPanel.isActive) {
      typeText("<C-C>")
    }
  }

  protected val exEntryPanel: ExEntryPanel
    get() = ExEntryPanel.getOrCreatePanelInstance()
}
