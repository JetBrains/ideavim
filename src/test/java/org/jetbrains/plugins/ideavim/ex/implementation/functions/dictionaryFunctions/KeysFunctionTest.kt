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

class KeysFunctionTest : VimTestCase("\n") {
  @Test
  fun `test keys on String reports error`() {
    enterCommand("echo keys('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test keys on Number reports error`() {
    enterCommand("echo keys(12)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test keys on Float reports error`() {
    enterCommand("echo keys(2.3)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test keys on List reports error`() {
    enterCommand("echo keys([1,2,3])")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test keys on Dictionary returns List of keys`() {
    enterCommand("let myDict = {'key1': 10, 'key2': 20}")
    assertCommandOutput("echo keys(myDict)", "['key1', 'key2']")
  }

  @Test
  fun `test keys on empty Dictionary returns empty List of keys`() {
    enterCommand("let myDict = {}")
    assertCommandOutput("echo keys(myDict)", "[]")
  }

  @Test
  fun `test keys called as method`() {
    enterCommand("let myDict = {'key1': 10, 'key2': 20}")
    assertCommandOutput("echo myDict->keys()", "['key1', 'key2']")
  }
}
