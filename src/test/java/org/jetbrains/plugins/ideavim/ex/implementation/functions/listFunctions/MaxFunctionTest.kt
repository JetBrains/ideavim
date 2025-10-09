/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MaxFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test max with list of numbers`() {
    assertCommandOutput("echo max([3, 1, 4, 1, 5])", "5")
  }

  @Test
  fun `test max with single element`() {
    assertCommandOutput("echo max([42])", "42")
  }

  @Test
  fun `test max with negative numbers`() {
    assertCommandOutput("echo max([-5, -3, -10, -7])", "-3")
  }

  @Test
  fun `test max with empty list`() {
    assertCommandOutput("echo max([])", "0")
  }

  @Test
  fun `test max with dictionary`() {
    assertCommandOutput("echo max({1: 5, 2: 3, 3: 7})", "7")
  }

  @Test
  fun `test max with empty dictionary`() {
    assertCommandOutput("echo max({})", "0")
  }
}
