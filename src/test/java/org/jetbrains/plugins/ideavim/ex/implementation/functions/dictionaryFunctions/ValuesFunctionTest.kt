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

class ValuesFunctionTest : VimTestCase("\n") {
  @Test
  fun `test values on String reports error`() {
    enterCommand("echo values('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test values on Number reports error`() {
    enterCommand("echo values(12)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test values on Float reports error`() {
    enterCommand("echo values(4.3)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test values on List reports error`() {
    enterCommand("echo values([1,2,3])")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 1")
  }

  @Test
  fun `test values on Dictionary returns List of values`() {
    enterCommand("let myDict = {'key1': 10, 'key2': 4.3, 'key3': 'string'}")
    assertCommandOutput("echo values(myDict)", "[10, 4.3, 'string']")
  }

  @Test
  fun `test values on empty Dictionary returns empty List of values`() {
    enterCommand("let myDict = {}")
    assertCommandOutput("echo values(myDict)", "[]")
  }

  @Test
  fun `test values called as method`() {
    enterCommand("let myDict = {'key1': 10, 'key2': 4.3, 'key3': 'string'}")
    assertCommandOutput("echo myDict->values()", "[10, 4.3, 'string']")
  }
}
