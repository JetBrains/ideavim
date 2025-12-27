/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.collectionFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FilterFunctionTest : VimTestCase("\n") {
  @Test
  fun `test filter with too few arguments reports error`() {
    enterCommand("echo filter([1,2,3])")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: filter")
  }

  @Test
  fun `test filter with too many arguments reports error`() {
    enterCommand("echo filter([1,2,3], 'v:val > 1', 42)")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: filter")
  }

  @Test
  fun `test filter with invalid first argument type`() {
    enterCommand("echo filter(12, 'v:val > 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E1250: Argument of filter() must be a List, String, Dictionary or Blob")
  }

  @Test
  fun `test filter with invalid second argument type`() {
    enterCommand("echo filter([1,2,3], [9,9,9])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test filter with empty expression reports error`() {
    enterCommand("echo filter([1,2,3], '')")
    assertPluginError(true)
    assertPluginErrorMessage("E15: Invalid expression: \"\"")
  }

  @Test
  fun `test filter List with expression keeping values greater than 1`() {
    assertCommandOutput("echo filter([1, 2, 3], 'v:val > 1')", "[2, 3]")
  }

  @Test
  fun `test filter List with expression keeping even values`() {
    assertCommandOutput("echo filter([1, 2, 3, 4], 'v:val % 2 == 0')", "[2, 4]")
  }

  @Test
  fun `test filter List keeping all values`() {
    assertCommandOutput("echo filter([1, 2, 3], '1')", "[1, 2, 3]")
  }

  @Test
  fun `test filter List removing all values`() {
    assertCommandOutput("echo filter([1, 2, 3], '0')", "[]")
  }

  @Test
  fun `test filter empty List`() {
    assertCommandOutput("echo filter([], 'v:val > 1')", "[]")
  }

  @Test
  fun `test filter List modifies original List`() {
    enterCommand("let l = [1, 2, 3, 4]")
    enterCommand("call filter(l, 'v:val > 2')")
    assertCommandOutput("echo l", "[3, 4]")
  }

  @Test
  fun `test filter List with mixed types`() {
    // The String value is parsed as 0
    enterCommand("let l = [1, 'two', 3]")
    assertCommandOutput("echo filter(l, 'v:val > 1')", "[3]")
  }

  @Test
  fun `test filter List using key variable`() {
    assertCommandOutput("echo filter([10, 20, 30], 'v:key > 0')", "[20, 30]")
  }

  @Test
  fun `test filter locked List reports error without modifying original List`() {
    enterCommand("let l = [1, 2, 3]")
    enterCommand("lockvar l")
    enterCommand("call filter(l, 'v:val > 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: filter() argument")
    assertCommandOutput("echo l", "[1, 2, 3]")
  }

  @Disabled("IdeaVim does not support lockvar l[1] syntax")
  @Test
  fun `test filter locked List item reports error and modifies original List`() {
    enterCommand("let l = [1, 2, 3]")
    enterCommand("lockvar l[1]")
    enterCommand("call filter(l, 'v:val > 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: filter() argument")
    assertCommandOutput("echo l", "[3]")
  }

  @Test
  fun `test filter List with Funcref`() {
    enterCommand("function! GreaterThanOne(idx, val) | return a:val > 1 | endfunction")
    assertCommandOutput("echo filter([1, 2, 3], function('GreaterThanOne'))", "[2, 3]")
  }

  @Test
  fun `test filter List with Funcref with too few parameters`() {
    enterCommand("function! GreaterThanOne(val) | return a:val > 1 | endfunction")
    enterCommand("echo filter([1, 2, 3], function('GreaterThanOne'))")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: GreaterThanOne")
  }

  @Test
  fun `test filter List with Funcref with too many parameters`() {
    enterCommand("function! GreaterThanOne(idx, val, other) | return a:val > 1 | endfunction")
    enterCommand("echo filter([1, 2, 3], function('GreaterThanOne'))")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: GreaterThanOne")
  }

  @Test
  fun `test filter List with Funcref using value variable`() {
    enterCommand("function! GreaterThanOne(idx, val) | return v:val > 1 | endfunction")
    assertCommandOutput("echo filter([1, 2, 3], function('GreaterThanOne'))", "[2, 3]")
  }

  @Test
  fun `test filter List with Funcref using key variable`() {
    enterCommand("function! GreaterThanZero(idx, val) | return v:key > 0 | endfunction")
    assertCommandOutput("echo filter([10, 20, 30], function('GreaterThanZero'))", "[20, 30]")
  }

  @Test
  fun `test filter List with Funcref variable`() {
    enterCommand("function! GreaterThanOne(idx, val) | return a:val > 1 | endfunction")
    enterCommand("let F = function('GreaterThanOne')")
    assertCommandOutput("echo filter([1, 2, 3], F)", "[2, 3]")
  }

  @Test
  fun `test filter List with dictionary function`() {
    enterCommand("function! GreaterThan(idx, val) dict | return a:val > (self.threshold) | endfunction")
    enterCommand("let d={'threshold':1, 'func':function('GreaterThan')}")
    assertCommandOutput("echo filter([1, 2, 3], d.func)", "[2, 3]")
  }

  @Test
  fun `test filter List with lambda`() {
    assertCommandOutput("echo filter([1, 2, 3], {idx, val -> val > 1})", "[2, 3]")
  }

  @Test
  fun `test filter List with lambda with too few parameters does not cause error`() {
    enterCommand("let l = [10, 20, 30]")
    enterCommand("call filter(l, {key -> key > 0})")
    assertPluginError(false)
    assertCommandOutput("echo l", "[20, 30]")
  }

  @Test
  fun `test filter List with lambda with too many parameters`() {
    enterCommand("echo filter([1, 2, 3], {idx, val, other -> val > 1})")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: <lambda>0")
  }

  @Test
  fun `test filter List with lambda using index`() {
    assertCommandOutput("echo filter([10, 20, 30], {idx, val -> idx > 0})", "[20, 30]")
  }

  @Test
  fun `test filter List with lambda using value variable`() {
    assertCommandOutput("echo filter([1, 2, 3], {idx, val -> v:val > 1})", "[2, 3]")
  }

  @Test
  fun `test filter List with lambda using key variable`() {
    assertCommandOutput("echo filter([10, 20, 30], {idx, val -> v:key > 0})", "[20, 30]")
  }

  @Test
  fun `test filter Dictionary with expression`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, 'v:val > 1')", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter empty Dictionary`() {
    assertCommandOutput("echo filter({}, 'v:val > 1')", "{}")
  }

  @Test
  fun `test filter Dictionary with expression using key variable`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, 'v:key ==# \"b\"')", "{'b': 2}")
  }

  @Test
  fun `test filter Dictionary keeping all values`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2}, '1')", "{'a': 1, 'b': 2}")
  }

  @Test
  fun `test filter Dictionary removing all values`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2}, '0')", "{}")
  }

  @Test
  fun `test filter Dictionary modifies original Dictionary`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    enterCommand("call filter(d, 'v:val > 1')")
    assertCommandOutput("echo d", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter locked Dictionary`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    enterCommand("lockvar d")
    enterCommand("call filter(d, 'v:val > 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: filter() argument")
  }

  @Disabled("IdeaVim does not support lockvar d['b'] syntax")
  @Test
  fun `test filter locked Dictionary item`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    enterCommand("lockvar d['b']")
    enterCommand("call filter(d, 'v:val > 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: filter() argument")
    assertCommandOutput("echo d", "{'c': 3}")
  }

  @Test
  fun `test filter Dictionary with Funcref`() {
    enterCommand("function! GreaterThanOne(key, val) | return a:val > 1 | endfunction")
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, function('GreaterThanOne'))", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter Dictionary with Funcref using key and value variables`() {
    enterCommand("function! Process(key, val) | return v:key ==# 'b' | endfunction")
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, function('Process'))", "{'b': 2}")
  }

  @Test
  fun `test filter Dictionary with Funcref variable`() {
    enterCommand("function! GreaterThanOne(key, val) | return a:val > 1 | endfunction")
    enterCommand("let F = function('GreaterThanOne')")
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, F)", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter Dictionary with Funcref with too few parameters`() {
    enterCommand("function! GreaterThanOne(val) | return a:val > 1 | endfunction")
    enterCommand("echo filter({'a': 1, 'b': 2, 'c': 3}, function('GreaterThanOne'))")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: GreaterThanOne")
  }

  @Test
  fun `test filter Dictionary with Funcref with too many parameters`() {
    enterCommand("function! GreaterThanOne(key, val, other) | return a:val > 1 | endfunction")
    enterCommand("echo filter({'a': 1, 'b': 2, 'c': 3}, function('GreaterThanOne'))")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: GreaterThanOne")
  }

  @Test
  fun `test filter Dictionary with dictionary function`() {
    enterCommand("function! GreaterThan(key, val) dict | return a:val > (self.threshold) | endfunction")
    enterCommand("let d={'threshold':1, 'func':function('GreaterThan')}")
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, d.func)", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter Dictionary with lambda`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, {key, val -> val > 1})", "{'b': 2, 'c': 3}")
  }

  @Test
  fun `test filter Dictionary with lambda using key and value variables`() {
    assertCommandOutput("echo filter({'a': 1, 'b': 2, 'c': 3}, {key, val -> v:key ==# 'b'})", "{'b': 2}")
  }

  @Test
  fun `test filter String with expression`() {
    assertCommandOutput("echo string(filter('abc', 'v:key > 0'))", "'bc'")
  }

  @Test
  fun `test filter empty String`() {
    assertCommandOutput("echo string(filter('', 'v:val ==# \"a\"'))", "''")
  }

  @Test
  fun `test filter String with expression using value`() {
    assertCommandOutput("echo string(filter('abc', 'v:val ==# \"b\"'))", "'b'")
  }

  @Test
  fun `test filter String with empty expression reports error`() {
    enterCommand("echo filter('abc', '')")
    assertPluginError(true)
    assertPluginErrorMessage("E15: Invalid expression: \"\"")
  }

  @Test
  fun `test filter String does not modify original String`() {
    enterCommand("let s = 'abc'")
    enterCommand("call filter(s, 'v:key > 0')")
    assertCommandOutput("echo s", "abc")
  }

  @Test
  fun `test filter String keeping all characters`() {
    assertCommandOutput("echo filter('abc', '1')", "abc")
  }

  @Test
  fun `test filter String removing all characters`() {
    assertCommandOutput("echo filter('abc', '0')", "")
  }

  @Test
  fun `test filter String with Funcref`() {
    enterCommand("function! KeepB(key, val) | return a:val ==# 'b' | endfunction")
    assertCommandOutput("echo filter('abc', function('KeepB'))", "b")
  }

  @Test
  fun `test filter String with Funcref using value and key variables`() {
    enterCommand("function! Process(key, val) | return v:key > 0 | endfunction")
    assertCommandOutput("echo filter('abc', function('Process'))", "bc")
  }

  @Test
  fun `test filter String with Funcref variable`() {
    enterCommand("function! KeepB(key, val) | return a:val ==# 'b' | endfunction")
    enterCommand("let F = function('KeepB')")
    assertCommandOutput("echo filter('abc', F)", "b")
  }

  @Test
  fun `test filter String with lambda`() {
    assertCommandOutput("echo filter('abc', {key, val -> val ==# 'b'})", "b")
  }

  @Test
  fun `test filter String with lambda using value and key variables`() {
    assertCommandOutput("echo filter('abc', {key, val -> v:key > 0})", "bc")
  }

  @Test
  fun `test filter String expression returning non-string does not cause error`() {
    enterCommand("echo filter('abc', {key, val -> key})")
    assertPluginError(false)
  }
}
