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

class ReverseFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test reverse basic list`() {
    assertCommandOutput("echo reverse([1, 2, 3])", "[3, 2, 1]")
  }

  @Test
  fun `test reverse string list`() {
    assertCommandOutput("echo reverse(['a', 'b', 'c'])", "['c', 'b', 'a']")
  }

  @Test
  fun `test reverse single element`() {
    assertCommandOutput("echo reverse([1])", "[1]")
  }

  @Test
  fun `test reverse empty list`() {
    assertCommandOutput("echo reverse([])", "[]")
  }

  @Test
  fun `test reverse modifies original list`() {
    enterCommand("let mylist = [1, 2, 3]")
    enterCommand("call reverse(mylist)")
    assertCommandOutput("echo mylist", "[3, 2, 1]")
  }

  @Test
  fun `test reverse with mixed types`() {
    assertCommandOutput("echo reverse([1, 'two', 3])", "[3, 'two', 1]")
  }

  @Test
  fun `test reverse with string`() {
    assertCommandOutput("echo reverse('hello')", "olleh")
  }

  @Test
  fun `test reverse with empty string`() {
    assertCommandOutput("echo reverse('')", "")
  }

  @Test
  fun `test reverse with single character string`() {
    assertCommandOutput("echo reverse('a')", "a")
  }

  @Test
  fun `test reverse with number returns 0`() {
    assertCommandOutput("echo reverse(123)", "0")
  }

  @Test
  fun `test reverse with dict returns 0`() {
    assertCommandOutput("echo reverse({1: 2})", "0")
  }
}
