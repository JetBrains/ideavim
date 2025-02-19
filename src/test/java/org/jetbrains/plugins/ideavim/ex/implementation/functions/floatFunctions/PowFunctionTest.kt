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

class PowFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test pow with Number`() {
    assertCommandOutput("echo pow(3, 3) pow(2, 16)", "27.0 65536.0")
  }

  @Test
  fun `test pow with Float`() {
    assertCommandOutput("echo pow(32, 0.2)", "2.0")
  }

  @Test
  fun `test pow with negative number`() {
    assertCommandOutput("echo pow(-2, 3)", "-8.0")
  }

  @Test
  fun `test pow with negative number for exponent`() {
    assertCommandOutput("echo pow(2, -3)", "0.125")
  }

  @Test
  fun `test pow with string causes errors`() {
    enterCommand("echo pow('1.0', 2)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with string causes errors 2`() {
    enterCommand("echo pow(1.0, '2')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with invalid string causes errors`() {
    enterCommand("echo pow('cheese', 2)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with invalid string causes errors 2`() {
    enterCommand("echo pow(2, 'cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with list causes errors`() {
    enterCommand("echo pow([1.0], 2)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with list causes errors 2`() {
    enterCommand("echo pow(2, [1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with dictionary causes errors`() {
    enterCommand("echo pow({1: 1.0}, 2)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test pow with dictionary causes errors 2`() {
    enterCommand("echo pow(2, {1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
