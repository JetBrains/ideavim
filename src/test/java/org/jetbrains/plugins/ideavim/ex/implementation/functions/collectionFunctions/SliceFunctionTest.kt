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

class SliceFunctionTest : VimTestCase("\n") {
  @Test
  fun `test slice on Float returns 0`() {
    // This isn't documented. I think Vim is returning back an empty value, which is assumed to be 0
    assertCommandOutput("echo string(slice(1.2, 2, 3))", "0")
  }

  @Test
  fun `test slice on Dictionary returns self`() {
    // Who knows why?
    assertCommandOutput("echo slice({'key1': 1, 'key2': 2}, 1)", "{'key1': 1, 'key2': 2}")
  }

  @Test
  fun `test slice on List returns sublist from start`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 2))", "[3, 4, 5]")
  }

  @Test
  fun `test slice on empty List returns empty List`() {
    assertCommandOutput("echo string(slice([], 2))", "[]")
  }

  @Test
  fun `test slice on List with invalid start index returns empty List`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 8))", "[]")
  }

  @Test
  fun `test slice on List with negative start index returns sublist starting from end offset`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], -2))", "[4, 5]")
  }

  @Test
  fun `test slice on List with start and end index treats end index as exclusive`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 1, 3))", "[2, 3]")
  }

  @Test
  fun `test slice on List with end index before start index returns empty List`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 2, 1))", "[]")
  }

  @Test
  fun `test slice on List with invalid end index returns sublist from start to end`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 1, 8))", "[2, 3, 4, 5]")
  }

  @Test
  fun `test slice on List with negative end index returns sublist from start to end offset`() {
    assertCommandOutput("echo string(slice([1, 2, 3, 4, 5], 1, -2))", "[2, 3]")
  }

  @Test
  fun `test slice on List called as method`() {
    assertCommandOutput("echo [1, 2, 3, 4, 5]->slice(2,4)", "[3, 4]")
  }

  @Test
  fun `test slice on String returns sublist from start`() {
    assertCommandOutput("echo string(slice('abcdef', 2))", "'cdef'")
  }

  @Test
  fun `test slice on empty String returns empty String`() {
    assertCommandOutput("echo string(slice('', 2))", "''")
  }

  @Test
  fun `test slice on String with invalid start index returns empty String`() {
    assertCommandOutput("echo string(slice('abcdef', 8))", "''")
  }

  @Test
  fun `test slice on String with negative start index returns sublist starting from end offset`() {
    assertCommandOutput("echo string(slice('abcdef', -2))", "'ef'")
  }

  @Test
  fun `test slice on String with start and end index treats end index as exclusive`() {
    assertCommandOutput("echo string(slice('abcdef', 1, 3))", "'bc'")
  }

  @Test
  fun `test slice on String with end index before start index returns empty String`() {
    assertCommandOutput("echo string(slice('abcdef', 2, 1))", "''")
  }

  @Test
  fun `test slice on String with invalid end index returns substring from start to end`() {
    assertCommandOutput("echo string(slice('abcdef', 1, 8))", "'bcdef'")
  }

  @Test
  fun `test slice on String with negative end index returns substring from start to end offset`() {
    assertCommandOutput("echo string(slice('abcdef', 1, -2))", "'bcd'")
  }

  @Test
  fun `test slice on String called as method`() {
    assertCommandOutput("echo string('abcdef'->slice(2,4))", "'cd'")
  }

  @Test
  fun `test slice on Number returns slice of string representation of Number`() {
    assertCommandOutput("echo string(slice(1459, 1))", "'459'")
  }
}
