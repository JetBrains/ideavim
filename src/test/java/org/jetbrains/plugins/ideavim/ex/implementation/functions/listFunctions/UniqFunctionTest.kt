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

class UniqFunctionTest : VimTestCase("\n") {
  @Test
  fun `test uniq with String reports error`() {
    enterCommand("call uniq('string')")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of uniq() must be a List")
  }

  @Test
  fun `test uniq with Number reports error`() {
    enterCommand("call uniq(100)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of uniq() must be a List")
  }

  @Test
  fun `test uniq with Float reports error`() {
    enterCommand("call uniq(12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of uniq() must be a List")
  }

  @Test
  fun `test uniq with Dictionary reports error`() {
    enterCommand("call uniq({'key': 'value'})")
    assertPluginError(true)
    assertPluginErrorMessage("E686: Argument of uniq() must be a List")
  }

  @Test
  fun `test uniq on a locked List reports error`() {
    enterCommand("let a = [1, 1, 2]")
    enterCommand("lockvar a")
    enterCommand("call uniq(a)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: uniq() argument")
  }

  @Test
  fun `test uniq with invalid third argument type reports error`() {
    enterCommand("call uniq([1, 2, 3], 'n', 'not a dict')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @Test
  fun `test uniq with unknown function name reports error`() {
    enterCommand("call uniq([1, 2, 3], 'UnknownFunc')")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: UnknownFunc")
  }

  @Test
  fun `test uniq with default order`() {
    assertCommandOutput("echo uniq([1, 1, 2, 2, 3])", "[1, 2, 3]")
  }

  @Test
  fun `test uniq modifies original List`() {
    enterCommand("let a = [1, 1, 2, 2, 3]")
    enterCommand("let b = a")
    enterCommand("call uniq(a)")
    assertCommandOutput("echo a", "[1, 2, 3]")
    assertCommandOutput("echo b", "[1, 2, 3]")
  }

  @Test
  fun `test uniq with empty List`() {
    assertCommandOutput("echo uniq([])", "[]")
  }

  @Test
  fun `test uniq with single element`() {
    assertCommandOutput("echo uniq([42])", "[42]")
  }

  @Test
  fun `test uniq with Strings`() {
    assertCommandOutput("echo uniq(['apple', 'apple', 'banana', 'banana', 'cherry'])", "['apple', 'banana', 'cherry']")
  }

  @Test
  fun `test uniq with mixed case Strings`() {
    assertCommandOutput("echo uniq(['Apple', 'Apple', 'apple', 'apple'])", "['Apple', 'apple']")
  }

  @Test
  fun `test uniq with empty flag parameter uses natural sort order`() {
    assertCommandOutput("echo uniq([1, 1, 2, 2, 3], '')", "[1, 2, 3]")
  }

  @Test
  fun `test uniq with 0 flag uses natural sort order`() {
    assertCommandOutput("echo uniq([1, 1, 2, 2, 3], '0')", "[1, 2, 3]")
  }

  // Case-insensitive comparison
  @Test
  fun `test uniq with i flag for case insensitive`() {
    assertCommandOutput("echo uniq(['Apple', 'apple', 'APPLE', 'banana'], 'i')", "['Apple', 'banana']")
  }

  @Test
  fun `test uniq with 1 flag for case insensitive`() {
    assertCommandOutput("echo uniq(['Apple', 'apple', 'APPLE', 'banana'], '1')", "['Apple', 'banana']")
  }

  @Test
  fun `test uniq with 0 flag for case sensitive`() {
    assertCommandOutput("echo uniq(['Apple', 'apple', 'APPLE'], '0')", "['Apple', 'apple', 'APPLE']")
  }

  // Locale-based comparison
  @Disabled("Don't know how to force a specific locale")
  @Test
  fun `test uniq with l flag for locale`() {
    assertCommandOutput("echo uniq(['n', 'n', 'o', 'O', 'ö', 'ö'], 'l')", "['n', 'o', 'ö']")
  }

  // Numerical comparison
  @Test
  fun `test uniq with n flag for numerical with Number`() {
    assertCommandOutput("echo uniq([1, 1, 2, 2, 10, 10], 'n')", "[1, 2, 10]")
  }

  @Test
  fun `test uniq with n flag for numerical with Number and Float`() {
    assertCommandOutput("echo uniq([1.0, 1, 2.0, 2, 10.0, 10], 'n')", "[1.0, 2.0, 10.0]")
  }

  @Test
  fun `test uniq with n flag treats non-numbers as zero`() {
    assertCommandOutput("echo uniq(['foo', 'bar', 1, 1], 'n')", "['foo', 1]")
  }

  @Test
  fun `test uniq with N flag parses String as Number`() {
    assertCommandOutput("echo uniq(['1', '1', '2', '2', '10', '10'], 'N')", "['1', '2', '10']")
    assertCommandOutput("echo uniq(['1foo', '1bar', '2baz', '2qux'], 'N')", "['1foo', '2baz']")
  }

  @Test
  fun `test uniq with f flag with Float`() {
    assertCommandOutput("echo uniq([1.2, 1.2, 2.8, 2.8, 3.5], 'f')", "[1.2, 2.8, 3.5]")
  }

  @Test
  fun `test uniq with f flag with mixed numbers`() {
    assertCommandOutput("echo uniq([1, 1.0, 2, 2.0], 'f')", "[1, 2]")
  }

  @Test
  fun `test uniq with f flag reports error with String`() {
    enterCommand("echo uniq([1.2, '1', 2.8], 'f')")
    assertPluginError(true)
    assertPluginErrorMessage("E892: Using a String as a Float")
  }

  // Type ordering
  @Test
  fun `test uniq preserves type ordering`() {
    assertCommandOutput("echo uniq(['a', 'a', 1, 1, [], []])", "['a', 1, []]")
  }

  @Test
  fun `test uniq with mixed types keeps first of each type`() {
    assertCommandOutput("echo uniq([1, 'a', 2, 'b', 3, 'c'])", "[1, 'a', 2, 'b', 3, 'c']")
  }

  // Funcref comparison
  @Test
  fun `test uniq with Funcref`() {
    enterCommand("function! CompareAbs(a, b) | return abs(a:a) == abs(a:b) ? 0 : abs(a:a) > abs(a:b) ? 1 : -1 | endfunction")
    assertCommandOutput("echo uniq([1, -1, 2, -2, 3], function('CompareAbs'))", "[1, 2, 3]")
  }

  @Test
  fun `test uniq with Funcref and dictionary`() {
    enterCommand("function! CompareMod(a, b) dict | return (a:a % (self.mod)) == (a:b % (self.mod)) ? 0 : (a:a % (self.mod)) > (a:b % (self.mod)) ? 1 : -1 | endfunction")
    enterCommand("let d = {'mod': 3}")
    assertCommandOutput("echo uniq([1, 4, 2, 5, 3, 6], function('CompareMod'), d)", "[1, 2, 3]")
  }

  @Test
  fun `test uniq with dictionary function`() {
    enterCommand("function! CompareMod(a, b) dict | return (a:a % (self.mod)) == (a:b % (self.mod)) ? 0 : (a:a % (self.mod)) > (a:b % (self.mod)) ? 1 : -1 | endfunction")
    enterCommand("let d = {'mod': 3, 'func': function('CompareMod')}")
    assertCommandOutput("echo uniq([1, 4, 2, 5, 3, 6], d.func)", "[1, 2, 3]")
  }

  @VimBehaviorDiffers(originalVimAfter = "Error detected while processing function Compare[1]..function Compare:" +
    "E605: Exception not caught: error")
  @Test
  fun `test uniq with Funcref that throws error`() {
    enterCommand("function! Compare(a, b) | throw 'error' | endfunction")
    enterCommand("echo uniq([1, 1, 2], function('Compare'))")
    assertPluginError(true)
    assertPluginErrorMessage("error")
  }

  // Lambda comparison
  @Test
  fun `test uniq with lambda`() {
    assertCommandOutput("echo uniq([1, -1, 2, -2, 3], {a, b -> abs(a) == abs(b) ? 0 : abs(a) > abs(b) ? 1 : -1})", "[1, 2, 3]")
  }

  // Function name as String
  @Test
  fun `test uniq with function name as String`() {
    enterCommand("function! MyCompare(a, b) | return a:a - a:b | endfunction")
    assertCommandOutput("echo uniq([1, 1, 2, 2, 3], 'MyCompare')", "[1, 2, 3]")
  }

  @Test
  fun `test uniq with function name as String and dictionary`() {
    enterCommand("function! Compare(a, b) dict | return (a:a % (self.mod)) == (a:b % (self.mod)) ? 0 : (a:a % (self.mod)) > (a:b % (self.mod)) ? 1 : -1 | endfunction")
    enterCommand("let d = {'mod': 3}")
    assertCommandOutput("echo uniq([1, 4, 2, 5, 3, 6], 'Compare', d)", "[1, 2, 3]")
  }

  // Method call syntax
  @Test
  fun `test uniq as method`() {
    assertCommandOutput("echo [1, 1, 2, 2, 3]->uniq()", "[1, 2, 3]")
  }

  @Test
  fun `test uniq as method with parameter`() {
    assertCommandOutput("echo ['Apple', 'apple', 'APPLE', 'banana']->uniq('i')", "['Apple', 'banana']")
  }

  @Test
  fun `test uniq as method with lambda`() {
    assertCommandOutput("echo [1, -1, 2, -2, 3]->uniq({a, b -> abs(a) == abs(b) ? 0 : abs(a) > abs(b) ? 1 : -1})", "[1, 2, 3]")
  }

  // Edge cases
  @Test
  fun `test uniq with negative numbers sorted as string`() {
    assertCommandOutput("echo uniq([-1, -1, -3, -3, 1, 1, 2])", "[-1, -3, 1, 2]")
  }

  @Test
  fun `test uniq with negative numbers using n flag`() {
    assertCommandOutput("echo uniq([-3, -3, -1, -1, 1, 1, 2], 'n')", "[-3, -1, 1, 2]")
  }

  @Test
  fun `test uniq with no duplicates`() {
    assertCommandOutput("echo uniq([1, 2, 3, 4, 5])", "[1, 2, 3, 4, 5]")
  }

  @Test
  fun `test uniq with all duplicates`() {
    assertCommandOutput("echo uniq([1, 1, 1, 1, 1])", "[1]")
  }

  @Test
  fun `test uniq returns the same List reference`() {
    enterCommand("let a = [1, 1, 2, 2, 3]")
    enterCommand("let b = uniq(a)")
    assertCommandOutput("echo a is b", "1")
  }

  @Test
  fun `test uniq only removes consecutive duplicates`() {
    assertCommandOutput("echo uniq([1, 2, 1, 2, 1])", "[1, 2, 1, 2, 1]")
  }

  @Test
  fun `test uniq with nested Lists`() {
    assertCommandOutput("echo uniq([[1], [1], [2], [2]])", "[[1], [2]]")
  }

  @Test
  fun `test uniq with Dictionaries`() {
    assertCommandOutput("echo uniq([{}, {}, {'a': 1}, {'a': 1}])", "[{}, {'a': 1}]")
  }

  @Test
  fun `test uniq with Functions`() {
    assertCommandOutput("echo uniq([function('abs'), function('abs'), function('sin'), function('sin')])", "[function('abs'), function('sin')]")
  }
}
