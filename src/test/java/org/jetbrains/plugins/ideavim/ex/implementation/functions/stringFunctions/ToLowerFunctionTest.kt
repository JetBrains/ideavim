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

class ToLowerFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test tolower`() {
    assertCommandOutput("echo tolower('Vim is awesome')", "vim is awesome")
  }

  @Test
  fun `test tolower with number coerces to string`() {
    assertCommandOutput("echo tolower(123)", "123")
  }

  @Test
  fun `test tolower with list causes error`() {
    enterCommand("echo tolower([1, 2, 3])")
    assertNoOutput()
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test tolower with dict causes error`() {
    enterCommand("echo tolower({1: 2, 3: 4})")
    assertNoOutput()
    assertPluginError(true)
    assertPluginErrorMessage("E731: Using a Dictionary as a String")
  }
}
