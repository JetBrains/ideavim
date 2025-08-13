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
    assertEquals(expected, exEntryPanel.text)
  }

  protected fun assertExIsDeactivated() {
    assertFalse(exEntryPanel.isActive)
  }

  private val exEntryPanel
    get() = ExEntryPanel.getOrCreateInstance()
}
