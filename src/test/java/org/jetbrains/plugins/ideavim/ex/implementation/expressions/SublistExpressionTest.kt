/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SublistExpressionTest : VimTestCase("\n") {
  @Test
  fun `test sublist of String`() {
    assertCommandOutput("echo string('abc'[0:1])", "'ab'")
  }

  @Test
  fun `test sublist of List`() {
    assertCommandOutput("echo [1, 2, 3][0:1]", "[1, 2]")
  }

  @Test
  fun `test sublist is a copy of the original List`() {
    enterCommand("let l = [1, 2, 3, 4, 5]")
    enterCommand("let s = l[1:3]")
    enterCommand("let l[2] = 9")
    assertCommandOutput("echo l", "[1, 2, 9, 4, 5]")
    assertCommandOutput("echo s", "[2, 3, 4]")
  }

  @Test
  fun `test single item sublist of List`() {
    assertCommandOutput("echo [1, 2, 3][1:1]", "[2]")
  }

  @Test
  fun `test sublist treats Number as String`() {
    assertCommandOutput("echo string(123456789[0:3])", "'1234'")
  }

  @Test
  fun `test sublist of Dictionary raises error`() {
    enterCommand("let dict = {'a': 1, 'b': 2}")
    enterCommand("echo dict[0:1]")
    assertPluginError(true)
    assertPluginErrorMessage("E719: Cannot slice a Dictionary")
  }

  @Test
  fun `test sublist of Float raises error`() {
    enterCommand("echo 1.2[0:1]")
    assertPluginError(true)
    assertPluginErrorMessage("E806: Using a Float as a String")
  }

  @Test
  fun `test sublist with missing start index treated as zero`() {
    assertCommandOutput("echo [1, 2, 3][:1]", "[1, 2]")
  }

  @Test
  fun `test sublist missing end index treated as last item in List`() {
    assertCommandOutput("echo [1,2,3,4,5][2:]", "[3, 4, 5]")
  }

  @Test
  fun `test sublist with negative start index counts from end of List`() {
    assertCommandOutput("echo [1,2,3,4,5][-3:]", "[3, 4, 5]")
  }

  @Test
  fun `test sublist with negative end index counts from end of List`() {
    assertCommandOutput("echo [1,2,3,4,5][1:-2]", "[2, 3, 4]")
  }

  @Test
  fun `test sublist with end index larger than List length treated as List item count`() {
    assertCommandOutput("echo [1,2,3,4,5][1:10]", "[2, 3, 4, 5]")
  }

  @Test
  fun `test sublist with end index larger than String length treated as String length`() {
    @Suppress("SpellCheckingInspection")
    assertCommandOutput("echo 'abcde'[1:10]", "bcde")
  }

  @Test
  fun `test sublist with out of range negative start index returns empty List`() {
    assertCommandOutput("echo [1,2,3,4,5][-10:2]", "[]")
  }

  @Test
  fun `test sublist with out of range negative start index returns empty String`() {
    // Should be 'abc'!?!?!
    assertCommandOutput("echo string('abcde'[-10:2])", "''")
  }

  @Test
  fun `test sublist with out of range positive start index returns empty List`() {
    assertCommandOutput("echo [1,2,3,4,5][10:]", "[]")
  }

  @Test
  fun `test sublist with out of range positive start index returns empty String`() {
    assertCommandOutput("echo string('abcdefgh'[10:])", "''")
  }

  @Test
  fun `test sublist with out of range negative end index returns empty List`() {
    assertCommandOutput("echo [1,2,3,4,5][:-10]", "[]")
  }

  @Test
  fun `test sublist with out of range negative end index returns empty String`() {
    assertCommandOutput("echo string('abcdefgh'[:-10])", "''")
  }

  @Test
  fun `test sublist with out of order range returns empty List`() {
    assertCommandOutput("echo [1,2,3][100:10]", "[]")
  }

  @Test
  fun `test sublist with out of order range returns empty String`() {
    assertCommandOutput("echo string('abc'[100:10])", "''")
  }
}
