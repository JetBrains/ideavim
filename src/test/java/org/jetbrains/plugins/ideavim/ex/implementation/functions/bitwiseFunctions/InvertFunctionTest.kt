/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.bitwiseFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class InvertFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test invert function inverts bits`() {
    assertCommandOutput("echo invert(0b1010)", "-11")
  }

  @Test
  fun `test invert function with list causes error`() {
    enterCommand("echo invert([1, 2, 3])")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E745: Using a List as a Number")
  }

  @Test
  fun `test invert function with dict causes error`() {
    enterCommand("echo invert({1: 2, 3: 4})")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E728: Using a Dictionary as a Number")
  }

  @Test
  fun `test invert function with float causes error`() {
    enterCommand("echo invert(1.5)")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E805: Using a Float as a Number")
  }
}
