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

class SinFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test sin with Number`() {
    assertCommandOutput("echo sin(100)", "-0.506366")
    assertCommandOutput("echo sin(0) sin(1)", "0.0 0.841471")
  }

  @Test
  fun `test sin with Float`() {
    assertCommandOutput("echo sin(-4.01)", "0.763301")
    assertCommandOutput("echo sin(0.0) sin(1.0)", "0.0 0.841471")
  }

  @Test
  fun `test sin with string causes errors`() {
    enterCommand("echo sin('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sin with invalid string causes errors`() {
    enterCommand("echo sin('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sin with list causes errors`() {
    enterCommand("echo sin([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test sin with dictionary causes errors`() {
    enterCommand("echo sin({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
