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

class TruncFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test trunc truncates float value`() {
    assertCommandOutput("echo trunc(1.456)", "1.0")
    assertCommandOutput("echo trunc(4.0)", "4.0")
  }

  @Test
  fun `test trunc returns integer value as float value`() {
    assertCommandOutput("echo trunc(1)", "1.0")
  }

  @Test
  fun `test trunc with negative float value`() {
    assertCommandOutput("echo trunc(-5.456)", "-5.0")
  }

  @Test
  fun `test trunc with string causes errors`() {
    enterCommand("echo trunc('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test trunc with invalid string causes errors`() {
    enterCommand("echo trunc('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test trunc with list causes errors`() {
    enterCommand("echo trunc([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test trunc with dictionary causes errors`() {
    enterCommand("echo trunc({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
