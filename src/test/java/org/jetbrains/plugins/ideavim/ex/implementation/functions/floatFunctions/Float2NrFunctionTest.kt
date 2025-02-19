/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.floatFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class Float2NrFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test float2nr returns integer value for integer`() {
    assertCommandOutput("echo float2nr(42)", "42")
  }

  @VimBehaviorDiffers(
    originalVimAfter = "3 -23 2147483647 -2147483647 0",
    description = "The Vim docs say float2nr(-1.0e150) should return -2147483647 not -2147483648"
  )
  @Test
  fun `test float2nr returns integer value for float`() {
    assertCommandOutput(
      "echo float2nr(3.95) float2nr(-23.45) float2nr(1.0e100) float2nr(-1.0e150) float2nr(1.0e-100)",
      "3 -23 2147483647 -2147483648 0"
    )
  }

  @Test
  fun `test float2nr with string causes errors`() {
    enterCommand("echo float2nr('1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test float2nr with invalid string causes errors`() {
    enterCommand("echo float2nr('cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test float2nr with list causes errors`() {
    enterCommand("echo float2nr([1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test float2nr with dictionary causes errors`() {
    enterCommand("echo float2nr({1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
