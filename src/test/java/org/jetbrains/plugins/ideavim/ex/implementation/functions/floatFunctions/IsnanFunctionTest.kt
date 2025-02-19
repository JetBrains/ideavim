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

class IsnanFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test isnan returns false for valid integer`() {
    assertCommandOutput("echo isnan(42)", "0")
  }

  @Test
  fun `test isnan returns false for valid float`() {
    assertCommandOutput("echo isnan(4.2)", "0")
  }

  @Test
  fun `test isnan returns true for NaN`() {
    assertCommandOutput("echo isnan(0.0/0.0)", "1")
  }

  @Test
  fun `test isnan returns false for dividing by integer zero`() {
    assertCommandOutput("echo isnan(0/0)", "0")
  }

  @Test
  fun `test isnan returns false for string value`() {
    assertCommandOutput("echo isnan('42')", "0")
  }

  @Test
  fun `test isnan returns false for list`() {
    assertCommandOutput("echo isnan([1, 2, 3])", "0")
  }

  @Test
  fun `test isnan returns false for dictionary`() {
    assertCommandOutput("echo isnan({1: 1})", "0")
  }
}
