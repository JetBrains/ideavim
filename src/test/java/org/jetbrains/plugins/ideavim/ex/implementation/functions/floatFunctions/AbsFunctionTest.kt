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

class AbsFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test abs with float value`() {
    assertCommandOutput("echo abs(1.456)", "1.456")
    assertCommandOutput("echo abs(-5.456)", "5.456")
  }

  @Test
  fun `test abs with integer value`() {
    assertCommandOutput("echo abs(-4)", "4")
  }

  @Test
  fun `test abs with coerced float value`() {
    assertCommandOutput("""echo abs("-5.456")""", "5")
  }

  @Test
  fun `test abs with coerced integer value`() {
    assertCommandOutput("""echo abs("-4")""", "4")
  }

  @Test
  fun `test abs with list causes error`() {
    enterCommand("""echo abs([-5.456])""")
    assertPluginError(true)
    assertPluginErrorMessage("E745: Using a List as a Number")
  }

  @Test
  fun `test abs with dictionary causes error`() {
    enterCommand("""echo abs({1: -5.456})""")
    assertPluginError(true)
    assertPluginErrorMessage("E728: Using a Dictionary as a Number")
  }
}
