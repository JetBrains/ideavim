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

class TanFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test tan with Number`() {
    assertCommandOutput("echo tan(10)", "0.648361")
    assertCommandOutput("echo tan(0) tan(1)", "0.0 1.557408")
  }

  @Test
  fun `test tan with Float`() {
    assertCommandOutput("echo tan(-4.01)", "-1.181502")
    assertCommandOutput("echo tan(0.0) tan(1.0)", "0.0 1.557408")
  }

  @Test
  fun `test tan with string causes errors`() {
    enterCommand("echo tan('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test tan with invalid string causes errors`() {
    enterCommand("echo tan('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test tan with list causes errors`() {
    enterCommand("echo tan([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test tan with dictionary causes errors`() {
    enterCommand("echo tan({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
