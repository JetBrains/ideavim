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

class CoshFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test cosh with Number`() {
    assertCommandOutput("echo cosh(100)", "1.344059e43")
    assertCommandOutput("echo cosh(0) cosh(1)", "1.0 1.543081")
  }

  @Test
  fun `test cosh with Float`() {
    // The Vim docs show cosh(-0.5) == -1.127626, but the function returns the positive value...
    // We are matching Vim's actual behaviour here
    assertCommandOutput("echo cosh(0.5) cosh(-0.5)", "1.127626 1.127626")
  }

  @Test
  fun `test cosh with string causes errors`() {
    enterCommand("echo cosh('1.0')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test cosh with invalid string causes errors`() {
    enterCommand("echo cosh('cheese')")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test cosh with list causes errors`() {
    enterCommand("echo cosh([1.0])")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }

  @Test
  fun `test cosh with dictionary causes errors`() {
    enterCommand("echo cosh({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessage("E808: Number or Float required")
  }
}
