/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SortFunctionTest : VimTestCase("\n") {
  @Test
  fun `test sort with String reports error`() {
    enterCommand("call sort('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of sort() must be a List")
  }

  @Test
  fun `test sort with Number reports error`() {
    enterCommand("call sort(100)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of sort() must be a List")
  }

  @Test
  fun `test sort with Float reports error`() {
    enterCommand("call sort(12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of sort() must be a List")
  }

  @Test
  fun `test sort with Dictionary reports error`() {
    enterCommand("call sort({'key': 'value'})")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of sort() must be a List")
  }

  @Test
  fun `test sort on a locked List reports error`() {
    enterCommand("let a = [3, 1, 2]")
    enterCommand("lockvar a")
    enterCommand("call sort(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: sort() argument")
  }

  @Test
  fun `test sort with invalid third argument type reports error`() {
    enterCommand("call sort([1, 2, 3], 'n', 'not a dict')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @VimBehaviorDiffers(originalVimAfter = "E117: Unknown function: UnknownFunc" +
    "E702: Sort compare function failed")
  @Test
  fun `test sort with unknown function name reports error`() {
    enterCommand("call sort([1, 2, 3], 'UnknownFunc')")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: UnknownFunc")
  }

  @Test
  fun `test sort with default order`() {
    assertCommandOutput("echo sort([3, 1, 2])", "[1, 2, 3]")
  }

  @Test
  fun `test sort modifies original List`() {
    enterCommand("let a = [3, 1, 2]")
    enterCommand("let b = a")
    enterCommand("call sort(a)")
    assertCommandOutput("echo a", "[1, 2, 3]")
    assertCommandOutput("echo b", "[1, 2, 3]")
  }

  @Test
  fun `test sort with empty List`() {
    assertCommandOutput("echo sort([])", "[]")
  }

  @Test
  fun `test sort with single element`() {
    assertCommandOutput("echo sort([42])", "[42]")
  }

  @Test
  fun `test sort with Strings`() {
    assertCommandOutput("echo sort(['banana', 'apple', 'cherry'])", "['apple', 'banana', 'cherry']")
  }

  @Test
  fun `test sort with mixed case Strings`() {
    assertCommandOutput("echo sort(['Banana', 'apple', 'Cherry'])", "['Banana', 'Cherry', 'apple']")
  }

  @Test
  fun `test sort with empty flag parameter uses natural sort order`() {
    assertCommandOutput("echo sort([3, 1, 2], '')", "[1, 2, 3]")
  }

  @Test
  fun `test sort with 0 flag uses natural sort order`() {
    assertCommandOutput("echo sort([3, 1, 2], '0')", "[1, 2, 3]")
  }

  // Case-insensitive sorting
  @Test
  fun `test sort with i flag for case insensitive`() {
    assertCommandOutput("echo sort(['Banana', 'apple', 'Cherry'], 'i')", "['apple', 'Banana', 'Cherry']")
  }

  @Test
  fun `test sort with 1 flag for case insensitive`() {
    assertCommandOutput("echo sort(['Banana', 'apple', 'Cherry'], '1')", "['apple', 'Banana', 'Cherry']")
  }

  @Test
  fun `test sort with 0 flag for case sensitive`() {
    assertCommandOutput("echo sort(['Banana', 'apple', 'Cherry'], '0')", "['Banana', 'Cherry', 'apple']")
  }

  // Locale-based sorting
  @Disabled("Don't know how to force a specific locale")
  @Test
  fun `test sort with l flag for locale`() {
    assertCommandOutput("echo sort(['n', 'o', 'O', 'ö', 'p', 'z'], 'l')", "['n', 'o', 'O', 'ö', 'p', 'z']")
  }

  // Numerical sorting
  @Test
  fun `test sort with n flag for numerical with Number`() {
    assertCommandOutput("echo sort([10, 2, 1], 'n')", "[1, 2, 10]")
  }

  @Test
  fun `test sort with n flag for numerical with Number and Float`() {
    assertCommandOutput("echo sort([10.2, 2.9, 2.2, 1], 'n')", "[1, 2.2, 2.9, 10.2]")
  }

  @Test
  fun `test sort with n flag treats non-numbers as zero`() {
    assertCommandOutput("echo sort([3.5, 'foo', 1, 2.5], 'n')", "['foo', 1, 2.5, 3.5]")
  }

  @Test
  fun `test sort with N flag parses String as Number`() {
    assertCommandOutput("echo sort(['10', '2', '1'], 'N')", "['1', '2', '10']")
    assertCommandOutput("echo sort(['10', '2.9', '2.2', '1'], 'N')", "['1', '2.9', '2.2', '10']")
    assertCommandOutput("echo sort(['10foo', '2bar', '1baz'], 'N')", "['1baz', '2bar', '10foo']")
  }

  @Test
  fun `test sort with f flag with Float`() {
    assertCommandOutput("echo sort([3.5, 1.2, 2.8], 'f')", "[1.2, 2.8, 3.5]")
  }

  @Test
  fun `test sort with f flag with mixed numbers`() {
    assertCommandOutput("echo sort([3, 1.5, 2], 'f')", "[1.5, 2, 3]")
  }

  @Test
  fun `test sort with f flag reports error with String`() {
    enterCommand("echo sort([3.5, '1', 2.8], 'f')")
    assertPluginError(true)
    assertPluginErrorMessage("E892: Using a String as a Float")
  }

  // Type ordering
  @Test
  fun `test sort orders by type`() {
    assertCommandOutput("echo sort([[], 'string', 1, {}])", "['string', 1, [], {}]")
  }

  @Test
  fun `test sort orders Strings before Numbers`() {
    assertCommandOutput("echo sort([1, 'a', 2, 'b'])", "['a', 'b', 1, 2]")
  }

  @Test
  fun `test sort orders Numbers before Lists`() {
    assertCommandOutput("echo sort([[], 1, []])", "[1, [], []]")
  }

  @Test
  fun `test sort orders Lists before Dictionaries`() {
    assertCommandOutput("echo sort([{}, [], {}])", "[[], {}, {}]")
  }

  @Test
  fun `test sort orders Functions by name before Dictionaries and after Lists`() {
    assertCommandOutput("echo sort([function('sin'), {}, function('abs'), [], 12])", "[12, [], function('abs'), function('sin'), {}]")
  }

  // Funcref sorting
  @Test
  fun `test sort with Funcref`() {
    enterCommand("function! Reverse(a, b) | return a:a == a:b ? 0 : a:a > a:b ? -1 : 1 | endfunction")
    assertCommandOutput("echo sort([1, 3, 2], function('Reverse'))", "[3, 2, 1]")
  }

  @Test
  fun `test sort with Funcref and dictionary`() {
    // Note that `self.direction` must be accessed with parentheses due to incorrect precedence handling
    enterCommand("function! Compare(a, b) dict | return (a:a == a:b ? 0 : a:a > a:b ? 1 : -1) * (self.direction) | endfunction")
    enterCommand("let d = {'direction': -1}")
    assertCommandOutput("echo sort([5, 1, 3], function('Compare'), d)", "[5, 3, 1]")
  }

  @Test
  fun `test sort with dictionary function`() {
    // Note that `self.direction` must be accessed with parentheses due to incorrect precedence handling
    enterCommand("function! Compare(a, b) dict | return (a:a == a:b ? 0 : a:a > a:b ? 1 : -1) * (self.direction) | endfunction")
    enterCommand("let d = {'direction': -1, 'func': function('Compare')}")
    assertCommandOutput("echo sort([5, 1, 3], d.func)", "[5, 3, 1]")
  }

  @VimBehaviorDiffers(originalVimAfter = "Error detected while processing function Reverse[1]..function Reverse:" +
    "E605: Exception not caught: error" +
    "E702: Sort compare function failed")
  @Test
  fun `test sort with Funcref that throws error`() {
    enterCommand("function! Reverse(a, b) | throw 'error' | endfunction")
    enterCommand("echo sort([1, 3, 2], function('Reverse'))")
    assertPluginError(true)
    assertPluginErrorMessage("error")
  }

  // Lambda sorting
  @Test
  fun `test sort with lambda`() {
    assertCommandOutput("echo sort([1, 3, 2], {a, b -> a == b ? 0 : a > b ? -1 : 1})", "[3, 2, 1]")
  }

  // Function name as String
  @Test
  fun `test sort with function name as String`() {
    enterCommand("function! MyCompare(a, b) | return a:a - a:b | endfunction")
    assertCommandOutput("echo sort([5, 1, 3], 'MyCompare')", "[1, 3, 5]")
  }

  @Test
  fun `test sort with function name as String and dictionary`() {
    // Note that `self.direction` must be accessed with parentheses due to incorrect precedence handling
    enterCommand("function! Compare(a, b) dict | return (a:a == a:b ? 0 : a:a > a:b ? 1 : -1) * (self.direction) | endfunction")
    enterCommand("let d = {'direction': -1}")
    assertCommandOutput("echo sort([5, 1, 3], 'Compare', d)", "[5, 3, 1]")
  }

  // Method call syntax
  @Test
  fun `test sort as method`() {
    assertCommandOutput("echo [3, 1, 2]->sort()", "[1, 2, 3]")
  }

  @Test
  fun `test sort as method with parameter`() {
    assertCommandOutput("echo ['Banana', 'apple', 'Cherry']->sort('i')", "['apple', 'Banana', 'Cherry']")
  }

  @Test
  fun `test sort as method with lambda`() {
    assertCommandOutput("echo [1, 3, 2]->sort({a, b -> a > b ? -1 : a < b ? 1 : 0})", "[3, 2, 1]")
  }

  // Stability tests
  @Test
  fun `test sort is stable for equal elements`() {
    enterCommand("let a = [[1, 'a'], [1, 'b'], [1, 'c']]")
    assertCommandOutput("echo sort(a, {a, b -> a[0] - b[0]})", "[[1, 'a'], [1, 'b'], [1, 'c']]")
  }

  // Edge cases
  @Test
  fun `test sort with negative numbers sorted as string`() {
    assertCommandOutput("echo sort([1, -3, 2, -1])", "[-1, -3, 1, 2]")
  }

  @Test
  fun `test sort with string based negative numbers using n flag treated as zero`() {
    assertCommandOutput("echo sort(['1', '-3', '2', '-1'], 'n')", "['1', '-3', '2', '-1']")
  }

  @Test
  fun `test sort with negative numbers using n flag`() {
    assertCommandOutput("echo sort([1, -3, 2, -1], 'n')", "[-3, -1, 1, 2]")
  }

  @Test
  fun `test sort with duplicate values`() {
    assertCommandOutput("echo sort([3, 1, 2, 1, 3])", "[1, 1, 2, 3, 3]")
  }

  @Test
  fun `test sort returns the same List reference`() {
    enterCommand("let a = [3, 1, 2]")
    enterCommand("let b = sort(a)")
    assertCommandOutput("echo a is b", "1")
  }
}
