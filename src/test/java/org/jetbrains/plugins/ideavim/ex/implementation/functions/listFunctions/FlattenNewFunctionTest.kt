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

class FlattenNewFunctionTest : VimTestCase("\n") {
  @Test
  fun `test flattennew with String reports error`() {
    enterCommand("call flattennew('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flattennew() must be a List")
  }

  @Test
  fun `test flattennew with Number reports error`() {
    enterCommand("call flattennew(100)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flattennew() must be a List")
  }

  @Test
  fun `test flattennew with Float reports error`() {
    enterCommand("call flattennew(12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flattennew() must be a List")
  }

  @Test
  fun `test flattennew with Dictionary reports error`() {
    enterCommand("call flattennew({'key': 'value'})")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of flattennew() must be a List")
  }

  @Test
  fun `test flattennew with nested List`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]])", "[1, 2, 3, 4, 5]")
  }

  @Test
  fun `test flattennew does not modify original List`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("let b = a")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 3, 4, 5]")
    assertCommandOutput("echo a", "[1, [2, 3], [4, [5]]]")
    assertCommandOutput("echo b", "[1, [2, 3], [4, [5]]]")
  }

  @Test
  fun `test flattennew does not modify nested Lists`() {
    enterCommand("let b = [1, [2, 3], [4, [5]]]")
    enterCommand("let c = [6, [7, 8], [9, [10]]]")
    enterCommand("let a = [b, c]")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]")
    assertCommandOutput("echo a", "[[1, [2, 3], [4, [5]]], [6, [7, 8], [9, [10]]]]")
    assertCommandOutput("echo b", "[1, [2, 3], [4, [5]]]")
    assertCommandOutput("echo c", "[6, [7, 8], [9, [10]]]")
  }

  @Test
  fun `test flattennew with already flat List`() {
    assertCommandOutput("echo flattennew([1, 2, 3])", "[1, 2, 3]")
  }

  @Test
  fun `test flattennew with empty List`() {
    assertCommandOutput("echo flattennew([])", "[]")
  }

  @Test
  fun `test flattennew with recursive List`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let a[2] = a")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flattennew with nested recursive List`() {
    enterCommand("let a = [[1, 2], 3, [4, 5]]")
    enterCommand("let a[1] = a")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flattennew with indirectly recursive List`() {
    enterCommand("let a = [1, [2, 3, 4], 5]")
    enterCommand("let a[1][1] = a")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 1, 2, 4, 5, 4, 5]")
  }

  @Test
  fun `test flattennew on a locked List reports error`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("lockvar a")
    enterCommand("call flattennew(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: flattennew() argument")
  }

  @Test
  fun `test flattennew on a locked List item does nor report error`() {
    enterCommand("let a = [1, [2, 3], [4, [5]]]")
    enterCommand("lockvar a[1]")
    assertCommandOutput("echo flattennew(a)", "[1, 2, 3, 4, 5]")
    assertPluginError(false)
  }

  @Test
  fun `test flattennew with invalid depth argument reports error`() {
    enterCommand("call flattennew([1, [2, 3]], 12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @Test
  fun `test flattennew with negative depth argument reports error`() {
    enterCommand("call flattennew([1, [2, 3]], -1)")
    assertPluginError(true)
    assertPluginErrorMessage("E900: maxdepth must be non-negative")
  }

  @Test
  fun `test flattennew with max depth 0 does not flatten List`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]], 0)", "[1, [2, 3], [4, [5]]]")
  }

  @Test
  fun `test flattennew with max depth 1 only flattens first depth`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]], 1)", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flattennew with String max depth 1 only flattens first depth`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]], '1')", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flattennew with String max depth 1 with trailing characters flattens first depth`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]], '1foo')", "[1, 2, 3, 4, [5]]")
  }

  @Test
  fun `test flattennew with max depth 2 flattens deeper`() {
    assertCommandOutput("echo flattennew([1, [2, 3], [4, [5]]], 2)", "[1, 2, 3, 4, 5]")
  }
}
