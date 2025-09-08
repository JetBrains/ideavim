/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.dictionaryFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class HasKeyFunctionTest : VimTestCase("\n") {
  @Test
  fun `test has_key returns true if key exists`() {
    enterCommand("let myDict = {'a': 1, 'b': 2}")
    assertCommandOutput("echo has_key(myDict, 'a')", "1")
  }

  @Test
  fun `test has_key returns false if key does not exist`() {
    enterCommand("let myDict = {'a': 1, 'b': 2}")
    assertCommandOutput("echo has_key(myDict, 'z')", "0")
  }

  @Test
  fun `test has_key with non-dictionary argument raises error`() {
    enterCommand("echo has_key(1, 'a')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test has_key with Number key argument is coerced to String`() {
    enterCommand("let myDict = {'1': 10, '2': 20}")
    assertCommandOutput("echo has_key(myDict, 1)", "1")
  }

  // Surprising coercion!
  @Test
  fun `test has_key with Float key argument is coerced to String`() {
    enterCommand("let myDict = {'1.2': 10, '2.0': 20}")
    assertCommandOutput("echo has_key(myDict, 1.2)", "1")
  }

  @Test
  fun `test has_key called as method`() {
    enterCommand("let myDict = {'a': 1, 'b': 2}")
    assertCommandOutput("echo myDict->has_key('a')", "1")
  }
}
