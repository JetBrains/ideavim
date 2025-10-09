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

class IndexFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test index finds element`() {
    assertCommandOutput("echo index([1, 2, 3, 2], 2)", "1")
  }

  @Test
  fun `test index element not found`() {
    assertCommandOutput("echo index([1, 2, 3], 5)", "-1")
  }

  @Test
  fun `test index with start position`() {
    assertCommandOutput("echo index([1, 2, 3, 2], 2, 2)", "3")
  }

  @Test
  fun `test index with negative start`() {
    assertCommandOutput("echo index([1, 2, 3, 4, 5], 4, -3)", "3")
  }

  @Test
  fun `test index with ignore case`() {
    assertCommandOutput("echo index(['a', 'B', 'c'], 'b', 0, 1)", "1")
  }

  @Test
  fun `test index string vs number different`() {
    assertCommandOutput("echo index(['4', 5, 6], 4)", "-1")
  }

  @Test
  fun `test index in empty list`() {
    assertCommandOutput("echo index([], 1)", "-1")
  }

  @Test
  fun `test index first occurrence`() {
    assertCommandOutput("echo index([1, 2, 1, 2], 1)", "0")
  }
}
