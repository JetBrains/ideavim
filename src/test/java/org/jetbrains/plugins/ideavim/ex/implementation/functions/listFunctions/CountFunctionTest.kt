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

class CountFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test count in list`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2)", "2")
  }

  @Test
  fun `test count in list with no matches`() {
    assertCommandOutput("echo count([1, 2, 3], 5)", "0")
  }

  @Test
  fun `test count in string`() {
    assertCommandOutput("echo count('hello world', 'l')", "3")
  }

  @Test
  fun `test count in string non-overlapping`() {
    assertCommandOutput("echo count('aaaa', 'aa')", "2")
  }

  @Test
  fun `test count in dictionary`() {
    assertCommandOutput("echo count({1: 'a', 2: 'b', 3: 'a'}, 'a')", "2")
  }

  @Test
  fun `test count with start index`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2, 0, 2)", "1")
  }

  @Test
  fun `test count with ignore case`() {
    assertCommandOutput("echo count(['A', 'a', 'B'], 'a', 1)", "2")
  }

  @Test
  fun `test count empty string`() {
    assertCommandOutput("echo count('hello', '')", "0")
  }
}
