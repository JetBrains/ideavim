/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.variousFunctions

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class HasFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test has for supported feature`() {
    assertCommandOutput("echo has('ide')", "1")
  }

  @Test
  fun `test has for unsupported feature`() {
    assertCommandOutput("echo has('autocmd')", "0")
  }

  @Test
  fun `test has for int as an argument`() {
    assertCommandOutput("echo has(42)", "0")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test has for list as an argument`() {
    enterCommand("echo has([])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E730: Using a List as a String")
  }
}
