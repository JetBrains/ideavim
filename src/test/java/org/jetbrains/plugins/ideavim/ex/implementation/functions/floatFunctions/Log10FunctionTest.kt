/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.floatFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class Log10FunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test log10 with Number`() {
    assertCommandOutput("echo log10(1000)", "3.0")
  }

  @Test
  fun `test log10 with Float`() {
    assertCommandOutput("echo log10(0.01)", "-2.0")
  }

  @Test
  fun `test log10 with negative number`() {
    assertCommandOutput("echo log10(-1)", "nan")
  }

  @Test
  fun `test log10 with string causes errors`() {
    enterCommand("echo log10('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test log10 with invalid string causes errors`() {
    enterCommand("echo log10('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test log10 with list causes errors`() {
    enterCommand("echo log10([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test log10 with dictionary causes errors`() {
    enterCommand("echo log10({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
