/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.collectionFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CountFunctionTest : VimTestCase("\n") {
  @Test
  fun `test count in list`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2)", "2")
  }

  @Test
  fun `test count in list with no matches`() {
    assertCommandOutput("echo count([1, 2, 3], 5)", "0")
  }

  @Test
  fun `test count list occurrences in list`() {
    assertCommandOutput("echo count([[1,2,3], [1,2,3], [4,5,6]], [1,2,3])", "2")
  }

  @Test
  fun `test count list reference occurrences in list`() {
    enterCommand("let l = [1, 2, 3]")
    assertCommandOutput("echo count([l, [1,2,3], [4,5,6]], l)", "2")
  }

  @Test
  fun `test count in string`() {
    assertCommandOutput("echo count('hello world', 'l')", "3")
  }

  @Test
  fun `test count in string non-overlapping`() {
    assertCommandOutput("echo count('aaaa', 'aa')", "2")
  }

  @Test
  fun `test count in dictionary`() {
    assertCommandOutput("echo count({1: 'a', 2: 'b', 3: 'a'}, 'a')", "2")
  }

  @Test
  fun `test count dictionary occurrences in dictionary`() {
    assertCommandOutput("echo count({1: 'a', 2: {'key1': 'value'}, 3: 'a'}, {'key1': 'value'})", "1")
  }

  @Test
  fun `test count list with start index`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2, 0, 2)", "1")
  }

  @Test
  fun `test count list with zero start index`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2, 0, 0)", "2")
  }

  @Test
  fun `test count list with negative start index`() {
    // Vim counts negative start indices from the end of the list, so -2 starts at the last two items [2, 1]
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2, 0, -2)", "1")
  }

  @Test
  fun `test count list with negative start index covering whole list`() {
    assertCommandOutput("echo count([1, 2, 3, 2, 1], 2, 0, -5)", "2")
  }

  @Test
  fun `test count list with out of range start index`() {
    enterCommand("echo count([1, 2, 3, 2, 1], 2, 0, 5)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 5")
  }

  @Test
  fun `test count list with out of range negative start index`() {
    enterCommand("echo count([1, 2, 3, 2, 1], 2, 0, -6)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: -6")
  }

  @Test
  fun `test count list with ignore case`() {
    assertCommandOutput("echo count(['A', 'a', 'B'], 'a', 1)", "2")
  }

  @Test
  fun `test count empty string`() {
    assertCommandOutput("echo count('hello', '')", "0")
  }
}
