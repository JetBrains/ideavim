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

class ReverseFunctionTest : VimTestCase("\n") {
  @Test
  fun `test reverse with Number reports error`() {
    enterCommand("echo reverse(12)")
    assertPluginError(true)
    assertPluginErrorMessage("E1252: String, List or Blob required for argument 1")
  }

  @Test
  fun `test reverse with Float reports error`() {
    enterCommand("echo reverse(1.23)")
    assertPluginError(true)
    assertPluginErrorMessage("E1252: String, List or Blob required for argument 1")
  }

  @Test
  fun `test reverse with Dictionary reports error`() {
    enterCommand("echo reverse({'key1': 1, 'key2': 2})")
    assertPluginError(true)
    assertPluginErrorMessage("E1252: String, List or Blob required for argument 1")
  }

  @Test
  fun `test reverse List`() {
    assertCommandOutput("echo reverse([1, 2, 3])", "[3, 2, 1]")
  }

  @Test
  fun `test reverse String list`() {
    assertCommandOutput("echo reverse(['a', 'b', 'c'])", "['c', 'b', 'a']")
  }

  @Test
  fun `test reverse single element`() {
    assertCommandOutput("echo reverse([1])", "[1]")
  }

  @Test
  fun `test reverse empty List`() {
    assertCommandOutput("echo reverse([])", "[]")
  }

  @Test
  fun `test reverse modifies original List`() {
    enterCommand("let mylist = [1, 2, 3]")
    enterCommand("call reverse(mylist)")
    assertCommandOutput("echo mylist", "[3, 2, 1]")
  }

  @Test
  fun `test reverse List with mixed types`() {
    assertCommandOutput("echo reverse([1, 'two', 3])", "[3, 'two', 1]")
  }

  @Test
  fun `test reverse locked List reports errors`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar a")
    enterCommand("call reverse(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: reverse() argument")
  }

  @Test
  fun `test reverse List with locked item does not report error`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar a[1]")
    assertCommandOutput("echo reverse(a)", "[3, 2, 1]")
    assertPluginError(false)
  }

  @Test
  fun `test reverse with String`() {
    assertCommandOutput("echo string(reverse('hello'))", "'olleh'")
  }

  @Test
  fun `test reverse with empty String`() {
    assertCommandOutput("echo string(reverse(''))", "''")
  }

  @Test
  fun `test reverse with single character String`() {
    assertCommandOutput("echo string(reverse('a'))", "'a'")
  }

  @Test
  fun `test reverse String does not modify original String`() {
    enterCommand("let a = 'abc'")
    assertCommandOutput("echo string(reverse(a))", "'cba'")
    assertCommandOutput("echo string(a)", "'abc'")
  }

  @Test
  fun `test reverse locked String reports errors`() {
    enterCommand("let a = 'abc'")
    enterCommand("lockvar a")
    enterCommand("call reverse(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: reverse() argument")
  }
}
