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

class RoundFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test round rounds float value to nearest integer`() {
    assertCommandOutput("echo round(0.456) round(4.5)", "0.0 5.0")
  }

  @Test
  fun `test round returns integer value as float value`() {
    assertCommandOutput("echo round(1)", "1.0")
  }

  @Test
  fun `test round with negative float value`() {
    assertCommandOutput("echo round(-4.5)", "-5.0")
  }

  @Test
  fun `test round with string causes errors`() {
    enterCommand("echo round('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test round with invalid string causes errors`() {
    enterCommand("echo round('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test round with list causes errors`() {
    enterCommand("echo round([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test round with dictionary causes errors`() {
    enterCommand("echo round({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
