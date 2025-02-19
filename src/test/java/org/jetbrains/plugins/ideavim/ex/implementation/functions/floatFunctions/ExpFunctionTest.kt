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

class ExpFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test exp with Number`() {
    assertCommandOutput("echo exp(2)", "7.389056")
  }

  @Test
  fun `test exp with Float`() {
    assertCommandOutput("echo exp(-4.01)", "0.018133")
  }

  @Test
  fun `test exp with negative number`() {
    assertCommandOutput("echo exp(-1)", "0.367879")
  }

  @Test
  fun `test exp with string causes errors`() {
    enterCommand("echo exp('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test exp with invalid string causes errors`() {
    enterCommand("echo exp('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test exp with list causes errors`() {
    enterCommand("echo exp([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test exp with dictionary causes errors`() {
    enterCommand("echo exp({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
