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

class LogFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test log with Number`() {
    assertCommandOutput("echo log(10)", "2.302585")
  }

  @Test
  fun `test log with Float`() {
    assertCommandOutput("echo log(0.1)", "-2.302585")
  }

  @Test
  fun `test log and exp round trips value`() {
    assertCommandOutput("echo log(exp(1))", "1.0")
  }

  @Test
  fun `test log with negative number`() {
    assertCommandOutput("echo log(-1)", "nan")
  }

  @Test
  fun `test log with string causes errors`() {
    enterCommand("echo log('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test log with invalid string causes errors`() {
    enterCommand("echo log('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test log with list causes errors`() {
    enterCommand("echo log([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test log with dictionary causes errors`() {
    enterCommand("echo log({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
