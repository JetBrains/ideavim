/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class LenFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test len`() {
    assertCommandOutput("echo len(123)", "3")
    assertCommandOutput("echo len('abcd')", "4")
    assertCommandOutput("echo len([1])", "1")
    assertCommandOutput("echo len({})", "0")
    assertCommandOutput("echo len(#{1: 'one'})", "1")
    assertCommandOutput("echo len(12 . 4)", "3")
  }

  @Test
  fun `test len with float causes errors`() {
    enterCommand("echo len(4.2)")
    assertPluginErrorMessageContains("E701: Invalid type for len()")
  }
}
