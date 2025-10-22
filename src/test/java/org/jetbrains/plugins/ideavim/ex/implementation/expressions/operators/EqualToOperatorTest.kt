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

  @Test
  fun `test equal to with simple Dictionary`() {
    assertCommandOutput("echo {'key1':1, 'key2':2} == {'key1':1, 'key2':2}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':2} == {'key1':1, 'key2':3}", "0")
  }

  @Test
  fun `test equal to returns false for Dictionary with different values`() {
    assertCommandOutput("echo {'key1':1} == {'key1':2}", "0")
  }

  @Test
  fun `test equal to returns false for Dictionary with same key but different case`() {
    assertCommandOutput("echo {'key1':1} == {'KEY1':1}", "0")
  }

  @Test
  fun `test equal to with same Dictionary reference`() {
    enterCommand("let a = {'key1':1, 'key2':2}")
    enterCommand("let b = a")
    assertCommandOutput("echo a == b", "1")
  }

  @Test
  fun `test equal to with empty Dictionaries`() {
    assertCommandOutput("echo {} == {}", "1")
  }

  @Test
  fun `test equal to with Dictionary with keys in different order`() {
    assertCommandOutput("echo {'key1':1, 'key2':2} == {'key2':2, 'key1':1}", "1")
  }

  @Test
  fun `test equal to with Dictionary does not coerce values`() {
    assertCommandOutput("echo {'key1':1} == {'key1':1.0}", "0")
  }

  @Test
  fun `test equal to with Dictionary does not coerce values 2`() {
    assertCommandOutput("echo {'key1':1} == {'key1':'1'}", "0")
  }

  @Test
  fun `test equal to with Dictionary with String value depends on 'ignorecase'`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} == {'key1':'abc', 'key2':'DEF'}", "1")
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} == {'key1':'ABC', 'key2':'def'}", "0")
    assertCommandOutput("echo {'key1':'foo', 'key2':'bar'} == {'key1':'ABC', 'key2':'def'}", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} == {'key1':'ABC', 'key2':'def'}", "1")
  }

  @Test
  fun `test equal to case sensitive with Dictionary with String value`() {
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} ==# {'key1':'abc', 'key2':'DEF'}", "1")
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} ==# {'key1':'ABC', 'key2':'def'}", "0")
    assertCommandOutput("echo {'key1':'foo', 'key2':'bar'} ==# {'key1':'ABC', 'key2':'def'}", "0")
  }

  @Test
  fun `test equal to case insensitive with Dictionary with String value`() {
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} ==? {'key1':'abc', 'key2':'DEF'}", "1")
    assertCommandOutput("echo {'key1':'abc', 'key2':'DEF'} ==? {'key1':'ABC', 'key2':'def'}", "1")
    assertCommandOutput("echo {'key1':'foo', 'key2':'bar'} ==? {'key1':'ABC', 'key2':'def'}", "0")
  }

  @Test
  fun `test equal to with nested Dictionary`() {
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':1, 'subkey2':2}, 'key3':3} == {'key1':1, 'key2':{'subkey1':1, 'subkey2':2}, 'key3':3}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':1, 'subkey2':2}, 'key3':3} == {'key1':1, 'key2':{'subkey1':1, 'subkey2':3}, 'key3':3}", "0")
  }

  @Test
  fun `test equal to with nested Dictionary reference`() {
    enterCommand("let a = {'subkey1':1, 'subkey2':2}")
    enterCommand("let b = {'subkey1':1, 'subkey2':2}")
    assertCommandOutput("echo {'key1':1, 'key2':a} == {'key1':1, 'key2':a}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':a} == {'key1':1, 'key2':b}", "1")
  }

  @Test
  fun `test equal to with mismatched nested Dictionary`() {
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':1, 'subkey2':2}, 'key3':3} == {'key1':1, 'key2':'foo', 'key3':3}", "0")
  }

  @Test
  fun `test equal to with nested Dictionary of String values depends on 'ignorecase'`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} == {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} == {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "0")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'foo', 'subkey2':'bar'}, 'key3':3} == {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} == {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "1")
  }

  @Test
  fun `test equal to case sensitive with nested Dictionary`() {
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} ==# {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} ==# {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "0")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'foo', 'subkey2':'bar'}, 'key3':3} ==# {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "0")
  }

  @Test
  fun `test equal to case insensitive with nested Dictionary`() {
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} ==? {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'abc', 'subkey2':'DEF'}, 'key3':3} ==? {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "1")
    assertCommandOutput("echo {'key1':1, 'key2':{'subkey1':'foo', 'subkey2':'bar'}, 'key3':3} ==? {'key1':1, 'key2':{'subkey1':'ABC', 'subkey2':'def'}, 'key3':3}", "0")
  }

  @Test
  fun `test equal to with same reference to recursive Dictionary`() {
    // Short-circuits with reference check
    enterCommand("let a = {'key1':1, 'key2':2, 'key3':3}")
    enterCommand("let a['key2'] = a")
    assertCommandOutput("echo a == a", "1")
  }

  @Test
  fun `test equal to with recursive Dictionary`() {
    enterCommand("let a = {'key1':1, 'key2':2, 'key3':3}")
    enterCommand("let a['key2'] = a")
    enterCommand("let b = {'key1':1, 'key2':2, 'key3':3}")
    enterCommand("let b['key2'] = b")
    assertCommandOutput("echo a == b", "1")
  }

  @Test
  fun `test equal to with Dictionary containing a List`() {
    assertCommandOutput("echo {'key1':[1, 2, 3], 'key2':2} == {'key1':[1, 2, 3], 'key2':2}", "1")
    assertCommandOutput("echo {'key1':[1, 2, 3], 'key2':2} == {'key1':[1, 9, 3], 'key2':2}", "0")
  }

  @Test
  fun `test equal to with List containing a Dictionary`() {
    assertCommandOutput("echo [1, {'key1':1, 'key2':2}] == [1, {'key1':1, 'key2':2}]", "1")
    assertCommandOutput("echo [1, {'key1':1, 'key2':2}] == [1, {'key1':9, 'key2':2}]", "0")
  }

  @Test
  fun `test equal to with List containing recursive Dictionary`() {
    enterCommand("let a = {'key1':1, 'key2':2}")
    enterCommand("let a['key2'] = a")
    assertCommandOutput("echo [1, a] == [1, a]", "1")
  }

  @Test
  fun `test equal to with Dictionary containing recursive List`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    assertCommandOutput("echo {'key1':a, 'key2':2} == {'key1':a, 'key2':2}", "1")
  }

  @Test
  fun `test equal to with Dictionary containing List containing recursive Dictionary`() {
    enterCommand("let a = {'key1':1, 'key2':2}")
    enterCommand("let a['key2'] = a")
    enterCommand("let b = [1, a]")
    assertCommandOutput("echo {'key1':b, 'key2':2} == {'key1':[1,a], 'key2':2}", "1")
  }

  @Test
  fun `test equal to with Funcref and non-Funcref values`() {
    // Surprisingly not an error
    assertCommandOutput("echo function('abs') == 'abs'", "0")
    assertPluginError(false)
  }

  @Test
  fun `test equal to with simple Funcref`() {
    assertCommandOutput("echo function('abs') == function('abs')", "1")
    assertCommandOutput("echo function('abs') == function('max')", "0")
  }

  @Test
  fun `test equal to with same Funcref references`() {
    enterCommand("let a = function('abs')")
    enterCommand("let b = a")
    assertCommandOutput("echo a == b", "1")
  }

  @Test
  fun `test equal to with Funcref with arguments`() {
    assertCommandOutput("echo function('abs', [1]) == function('abs', [1])", "1")
    assertCommandOutput("echo function('abs', [-1]) == function('abs')", "0")
    assertCommandOutput("echo function('abs', [-1]) == function('abs', [1])", "0")
    assertCommandOutput("echo function('abs', [1]) == function('abs')", "0")
    assertCommandOutput("echo function('abs') == function('abs', [1])", "0")
  }

  @Test
  fun `test equal to with Funcref with recursive arguments`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    assertCommandOutput("echo function('abs', a) == function('abs', a)", "1")
  }

  @Test
  fun `test equal to with Funcref with dictionary`() {
    assertCommandOutput("echo function('abs', {'a':1, 'b':2}) == function('abs', {'a':1, 'b':2})", "1")
    assertCommandOutput("echo function('abs', {'a':1, 'b':2}) == function('abs', {'a':1, 'b':3})", "0")
    assertCommandOutput("echo function('abs', {'a':1, 'b':2}) == function('abs')", "0")
    assertCommandOutput("echo function('abs') == function('abs', {'a':1, 'b':3})", "0")
  }

  @Test
  fun `test equal to with Funcref with recursive dictionary`() {
    enterCommand("let a = {'a':1, 'b':2}")
    enterCommand("let a['b'] = a")
    assertCommandOutput("echo function('abs', a) == function('abs', a)", "1")
  }

  @Test
  fun `test equal to with Funcref and arguments and dictionary`() {
    assertCommandOutput("echo function('abs', [1], {'a':1}) == function('abs', [1], {'a':1})", "1")
    assertCommandOutput("echo function('abs', [1]) == function('abs', [1], {'a':1})", "0")
    assertCommandOutput("echo function('abs', [1], {'a':1}) == function('abs', [1])", "0")
  }

  @Test
  fun `test equal to case sensitive with Funcref and arguments`() {
    assertCommandOutput("echo function('abs', ['abc']) ==# function('abs', ['abc'])", "1")
    assertCommandOutput("echo function('abs', ['ABC']) ==# function('abs', ['abc'])", "0")
  }

  @Test
  fun `test equal to case insensitive with Funcref and arguments`() {
    assertCommandOutput("echo function('abs', ['abc']) ==? function('abs', ['abc'])", "1")
    assertCommandOutput("echo function('abs', ['ABC']) ==? function('abs', ['abc'])", "1")
  }

  @Test
  fun `test equal to case sensitive with Funcref and dictionary`() {
    assertCommandOutput("echo function('abs', {'key1':'abc'}) ==# function('abs', {'key1':'abc'})", "1")
    assertCommandOutput("echo function('abs', {'key1':'ABC'}) ==# function('abs', {'key1':'abc'})", "0")
  }

  @Test
  fun `test equal to case insensitive with Funcref and dictionary`() {
    assertCommandOutput("echo function('abs', {'key1':'abc'}) ==? function('abs', {'key1':'abc'})", "1")
    assertCommandOutput("echo function('abs', {'key1':'ABC'}) ==? function('abs', {'key1':'abc'})", "1")
  }
}
