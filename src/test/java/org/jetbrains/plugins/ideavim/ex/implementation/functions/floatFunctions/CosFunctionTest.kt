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

class CosFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test cos with Number`() {
    assertCommandOutput("echo cos(100)", "0.862319")
    assertCommandOutput("echo cos(0) cos(1)", "1.0 0.540302")
  }

  @Test
  fun `test cos with Float`() {
    assertCommandOutput("echo cos(-4.01)", "-0.646043")
    assertCommandOutput("echo cos(0.0) cos(1.0)", "1.0 0.540302")
  }

  @Test
  fun `test cos with string causes errors`() {
    enterCommand("echo cos('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test cos with invalid string causes errors`() {
    enterCommand("echo cos('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test cos with list causes errors`() {
    enterCommand("echo cos([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test cos with dictionary causes errors`() {
    enterCommand("echo cos({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
