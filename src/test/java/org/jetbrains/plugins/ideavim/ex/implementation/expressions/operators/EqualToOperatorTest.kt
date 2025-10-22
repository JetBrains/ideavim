/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class EqualToOperatorTest : VimTestCase("\n") {
  @Test
  fun `test equal to with Number values`() {
    assertCommandOutput("echo 1 == 1", "1")
    assertCommandOutput("echo 1 == 2", "0")
  }

  @Test
  fun `test equal to with Float values`() {
    assertCommandOutput("echo 1.0 == 1.0", "1")
    assertCommandOutput("echo 1.0 == 2.0", "0")
  }

  @Test
  fun `test equal to with mixed Float and Number values compares as Float`() {
    assertCommandOutput("echo 1.0 == 1", "1")
    assertCommandOutput("echo 1 == 1.0", "1")
    assertCommandOutput("echo 2.0 == 1", "0")
    assertCommandOutput("echo 2 == 1.0", "0")
    assertCommandOutput("echo 1 == 1.5", "0")
  }

  @Test
  fun `test equal to with String values depends on 'ignorecase' option`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo 'abc' == 'abc'", "1")
    assertCommandOutput("echo 'abc' == 'def'", "0")
    assertCommandOutput("echo 'abc' == 'ABC'", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo 'abc' == 'ABC'", "1")
  }

  @Test
  fun `test case sensitive equal to with String values`() {
    assertCommandOutput("echo 'abc' ==# 'abc'", "1")
    assertCommandOutput("echo 'abc' ==# 'ABC'", "0")
    assertCommandOutput("echo 'abc' ==# 'def'", "0")
  }

  @Test
  fun `test case insensitive equal to with String values`() {
    assertCommandOutput("echo 'abc' ==? 'abc'", "1")
    assertCommandOutput("echo 'abc' ==? 'ABC'", "1")
    assertCommandOutput("echo 'abc' ==? 'def'", "0")
  }

  @Test
  fun `test equal to with mixed String and Number values`() {
    assertCommandOutput("echo '1' == 1", "1")
    assertCommandOutput("echo 1 == '1'", "1")
    assertCommandOutput("echo '1' == 2", "0")
    assertCommandOutput("echo 'abc' == 1", "0")
    assertCommandOutput("echo '1abc' == 1", "0")
  }

  @Test
  fun `test equal to with mixed String and Float values reports error`() {
    enterCommand("echo '1.0' == 1.0")
    assertPluginError(true)
    assertPluginErrorMessage("E892: Using a String as a Float")
  }

  @Test
  fun `test equal to with mixed String and Float values reports error 2`() {
    enterCommand("echo 1.0 == '1.0'")
    assertPluginError(true)
    assertPluginErrorMessage("E892: Using a String as a Float")
  }

  @Test
  fun `test equal to with List and non-List values`() {
    enterCommand("echo [1, 2, 3] == 'abc'")
    assertPluginError(true)
    assertPluginErrorMessage("E691: Can only compare List with List")
  }

  @Test
  fun `test equal to with List and non-List values 2`() {
    enterCommand("echo 3 == [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E691: Can only compare List with List")
  }

  @Test
  fun `test equal to with simple List`() {
    assertCommandOutput("echo ['abc', 2, 3.14] == ['abc', 2, 3.14]", "1")
    assertCommandOutput("echo ['abc', 2, 3.14] == ['abc', 2, 3.15]", "0")
  }

  @Test
  fun `test equal to with same List reference`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = a")
    assertCommandOutput("echo a == b", "1")
  }

  @Test
  fun `test equal to with empty Lists`() {
    assertCommandOutput("echo [] == []", "1")
  }

  @Test
  fun `test equal to requires List items to be of same type`() {
    // Vim coerces simple types for equality but does not do the same coercion for list items
    assertCommandOutput("echo [1.0] == [1]", "0") // `echo 1.0 == 1` => 1!
    assertCommandOutput("echo ['1'] == [1]", "0") // `echo '1' == 1` => 1!
  }

  @Test
  fun `test equal to with List of String values depends on 'ignorecase'`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo ['abc', 'DEF'] == ['abc', 'DEF']", "1")
    assertCommandOutput("echo ['abc', 'DEF'] == ['ABC', 'def']", "0")
    assertCommandOutput("echo ['foo', 'bar'] == ['ABC', 'def']", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo ['abc', 'DEF'] == ['ABC', 'def']", "1")
  }

  @Test
  fun `test equal to case sensitive with List of String values`() {
    assertCommandOutput("echo ['abc', 'DEF'] ==# ['abc', 'DEF']", "1")
    assertCommandOutput("echo ['abc', 'DEF'] ==# ['ABC', 'def']", "0")
    assertCommandOutput("echo ['foo', 'bar'] ==# ['ABC', 'def']", "0")
  }

  @Test
  fun `test equal to case insensitive with List of String values`() {
    assertCommandOutput("echo ['abc', 'DEF'] ==? ['abc', 'DEF']", "1")
    assertCommandOutput("echo ['abc', 'DEF'] ==? ['ABC', 'def']", "1")
    assertCommandOutput("echo ['foo', 'bar'] ==? ['ABC', 'def']", "0")
  }

  @Test
  fun `test equal to with nested Lists`() {
    assertCommandOutput("echo [1, [9, 8, 7], 3] == [1, [9, 8, 7], 3]", "1")
    assertCommandOutput("echo [1, [9, 8, 7], 3] == [1, ['a', 'b', 'c'], 3]", "0")
  }

  @Test
  fun `test equal to with nested List reference`() {
    enterCommand("let a = ['a', 'b', 'c']")
    enterCommand("let b = ['a', 'b', 'c']")
    assertCommandOutput("echo [1, a, 3] == [1, a, 3]", "1")
    assertCommandOutput("echo [1, a, 3] == [1, b, 3]", "1")
  }

  @Test
  fun `test equal to with mismatched nested List`() {
    assertCommandOutput("echo [1, [9, 8, 7], 3] == [1, 'foo', 3]", "0")
  }

  @Test
  fun `test equal to with nested List of String values depends on 'ignorecase'`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] == [1, ['abc', 'DEF'], 3]", "1")
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] == [1, ['ABC', 'def'], 3]", "0")
    assertCommandOutput("echo [1, ['foo', 'bar'], 3] == [1, ['ABC', 'def'], 3]", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] == [1, ['ABC', 'def'], 3]", "1")
  }

  @Test
  fun `test equal to case sensitive with nested Lists`() {
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] ==# [1, ['abc', 'DEF'], 3]", "1")
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] ==# [1, ['ABC', 'def'], 3]", "0")
    assertCommandOutput("echo [1, ['foo', 'bar'], 3] ==# [1, ['ABC', 'def'], 3]", "0")
  }

  @Test
  fun `test equal to case insensitive with nested Lists`() {
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] ==? [1, ['abc', 'DEF'], 3]", "1")
    assertCommandOutput("echo [1, ['abc', 'DEF'], 3] ==? [1, ['ABC', 'def'], 3]", "1")
    assertCommandOutput("echo [1, ['foo', 'bar'], 3] ==? [1, ['ABC', 'def'], 3]", "0")
  }

  @Test
  fun `test equal to with same reference to recursive Lists`() {
    // Short-circuits with reference check
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    assertCommandOutput("echo a == a", "1")
  }

  @Test
  fun `test equal to with recursive Lists`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    enterCommand("let b = [1, 2, 3]")
    enterCommand("let b[1] = b")
    assertCommandOutput("echo a == b", "1")
  }

  // TODO: Lists of Dictionary + case sensitivity

  @Test
  fun `test equal to with Dictionary and non-Dictionary values`() {
    enterCommand("echo {'key1':1} == 'abc'")
    assertPluginError(true)
    assertPluginErrorMessage("E735: Can only compare Dictionary with Dictionary")
  }

  @Test
  fun `test equal to with Dictionary and non-Dictionary values 2`() {
    enterCommand("echo 4.5 == {'key1':1}")
    assertPluginError(true)
    assertPluginErrorMessage("E735: Can only compare Dictionary with Dictionary")
  }

  // TODO: Loads more Dictionary tests
  // TODO: Funcref tests
}
