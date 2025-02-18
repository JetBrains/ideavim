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

class SplitFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test split with default delimiter`() {
    assertCommandOutput("echo split('Hello world')", "['Hello', 'world']")
  }

  @Test
  fun `test split comma separated text`() {
    assertCommandOutput("echo split('a,b,c,d', ',')", "['a', 'b', 'c', 'd']")
  }

  @Test
  fun `test split comma separated text 2`() {
    assertCommandOutput("echo split(',a,b,c,d,', ',')", "['a', 'b', 'c', 'd']")
  }

  @Test
  fun `test split comma separated text with keepempty flag`() {
    assertCommandOutput("echo split(',a,b,c,,d,', ',', 1)", "['', 'a', 'b', 'c', '', 'd', '']")
  }
}
