/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class JoinFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test join`() {
    assertCommandOutput("echo join(['Vim', 'is', 'awesome'], '_')", "Vim_is_awesome")
  }

  @Test
  fun `test join without second argument defaults to spaces`() {
    assertCommandOutput("echo join(['Vim', 'is', 'awesome'])", "Vim is awesome")
  }

  @Test
  fun `test join with wrong first argument type`() {
    typeText(commandToKeys("echo join('Vim is awesome')"))
    assertPluginError(true)
    assertPluginErrorMessage("E1211: List required for argument 1")
  }
}
