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

class RemoveFunctionTest : VimTestCase("\n") {
  @Test
  fun `test remove with String reports error`() {
    enterCommand("echo remove('string', 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E896: Argument of remove() must be a List, Dictionary or Blob")
  }

  @Test
  fun `test remove with Number reports error`() {
    enterCommand("echo remove(2, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E896: Argument of remove() must be a List, Dictionary or Blob")
  }

  @Test
  fun `test remove with Float reports error`() {
    enterCommand("echo remove(2.3, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E896: Argument of remove() must be a List, Dictionary or Blob")
  }

  // List
  @Test
  fun `test remove item from List modifies original list`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("call remove(a, 1)")
    assertCommandOutput("echo a", "[1, 3]")
  }

  @Test
  fun `test remove item from List returns removed item`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo remove(a, 1)", "2")
  }

  @Test
  fun `test remove item from List called as method`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("call a->remove(1)")
    assertCommandOutput("echo a", "[1, 3]")
  }

  @Test
  fun `test remove item from List with index 0`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo remove(a, 0)", "1")
  }

  @Test
  fun `test remove item from List with negative index removes from end of List`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo remove(a, -1)", "3")
    assertCommandOutput("echo a", "[1, 2]")
  }

  @Test
  fun `test remove item from List with index greater than List length`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("echo remove(a, 4)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 4")
  }

  @Test
  fun `test remove item from List with negative index greater than List length`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("echo remove(a, -4)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: -4")
  }

  @Test
  fun `test remove item range from List modifies original list`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("call remove(a, 1, 3)")
    assertCommandOutput("echo a", "[1, 5]")
  }

  @Test
  fun `test remove item range from List returns removed items as list`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo remove(a, 1, 3)", "[2, 3, 4]")
  }

  @Test
  fun `test remove item range from List with negative index is relative to end of list`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo remove(a, 1, -2)", "[2, 3, 4]")
  }

  @Test
  fun `test remove item range from List with single item range`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo remove(a, 1, 1)", "[2]")
  }

  @Test
  fun `test remove item range from List with single item range 2`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    assertCommandOutput("echo remove(a, 0, 0)", "[1]")
  }

  @Test
  fun `test remove item range from List with negative index greater than List length`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("echo remove(a, 1, -6)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: -6")
  }

  @Test
  fun `test remove item range from List with second index less than first index reports invalid range`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("echo remove(a, 3, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E16: Invalid range")
  }

  @Test
  fun `test remove item range from List with negative second index less than first index reports invalid range`() {
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("echo remove(a, 3, -4)")
    assertPluginError(true)
    assertPluginErrorMessage("E16: Invalid range")
  }

  @Test
  fun `test remove can remove locked list item`() {
    // This is unexpected, but it's Vim behaviour!
    enterCommand("let a = [1, 2, 3, 4, 5]")
    enterCommand("lockvar a[2]")
    enterCommand("call remove(a, 2)")
    assertPluginError(false)
    assertCommandOutput("echo a", "[1, 2, 4, 5]")
  }

  @Test
  fun `test remove dictionary item`() {
    enterCommand("let dict = {'a': 1, 'b': 2}")
    enterCommand("call remove(dict, 'a')")
    assertCommandOutput("echo dict", "{'b': 2}")
  }

  @Test
  fun `test remove dictionary item returns removed item`() {
    enterCommand("let dict = {'a': 42, 'b': 2}")
    assertCommandOutput("echo remove(dict, 'a')", "42")
  }

  @Test
  fun `test call remove on dictionary with end index reports error`() {
    enterCommand("let dict = {'a': 42, 'b': 2}")
    enterCommand("echo remove(dict, 'a', 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: remove()")
  }

  @Test
  fun `test remove invalid dictionary item reports error`() {
    enterCommand("let dict = {'a': 42, 'b': 2}")
    enterCommand("echo remove(dict, 'c')")
    assertPluginError(true)
    assertPluginErrorMessage("E716: Key not present in Dictionary: \"c\"")
  }

  @Test
  fun `test remove can remove locked dictionary item`() {
    // This is unexpected, but it's Vim behaviour!
    enterCommand("let dict = {'a': 1, 'b': 2}")
    enterCommand("lockvar dict.a")
    enterCommand("call remove(dict, 'a')")
    assertPluginError(false)
    assertCommandOutput("echo dict", "{'b': 2}")
  }

  @Test
  fun `test remove called as method on dictionary`() {
    enterCommand("let dict = {'a': 1, 'b': 2}")
    enterCommand("call dict->remove('a')")
    assertCommandOutput("echo dict", "{'b': 2}")
  }
}
