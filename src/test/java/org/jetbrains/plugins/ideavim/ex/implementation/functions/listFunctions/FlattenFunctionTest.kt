/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FlattenFunctionTest : VimTestCase("\n") {
  @Test
  fun `test flatten with String reports error`() {
    enterCommand("call flatten('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flatten() must be a List")
  }

  @Test
  fun `test flatten with Number reports error`() {
    enterCommand("call flatten(100)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flatten() must be a List")
  }

  @Test
  fun `test flatten with Float reports error`() {
    enterCommand("call flatten(12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flatten() must be a List")
  }

  @Test
  fun `test flatten with Dictionary reports error`() {
    enterCommand("call flatten({'key': 'value'})")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flatten() must be a List")
  }

  @Test
  fun `test flatten with nested List`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]])", "[1, 2, 3, 4, 5]")
  }

  @Test
  fun `test flatten modifies original List`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("let b = a")
    enterCommand("call flatten(a)")
    assertCommandOutput("echo a", "[1, 2, 3, 4, 5]")
    assertCommandOutput("echo b", "[1, 2, 3, 4, 5]")
  }

  @Test
  fun `test flatten does not modify nested Lists`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("let b = [1, [2, 3], [4, [5]]]")
    enterCommand("let c = [6, [7, 8], [9, [10]]]")
    enterCommand("let a = [b, c]")
    enterCommand("call flatten(a)")
    assertCommandOutput("echo a", "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]")
    assertCommandOutput("echo b", "[1, [2, 3], [4, [5]]]")
    assertCommandOutput("echo c", "[6, [7, 8], [9, [10]]]")
  }

  @Test
  fun `test flatten with already flat List`() {
    assertCommandOutput("echo flatten([1, 2, 3])", "[1, 2, 3]")
  }

  @Test
  fun `test flatten with empty List`() {
    assertCommandOutput("echo flatten([])", "[]")
  }

  @Test
  fun `test flatten with recursive List`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let a[2] = a")
    assertCommandOutput("echo flatten(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flatten with nested recursive List`() {
    enterCommand("let a = [[1, 2], 3, [4, 5]]")
    enterCommand("let a[1] = a")
    assertCommandOutput("echo flatten(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flatten with indirectly recursive List`() {
    enterCommand("let a = [1, [2, 3, 4], 5]")
    enterCommand("let a[1][1] = a")
    assertCommandOutput("echo flatten(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flatten on a locked List reports error`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("lockvar a")
    enterCommand("call flatten(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: flatten() argument")
  }

  @Test
  fun `test flatten on a locked List item does nor report error`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("lockvar a[1]")
    assertCommandOutput("echo flatten(a)", "[1, 2, 3, 4, 5]")
    assertPluginError(false)
  }

  @Test
  fun `test flatten with invalid depth argument reports error`() {
    enterCommand("call flatten([1, [2, 3]], 12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @Test
  fun `test flatten with negative depth argument reports error`() {
    enterCommand("call flatten([1, [2, 3]], -1)")
    assertPluginError(true)
    assertPluginErrorMessage("E900: maxdepth must be non-negative")
  }

  @Test
  fun `test flatten with max depth 0 does not flatten List`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]], 0)", "[1, [2, 3], [4, [5]]]")
  }

  @Test
  fun `test flatten with max depth 1 only flattens first depth`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]], 1)", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flatten with String max depth 1 only flattens first depth`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]], '1')", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flatten with String max depth 1 with trailing characters flattens first depth`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]], '1foo')", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flatten with max depth 2 flattens deeper`() {
    assertCommandOutput("echo flatten([1, [2, 3], [4, [5]]], 2)", "[1, 2, 3, 4, 5]")
  }
}
