/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CopyFunctionTest : VimTestCase("\n") {
  @Test
  fun `test copy Number returns same value`() {
    assertCommandOutput("echo copy(12)", "12")
    assertCommandOutput("echo 12 is 12->copy()", "1")
  }

  @Test
  fun `test copy Float returns same value`() {
    assertCommandOutput("echo copy(12.23)", "12.23")
    assertCommandOutput("echo 12.23 is 12.23->copy()", "1")
  }

  @Test
  fun `test copy String returns same value`() {
    assertCommandOutput("echo copy('hello')", "hello")
    assertCommandOutput("echo 'hello' is 'hello'->copy()", "1")
  }

  @Test
  fun `test copy List returns new instance with same items`() {
    enterCommand("let a = [1, [2], 3]")
    assertCommandOutput("echo copy(a)", "[1, [2], 3]")
    assertCommandOutput("echo a is a->copy()", "0")
    assertCommandOutput("echo a[0] is a->copy()[0]", "1")
    enterCommand("let b = copy(a)")
    enterCommand("let a[0] = 9")
    assertCommandOutput("echo a b", "[9, [2], 3] [1, [2], 3]")
    enterCommand("let a[1][0] = 9")
    assertCommandOutput("echo a b", "[9, [9], 3] [1, [9], 3]")
  }

  @Test
  fun `test copy Dictionary returns new instance with same items`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    assertCommandOutput("echo copy(a)", "{'foo': [1], 'bar': 2}")
    assertCommandOutput("echo a is a->copy()", "0")
    assertCommandOutput("echo a['foo'] is a->copy()['foo']", "1")
    enterCommand("let b = copy(a)")
    enterCommand("let a['bar'] = 9")
    assertCommandOutput("echo a b", "{'foo': [1], 'bar': 9} {'foo': [1], 'bar': 2}")
    enterCommand("let a['foo'][0] = 9")
    assertCommandOutput("echo a b", "{'foo': [9], 'bar': 9} {'foo': [9], 'bar': 2}")
  }

  @Test
  fun `test copy List with recursive structure only creates new instance for top level List`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    enterCommand("let b = copy(a)")
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b[1] is a", "1")
  }

  @Test
  fun `test copy Dictionary with recursive structure only creates new instance for top level Dictionary`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    enterCommand("let a['foo'] = a")
    enterCommand("let b = copy(a)")
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b['foo'] is a", "1")
  }
}
