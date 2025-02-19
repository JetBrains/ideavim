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

class AcosFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test acos with Number`() {
    assertCommandOutput("echo acos(0)", "1.570796")
    assertCommandOutput("echo acos(-1) acos(0) acos(1)", "3.141593 1.570796 0.0")
  }

  @Test
  fun `test acos with Float`() {
    assertCommandOutput("echo acos(-0.5)", "2.094395")
    assertCommandOutput("echo acos(-1.0) acos(0.0) acos(1.0)", "3.141593 1.570796 0.0")
  }

  @Test
  fun `test acos with value greater than 1 returns nan`() {
    assertCommandOutput("echo acos(1.1)", "nan")
  }

  @Test
  fun `test acos with value less than -1 returns nan`() {
    assertCommandOutput("echo acos(-1.1)", "nan")
  }

  @Test
  fun `test acos with string causes errors`() {
    enterCommand("echo acos('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test acos with invalid string causes errors`() {
    enterCommand("echo acos('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test acos with list causes errors`() {
    enterCommand("echo acos([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test acos with dictionary causes errors`() {
    enterCommand("echo acos({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
