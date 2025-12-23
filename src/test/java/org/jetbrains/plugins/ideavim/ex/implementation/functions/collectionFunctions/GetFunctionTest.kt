/*
 * Copyright 2003-2023 The IdeaVim authors
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

class GetFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test get with dictionary`() {
    assertCommandOutput("echo get({1: 'one', 2: 'two', 3: 'three'}, '2')", "two")
  }

  @Test
  fun `test get by nonexistent key in dictionary`() {
    assertCommandOutput("echo get({1: 'one', 2: 'two', 3: 'three'}, '10')", "0")
  }

  @Test
  fun `test get by nonexistent key in dictionary with default value`() {
    assertCommandOutput("echo get({1: 'one', 2: 'two', 3: 'three'}, '10', 'null')", "null")
  }

  @Test
  fun `test get with list`() {
    assertCommandOutput("echo get(['one', 'two', 'three'], 1)", "two")
  }

  @Test
  fun `test get by nonexistent index in list`() {
    assertCommandOutput("echo get(['one', 'two', 'three'], 10)", "0")
  }

  @Test
  fun `test get by nonexistent index in list with default value`() {
    assertCommandOutput("echo get(['one', 'two', 'three'], 10, 'null')", "null")
  }
}
