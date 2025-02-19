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

class TanhFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test tanh with Number`() {
    assertCommandOutput("echo tanh(100)", "1.0")
    assertCommandOutput("echo tanh(0) tanh(1)", "0.0 0.761594")
  }

  @Test
  fun `test tanh with Float`() {
    assertCommandOutput("echo tanh(0.5) tanh(-1.0)", "0.462117 -0.761594")
  }

  @Test
  fun `test tanh with string causes errors`() {
    enterCommand("echo tanh('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test tanh with invalid string causes errors`() {
    enterCommand("echo tanh('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test tanh with list causes errors`() {
    enterCommand("echo tanh([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test tanh with dictionary causes errors`() {
    enterCommand("echo tanh({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
