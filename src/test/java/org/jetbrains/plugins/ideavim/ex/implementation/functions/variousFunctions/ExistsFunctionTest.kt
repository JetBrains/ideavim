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

class ExistsFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test exists`() {
    assertCommandOutput("echo exists(\"&nu\")", "1")
    assertCommandOutput("echo exists(\"&unknownOptionName\")", "0")
    assertCommandOutput("echo exists(\"g:myVar\")", "0")
    enterCommand("let myVar = 42")
    assertCommandOutput("echo exists(\"g:myVar\")", "1")
  }
}
