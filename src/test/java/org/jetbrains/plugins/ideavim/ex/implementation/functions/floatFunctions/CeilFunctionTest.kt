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

class CeilFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test ceil rounds up float value`() {
    assertCommandOutput("echo ceil(1.456)", "2.0")
    assertCommandOutput("echo ceil(4.0)", "4.0")
  }

  @Test
  fun `test ceil returns integer value as float value`() {
    assertCommandOutput("echo ceil(1)", "1.0")
  }

  @Test
  fun `test ceil with negative float value`() {
    assertCommandOutput("echo ceil(-5.456)", "-5.0")
  }

  @Test
  fun `test ceil with string causes errors`() {
    enterCommand("echo ceil('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test ceil with invalid string causes errors`() {
    enterCommand("echo ceil('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test ceil with list causes errors`() {
    enterCommand("echo ceil([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test ceil with dictionary causes errors`() {
    enterCommand("echo ceil({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
