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

class FmodFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test fmod returns nan when arguments are zero`() {
    // The docs say this should return 0
    assertCommandOutput("echo fmod(0, 0) fmod(42, 0) fmod(0, 42)", "nan nan 0.0")
  }

  @Test
  fun `test fmod with integer values`() {
    assertCommandOutput("echo fmod(12, 7)", "5.0")
  }

  @Test
  fun `test fmod with float values`() {
    assertCommandOutput("echo fmod(12.33, 1.22)", "0.13")
  }

  @Test
  fun `test fmod with negative values`() {
    assertCommandOutput("echo fmod(-12.33, 1.22)", "-0.13")
  }

  @Test
  fun `test fmod with string value causes errors`() {
    enterCommand("echo fmod('42', 7)")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test fmod with string value causes errors 2`() {
    enterCommand("echo fmod(42, '7')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test fmod with list value causes errors`() {
    enterCommand("echo fmod([1, 2], 7)")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test fmod with list value causes errors 2`() {
    enterCommand("echo fmod(42, [1, 2])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test fmod with dictionary value causes errors`() {
    enterCommand("echo fmod({1: 2}, 7)")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test fmod with dictionary value causes errors 2`() {
    enterCommand("echo fmod(42, {1: 2})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
