/*
 * Copyright 2003-2026 The IdeaVim authors
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

class XorFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test xor function bitwise XORs two numbers`() {
    assertCommandOutput("echo xor(2, 7)", "5")
  }

  @Test
  fun `test xor function bitwise XORs two numbers with negative numbers`() {
    assertCommandOutput("echo xor(-2, -6)", "4")
  }

  @Test
  fun `test xor function returns 0`() {
    assertCommandOutput("echo xor(2, 2)", "0")
  }

  @Test
  fun `test xor function coerces string to number`() {
    assertCommandOutput("echo xor('0x07', '35')", "36")
  }

  @Test
  fun `test xor function with list causes error`() {
    enterCommand("echo xor([1, 2, 3], [2, 3, 4])")
    assertPluginError(true)
    assertPluginErrorMessage("E745: Using a List as a Number")
  }

  @Test
  fun `test xor function with dict causes error`() {
    enterCommand("echo xor({1: 2, 3: 4}, {3: 4, 5: 6})")
    assertPluginError(true)
    assertPluginErrorMessage("E728: Using a Dictionary as a Number")
  }

  @Test
  fun `test xor function with float causes error`() {
    enterCommand("echo xor(1.5, 2.5)")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }
}
