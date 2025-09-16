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

class SinhFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test sinh with Number`() {
    assertCommandOutput("echo sinh(100)", "1.344059e43")
    assertCommandOutput("echo sinh(0) sinh(1)", "0.0 1.175201")
  }

  @Test
  fun `test sinh with Float`() {
    assertCommandOutput("echo sinh(0.5) sinh(-0.9)", "0.521095 -1.026517")
  }

  @Test
  fun `test sinh with string causes errors`() {
    enterCommand("echo sinh('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sinh with invalid string causes errors`() {
    enterCommand("echo sinh('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sinh with list causes errors`() {
    enterCommand("echo sinh([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sinh with dictionary causes errors`() {
    enterCommand("echo sinh({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
