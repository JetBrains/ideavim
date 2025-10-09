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

class Nr2charFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test nr2char with lowercase letter`() {
    assertCommandOutput("echo nr2char(97)", "a")
  }

  @Test
  fun `test nr2char with uppercase letter`() {
    assertCommandOutput("echo nr2char(65)", "A")
  }

  @Test
  fun `test nr2char with number`() {
    assertCommandOutput("echo nr2char(48)", "0")
  }

  @Test
  fun `test nr2char with space`() {
    assertCommandOutput("echo nr2char(32)", " ")
  }

  @Test
  fun `test nr2char with newline`() {
    assertCommandOutput("echo nr2char(10)", "\n")
  }

  @Test
  fun `test nr2char with tab`() {
    assertCommandOutput("echo nr2char(9)", "\t")
  }

  @Test
  fun `test nr2char with unicode character`() {
    assertCommandOutput("echo nr2char(169)", "©")
  }

  @Test
  fun `test nr2char with negative number`() {
    assertCommandOutput("echo nr2char(-1)", "")
  }

  @Test
  fun `test nr2char with zero`() {
    assertCommandOutput("echo nr2char(0)", "\u0000")
  }
}
