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

class RepeatFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test repeat basic string`() {
    assertCommandOutput("echo repeat('abc', 3)", "abcabcabc")
  }

  @Test
  fun `test repeat with zero count`() {
    assertCommandOutput("echo repeat('hello', 0)", "")
  }

  @Test
  fun `test repeat with negative count`() {
    assertCommandOutput("echo repeat('test', -5)", "")
  }

  @Test
  fun `test repeat single character`() {
    assertCommandOutput("echo repeat('x', 5)", "xxxxx")
  }

  @Test
  fun `test repeat empty string`() {
    assertCommandOutput("echo repeat('', 10)", "")
  }

  @Test
  fun `test repeat with number coerces to string`() {
    assertCommandOutput("echo repeat(123, 2)", "123123")
  }

  @Test
  fun `test repeat with list`() {
    assertCommandOutput("echo repeat(['a', 'b'], 3)", "['a', 'b', 'a', 'b', 'a', 'b']")
  }

  @Test
  fun `test repeat with empty list`() {
    assertCommandOutput("echo repeat([], 5)", "[]")
  }

  @Test
  fun `test repeat with list and zero count`() {
    assertCommandOutput("echo repeat([1, 2, 3], 0)", "[]")
  }
}
