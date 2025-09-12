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

class FloorFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test floor rounds down float value`() {
    assertCommandOutput("echo floor(1.856)", "1.0")
    assertCommandOutput("echo floor(4.0)", "4.0")
  }

  @Test
  fun `test floor returns integer value as float value`() {
    assertCommandOutput("echo floor(1)", "1.0")
  }

  @Test
  fun `test floor with negative float value`() {
    assertCommandOutput("echo floor(-5.456)", "-6.0")
  }

  @Test
  fun `test floor with string causes errors`() {
    enterCommand("echo floor('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test floor with invalid string causes errors`() {
    enterCommand("echo floor('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test floor with list causes errors`() {
    enterCommand("echo floor([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test floor with dictionary causes errors`() {
    enterCommand("echo floor({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
