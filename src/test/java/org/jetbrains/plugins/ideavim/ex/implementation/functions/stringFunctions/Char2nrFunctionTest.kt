/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.stringFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class Char2nrFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test char2nr with single character`() {
    assertCommandOutput("echo char2nr('a')", "97")
  }

  @Test
  fun `test char2nr with uppercase character`() {
    assertCommandOutput("echo char2nr('A')", "65")
  }

  @Test
  fun `test char2nr with number character`() {
    assertCommandOutput("echo char2nr('0')", "48")
  }

  @Test
  fun `test char2nr with space`() {
    assertCommandOutput("echo char2nr(' ')", "32")
  }

  @Test
  fun `test char2nr with multiple characters returns first`() {
    assertCommandOutput("echo char2nr('abc')", "97")
  }

  @Test
  fun `test char2nr with empty string`() {
    assertCommandOutput("echo char2nr('')", "0")
  }

  @Test
  fun `test char2nr with unicode character`() {
    assertCommandOutput("echo char2nr('©')", "169")
  }
}
