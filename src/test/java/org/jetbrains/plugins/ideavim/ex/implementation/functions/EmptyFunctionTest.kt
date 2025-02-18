/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class EmptyFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test empty`() {
    assertCommandOutput("echo empty(0) empty(1)", "1 0")
    assertCommandOutput("echo empty(\"123\") empty(\"\")", "0 1")
    assertCommandOutput("echo empty([1, 2]) empty([])", "0 1")
    assertCommandOutput("echo empty({1:2}) empty({})", "0 1")
  }
}
