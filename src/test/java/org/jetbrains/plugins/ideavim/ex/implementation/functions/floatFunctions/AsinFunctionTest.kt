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

class AsinFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test asin with Number`() {
    assertCommandOutput("echo asin(1)", "1.570796")
    assertCommandOutput("echo asin(-1) asin(0) asin(1)", "-1.570796 0.0 1.570796")
  }

  @Test
  fun `test asin with Float`() {
    assertCommandOutput("echo asin(1.0)", "1.570796")
    assertCommandOutput("echo asin(0.8) asin(-0.5)", "0.927295 -0.523599")
  }

  @Test
  fun `test asin with value greater than 1 returns nan`() {
    assertCommandOutput("echo asin(1.1)", "nan")
  }

  @Test
  fun `test asin with value less than -1 returns nan`() {
    assertCommandOutput("echo asin(-1.1)", "nan")
  }

  @Test
  fun `test asin with string causes errors`() {
    enterCommand("echo asin('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test asin with invalid string causes errors`() {
    enterCommand("echo asin('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test asin with list causes errors`() {
    enterCommand("echo asin([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test asin with dictionary causes errors`() {
    enterCommand("echo asin({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
