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

class LenFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test len`() {
    assertCommandOutput("echo len(123)", "3")
    assertCommandOutput("echo len('abcd')", "4")
    assertCommandOutput("echo len([1])", "1")
    assertCommandOutput("echo len({})", "0")
    assertCommandOutput("echo len(#{1: 'one'})", "1")
    assertCommandOutput("echo len(12 . 4)", "3")
  }

  @Test
  fun `test len with empty string`() {
    assertCommandOutput("echo len('')", "0")
    assertCommandOutput("echo len(\"\")", "0")
  }

  @Test
  fun `test len with empty list`() {
    assertCommandOutput("echo len([])", "0")
  }

  @Test
  fun `test len with zero number`() {
    assertCommandOutput("echo len(0)", "1")
  }

  @Test
  fun `test len with negative numbers`() {
    assertCommandOutput("echo len(-123)", "4")
    assertCommandOutput("echo len(-1)", "2")
  }

  @Test
  fun `test len with large number`() {
    assertCommandOutput("echo len(9999999)", "7")
  }

  @Test
  fun `test len with multi-element list`() {
    assertCommandOutput("echo len([1, 2, 3, 4, 5])", "5")
  }

  @Test
  fun `test len with multi-element dictionary`() {
    assertCommandOutput("echo len(#{a: 1, b: 2, c: 3})", "3")
  }

  @Test
  fun `test len with string containing special characters`() {
    // Single-quoted strings in Vim don't interpret escape sequences (except '')
    assertCommandOutput("echo len('hello\\nworld')", "12")
    // Double-quoted strings do interpret escape sequences
    assertCommandOutput("echo len(\"hello\\nworld\")", "11")
  }

  @Test
  fun `test len with float causes errors`() {
    enterCommand("echo len(4.2)")
    assertPluginErrorMessage("E701: Invalid type for len()")
  }
}
