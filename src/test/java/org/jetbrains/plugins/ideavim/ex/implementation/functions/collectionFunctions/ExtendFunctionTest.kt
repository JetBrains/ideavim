/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.collectionFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ExtendFunctionTest : VimTestCase("\n") {
  @Test
  fun `test extend with first argument of String reports error`() {
    enterCommand("echo extend('string', 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend with first argument of Number reports error`() {
    enterCommand("echo extend(1, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend with first argument of Float reports error`() {
    enterCommand("echo extend(1.23, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend with second argument of String reports error`() {
    enterCommand("echo extend([1,2,3], 'string')")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend with second argument of Number reports error`() {
    enterCommand("echo extend([1,2,3], 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend with second argument of Float reports error`() {
    enterCommand("echo extend([1,2,3], 1.23)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extend() must be a List or Dictionary")
  }

  @Test
  fun `test extend List with List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo extend(a, b)", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extend List modifies existing List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let x = a")
    enterCommand("let b = [3, 4]")
    enterCommand("call extend(a, b)")
    assertCommandOutput("echo x", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extend empty List with empty list`() {
    enterCommand("let a = []")
    enterCommand("let b = []")
    assertCommandOutput("echo extend(a, b)", "[]")
  }

  @Test
  fun `test extend empty List with List`() {
    enterCommand("let a = []")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo extend(a, b)", "[3, 4]")
  }

  @Test
  fun `test extend List with empty List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = []")
    assertCommandOutput("echo extend(a, b)", "[1, 2]")
  }

  @Test
  fun `test extend List with List called as method`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo a->extend(b)", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extend List at index`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let b = [6, 7, 8, 9, 0]")
    assertCommandOutput("echo extend(a, b, 3)", "[1, 2, 3, 6, 7, 8, 9, 0, 4, 5]")
  }

  @Test
  fun `test extend List at index 0`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let b = [6, 7, 8, 9, 0]")
    assertCommandOutput("echo extend(a, b, 0)", "[6, 7, 8, 9, 0, 1, 2, 3, 4, 5]")
  }

  @Test
  fun `test extend List with the same List`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo extend(a, a)", "[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]")
  }

  @Test
  fun `test extend List with the same List and index`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo extend(a, a, 1)", "[1, 1, 2, 3, 4, 5, 2, 3, 4, 5]")
  }

  @Test
  fun `test extend locked List reports error`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("lockvar a")
    enterCommand("call extend(a, [6, 7, 8, 9, 0])")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: extend() argument")
  }

  @Test
  fun `test extend Dictionary with Dictionary`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    assertCommandOutput("echo extend(a, b)", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extend Dictionary with Dictionary called as method`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    assertCommandOutput("echo a->extend(b)", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extend Dictionary modifies existing Dictionary`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let x = a")
    enterCommand("let b = {'c': 3, 'd': 4}")
    enterCommand("call extend(a, b)")
    assertCommandOutput("echo x", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extend Dictionary overwrites existing keys by default`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extend(a, b)", "{'a': 3, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extend Dictionary overwrites existing keys when passing force`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extend(a, b, 'force')", "{'a': 3, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extend Dictionary keeps existing keys when passing keep`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extend(a, b, 'keep')", "{'a': 1, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extend Dictionary reports error when trying to overwrite existing keys when passing error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    enterCommand("call extend(a, b, 'error')")
    assertPluginError(true)
    assertPluginErrorMessage("E737: Key already exists: a")
  }

  @Test
  fun `test extend Dictionary with Dictionary reports error with invalid additional argument`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    enterCommand("call extend(a, b, 'something')")
    assertPluginError(true)
    assertPluginErrorMessage("E475: Invalid argument: something")
  }

  @Test
  fun `test extend locked Dictionary reports error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("lockvar a")
    enterCommand("call extend(a, {'c': 3, 'd': 4})")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: extend() argument")
  }

  @Disabled("IdeaVim cannot lock a value in a Dictionary")
  @Test
  fun `test extend Dictionary with locked item reports error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'd': 4}")
//    enterCommand("lockvar a.a") // TODO: Support this syntax
    enterCommand("lockvar a['a']")
    enterCommand("call extend(a, b)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: extend() argument")
  }
}
