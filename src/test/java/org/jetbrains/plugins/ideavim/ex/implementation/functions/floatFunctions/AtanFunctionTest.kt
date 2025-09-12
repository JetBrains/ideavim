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

class AtanFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test atan with Number`() {
    assertCommandOutput("echo atan(100)", "1.560797")
    assertCommandOutput("echo atan(0) atan(1)", "0.0 0.785398")
  }

  @Test
  fun `test atan with Float`() {
    assertCommandOutput("echo atan(100.0) atan(-4.01)", "1.560797 -1.326405")
  }

  @Test
  fun `test atan with string causes errors`() {
    enterCommand("echo atan('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test atan with invalid string causes errors`() {
    enterCommand("echo atan('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test atan with list causes errors`() {
    enterCommand("echo atan([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test atan with dictionary causes errors`() {
    enterCommand("echo atan({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
