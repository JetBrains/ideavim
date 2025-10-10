/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ExtendNewFunctionTest : VimTestCase("\n") {
  @Test
  fun `test extendnew with first argument of String reports error`() {
    enterCommand("echo extendnew('string', 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew with first argument of Number reports error`() {
    enterCommand("echo extendnew(1, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew with first argument of Float reports error`() {
    enterCommand("echo extendnew(1.23, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew with second argument of String reports error`() {
    enterCommand("echo extendnew([1,2,3], 'string')")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew with second argument of Number reports error`() {
    enterCommand("echo extendnew([1,2,3], 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew with second argument of Float reports error`() {
    enterCommand("echo extendnew([1,2,3], 1.23)")
    assertPluginError(true)
    assertPluginErrorMessage("E712: Argument of extendnew() must be a List or Dictionary")
  }

  @Test
  fun `test extendnew List with List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo extendnew(a, b)", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extendnew List does not modify existing List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = [3, 4]")
    enterCommand("let x = extendnew(a, b)")
    assertCommandOutput("echo a", "[1, 2]")
    assertCommandOutput("echo x", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extendnew empty List with empty list`() {
    enterCommand("let a = []")
    enterCommand("let b = []")
    assertCommandOutput("echo extendnew(a, b)", "[]")
  }

  @Test
  fun `test extendnew empty List with List`() {
    enterCommand("let a = []")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo extendnew(a, b)", "[3, 4]")
  }

  @Test
  fun `test extendnew List with empty List`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = []")
    assertCommandOutput("echo extendnew(a, b)", "[1, 2]")
  }

  @Test
  fun `test extendnew List with List called as method`() {
    enterCommand("let a = [1, 2]")
    enterCommand("let b = [3, 4]")
    assertCommandOutput("echo a->extendnew(b)", "[1, 2, 3, 4]")
  }

  @Test
  fun `test extendnew List at index`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let b = [6, 7, 8, 9, 0]")
    assertCommandOutput("echo extendnew(a, b, 3)", "[1, 2, 3, 6, 7, 8, 9, 0, 4, 5]")
  }

  @Test
  fun `test extendnew List at index 0`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("let b = [6, 7, 8, 9, 0]")
    assertCommandOutput("echo extendnew(a, b, 0)", "[6, 7, 8, 9, 0, 1, 2, 3, 4, 5]")
  }

  @Test
  fun `test extendnew List with the same List`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo extendnew(a, a)", "[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]")
  }

  @Test
  fun `test extendnew List with the same List and index`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo extendnew(a, a, 1)", "[1, 1, 2, 3, 4, 5, 2, 3, 4, 5]")
  }

  @Test
  fun `test extendnew with locked List does not report error`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("lockvar a")
    enterCommand("let x = extendnew(a, [6, 7, 8, 9, 0])")
    assertPluginError(false)
    assertCommandOutput("echo x", "[1, 2, 3, 4, 5, 6, 7, 8, 9, 0]")
  }

  @Test
  fun `test extendnew Dictionary with Dictionary`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    assertCommandOutput("echo extendnew(a, b)", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extendnew Dictionary with Dictionary called as method`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    assertCommandOutput("echo a->extendnew(b)", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extendnew Dictionary does not modify existing Dictionary`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    enterCommand("let x = extendnew(a, b)")
    assertCommandOutput("echo a", "{'a': 1, 'b': 2}")
    assertCommandOutput("echo x", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Test
  fun `test extendnew Dictionary overwrites existing keys by default`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extendnew(a, b)", "{'a': 3, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extendnew Dictionary overwrites existing keys when passing force`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extendnew(a, b, 'force')", "{'a': 3, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extendnew Dictionary keeps existing keys when passing keep`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    assertCommandOutput("echo extendnew(a, b, 'keep')", "{'a': 1, 'b': 2, 'c': 4}")
  }

  @Test
  fun `test extendnew Dictionary reports error when trying to overwrite existing keys when passing error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'c': 4}")
    enterCommand("call extendnew(a, b, 'error')")
    assertPluginError(true)
    assertPluginErrorMessage("E737: Key already exists: a")
  }

  @Test
  fun `test extendnew Dictionary with Dictionary reports error with invalid additional argument`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'c': 3, 'd': 4}")
    enterCommand("call extendnew(a, b, 'something')")
    assertPluginError(true)
    assertPluginErrorMessage("E475: Invalid argument: something")
  }

  @Test
  fun `test extendnew locked Dictionary does not report error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("lockvar a")
    enterCommand("let x = extendnew(a, {'c': 3, 'd': 4})")
    assertPluginError(false)
    assertCommandOutput("echo x", "{'a': 1, 'b': 2, 'c': 3, 'd': 4}")
  }

  @Disabled("IdeaVim cannot lock a value in a Dictionary")
  @Test
  fun `test extendnew Dictionary with locked item reports error`() {
    enterCommand("let a = {'a': 1, 'b': 2}")
    enterCommand("let b = {'a': 3, 'd': 4}")
//    enterCommand("lockvar a.a") // TODO: Support this syntax
    enterCommand("lockvar a['a']")
    enterCommand("call extendnew(a, b)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: extendnew() argument")
  }
}
