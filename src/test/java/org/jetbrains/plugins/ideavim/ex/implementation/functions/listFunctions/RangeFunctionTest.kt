/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class RangeFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test range with single argument`() {
    assertCommandOutput("echo range(4)", "[0, 1, 2, 3]")
  }

  @Test
  fun `test range with start and end`() {
    assertCommandOutput("echo range(2, 4)", "[2, 3, 4]")
  }

  @Test
  fun `test range with stride`() {
    assertCommandOutput("echo range(2, 9, 3)", "[2, 5, 8]")
  }

  @Test
  fun `test range with negative stride`() {
    assertCommandOutput("echo range(2, -2, -1)", "[2, 1, 0, -1, -2]")
  }

  @Test
  fun `test range with zero returns empty`() {
    assertCommandOutput("echo range(0)", "[]")
  }

  @Test
  fun `test range max one before start returns empty`() {
    assertCommandOutput("echo range(2, 1)", "[]")
  }

  @Test
  fun `test range single element`() {
    assertCommandOutput("echo range(5, 5)", "[5]")
  }

  @Test
  fun `test range with zero stride throws error`() {
    enterCommand("echo range(1, 5, 0)")
    kotlin.test.assertTrue(injector.messages.isError())
  }

  @Test
  fun `test range with start past end throws error`() {
    enterCommand("echo range(2, 0)")
    kotlin.test.assertTrue(injector.messages.isError())
  }

  @Test
  fun `test range negative with start past end throws error`() {
    enterCommand("echo range(-2, 0, -1)")
  }
}
