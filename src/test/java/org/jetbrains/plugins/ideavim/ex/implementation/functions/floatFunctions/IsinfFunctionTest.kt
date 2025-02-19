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

class IsinfFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test isinf returns false for valid integer`() {
    assertCommandOutput("echo isinf(42)", "0")
  }

  @Test
  fun `test isinf returns false for valid float`() {
    assertCommandOutput("echo isinf(4.2)", "0")
  }

  @Test
  fun `test isinf returns one for positive infinity`() {
    assertCommandOutput("echo isinf(1.0/0.0)", "1")
  }

  @Test
  fun `test isinf returns minus one for negative infinity`() {
    assertCommandOutput("echo isinf(-1.0/0.0)", "-1")
  }

  @Test
  fun `test isinf returns false for dividing by integer zero`() {
    assertCommandOutput("echo isinf(0/0) isinf(1/0)", "0 0")
  }

  @Test
  fun `test isinf returns false for string value`() {
    assertCommandOutput("echo isinf('42')", "0")
  }

  @Test
  fun `test isinf returns false for list`() {
    assertCommandOutput("echo isinf([1, 2, 3])", "0")
  }

  @Test
  fun `test isinf returns false for dictionary`() {
    assertCommandOutput("echo isinf({1: 1})", "0")
  }
}
