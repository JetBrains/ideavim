/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.floatFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class Atan2FunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test atan2 with Number`() {
    assertCommandOutput("echo atan2(100, 100)", "0.785398")
    assertCommandOutput("echo atan2(-1, 1) atan2(1, -1)", "-0.785398 2.356194")
  }

  @Test
  fun `test atan2 with Float`() {
    assertCommandOutput("echo atan2(-1.0, 1.0) atan2(1.0, -1.0)", "-0.785398 2.356194")
  }

  @Test
  fun `test atan2 with string causes errors`() {
    enterCommand("echo atan2('1.0', 1.0)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with string causes errors 2`() {
    enterCommand("echo atan2(1.0, '1.0')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with invalid string causes errors`() {
    enterCommand("echo atan2('cheese', 1.0)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
  @Test

  fun `test atan2 with invalid string causes errors 2`() {
    enterCommand("echo atan2(1.0, 'cheese')")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with list causes errors`() {
    enterCommand("echo atan2([1.0], 1.0)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with list causes errors 2`() {
    enterCommand("echo atan2(1.0, [1.0])")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with dictionary causes errors`() {
    enterCommand("echo atan2({1: 1.0}, 1.0)")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }

  @Test
  fun `test atan2 with dictionary causes errors 2`() {
    enterCommand("echo atan2(1.0, {1: 1.0})")
    assertPluginError(true)
    assertPluginErrorMessageContains("E808: Number or Float required")
  }
}
