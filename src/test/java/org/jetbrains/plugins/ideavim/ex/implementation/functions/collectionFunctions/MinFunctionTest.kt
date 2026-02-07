/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.collectionFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MinFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test min with list of numbers`() {
    assertCommandOutput("echo min([3, 1, 4, 1, 5])", "1")
  }

  @Test
  fun `test min with single element`() {
    assertCommandOutput("echo min([42])", "42")
  }

  @Test
  fun `test min with negative numbers`() {
    assertCommandOutput("echo min([5, -3, 10, -7])", "-7")
  }

  @Test
  fun `test min with empty list`() {
    assertCommandOutput("echo min([])", "0")
  }

  @Test
  fun `test min with dictionary`() {
    assertCommandOutput("echo min({1: 5, 2: 3, 3: 7})", "3")
  }

  @Test
  fun `test min with empty dictionary`() {
    assertCommandOutput("echo min({})", "0")
  }
}
