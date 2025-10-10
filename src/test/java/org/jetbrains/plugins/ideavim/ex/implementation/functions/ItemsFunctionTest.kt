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

class ItemsFunctionTest : VimTestCase("\n") {
  @Test
  fun `test items on Number reports error`() {
    enterCommand("echo items(1)")
    assertPluginError(true)
    assertPluginErrorMessage("E1225: String, List or Dictionary required for argument 1")
  }

  @Test
  fun `test items on Float reports error`() {
    enterCommand("echo items(1.2)")
    assertPluginError(true)
    assertPluginErrorMessage("E1225: String, List or Dictionary required for argument 1")
  }

  @Test
  fun `test items on Dictionary returns List of key value pairs`() {
    assertCommandOutput("echo items({'key1': 2, 'key2': 4})", "[['key1', 2], ['key2', 4]]")
  }

  @Test
  fun `test items on empty Dictionary returns empty List of key value pairs`() {
    assertCommandOutput("echo items({})", "[]")
  }

  @Test
  fun `test items called as method on Dictionary`() {
    assertCommandOutput("echo {'key1': 2, 'key2': 4}->items()", "[['key1', 2], ['key2', 4]]")
  }

  @Test
  fun `test items on List returns List of indices and values`() {
    assertCommandOutput("echo items(['a', 'b', 'c'])", "[[0, 'a'], [1, 'b'], [2, 'c']]")
  }

  @Test
  fun `test items on empty List returns empty List of indices and values`() {
    assertCommandOutput("echo items([])", "[]")
  }

  @Test
  fun `test items called as method on List`() {
    assertCommandOutput("echo ['a', 'b', 'c']->items()", "[[0, 'a'], [1, 'b'], [2, 'c']]")
  }

  @Test
  fun `test items on String returns List of indices and characters`() {
    assertCommandOutput("echo items('abc')", "[[0, 'a'], [1, 'b'], [2, 'c']]")
  }

  @Test
  fun `test items on empty String returns empty List of indices and characters`() {
    assertCommandOutput("echo items('')", "[]")
  }

  @Test
  fun `test items called as method on String`() {
    assertCommandOutput("echo 'abc'->items()", "[[0, 'a'], [1, 'b'], [2, 'c']]")
  }
}
