/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.variousFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ErrTeapotFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test err_teapot`() {
    enterCommand("echo err_teapot()")
    assertPluginError(true)
    assertPluginErrorMessage("E418: I am a teapot")
  }

  @Test
  fun `test err_teapot with boolean false argument`() {
    enterCommand("echo err_teapot(0)")
    assertPluginError(true)
    assertPluginErrorMessage("E418: I am a teapot")
  }

  @Test
  fun `test err_teapot with boolean true argument`() {
    enterCommand("echo err_teapot(1)")
    assertPluginError(true)
    assertPluginErrorMessage("E503: Coffee is currently not available")
  }

  @Test
  fun `test err_teapot with false string argument`() {
    enterCommand("echo err_teapot(\"0\")")
    assertPluginError(true)
    assertPluginErrorMessage("E418: I am a teapot")
  }

  @Test
  fun `test err_teapot with true string argument`() {
    enterCommand("echo err_teapot(\"1\")")
    assertPluginError(true)
    assertPluginErrorMessage("E503: Coffee is currently not available")
  }

  @Test
  fun `test err_teapot with list argument`() {
    enterCommand("echo err_teapot([0])")
    assertPluginError(true)
    assertPluginErrorMessage("E745: Using a List as a Number")
  }

  @Test
  fun `test err_teapot with dictionary argument`() {
    enterCommand("echo err_teapot({1: 0})")
    assertPluginError(true)
    assertPluginErrorMessage("E728: Using a Dictionary as a Number")
  }
}
