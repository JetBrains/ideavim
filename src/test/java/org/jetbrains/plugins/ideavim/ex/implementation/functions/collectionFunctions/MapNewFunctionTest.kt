/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.collectionFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class MapNewFunctionTest : VimTestCase("\n") {
  @Test
  fun `test mapnew with too few arguments reports error`() {
    enterCommand("echo mapnew([1,2,3])")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: mapnew")
  }

  @Test
  fun `test mapnew with too many arguments reports error`() {
    enterCommand("echo mapnew([1,2,3], 'v:val * 2', 42)")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: mapnew")
  }

  @Test
  fun `test mapnew with invalid first argument type`() {
    enterCommand("echo mapnew(12, 'v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("E1250: Argument of mapnew() must be a List, String, Dictionary or Blob")
  }

  @Test
  fun `test mapnew with invalid second argument type`() {
    enterCommand("echo mapnew([1,2,3], [9,9,9])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test mapnew with empty expression reports error`() {
    enterCommand("echo mapnew([1,2,3], '')")
    assertPluginError(true)
    assertPluginErrorMessage("E15: Invalid expression: \"\"")
  }

  @Test
  fun `test mapnew with simple Number expression replaces input List`() {
    assertCommandOutput("echo mapnew([1,2,3], 12)", "[12, 12, 12]")
  }

  @Test
  fun `test mapnew with simple Float expression replaces input List`() {
    assertCommandOutput("echo mapnew([1,2,3], 12.2)", "[12.2, 12.2, 12.2]")
  }

  @Test
  fun `test mapnew with simple String expression replaces input List`() {
    assertCommandOutput("echo mapnew([1,2,3], '\"xx\"')", "['xx', 'xx', 'xx']")
  }

  @Test
  fun `test mapnew with simple String reports unknown variable error`() {
    // The expression 'xx' is evaluated twice, once to resolve to an expression, and again to evaluate the expression
    enterCommand("echo mapnew([1,2,3], 'xx')")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: xx")
  }

  @Test
  fun `test mapnew List with expression`() {
    assertCommandOutput("echo mapnew([1, 2, 3], 'v:val * 2')", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew empty List`() {
    assertCommandOutput("echo mapnew([], 'v:val * 2')", "[]")
  }

  @Test
  fun `test mapnew List does not modify original List`() {
    enterCommand("let l = [1, 2, 3]")
    assertCommandOutput("echo mapnew(l, 'v:val * 2')", "[2, 4, 6]")
    assertCommandOutput("echo l", "[1, 2, 3]")
  }

  @Test
  fun `test mapnew List with mixed types`() {
    enterCommand("let l = [1, 'two', 3]")
    assertCommandOutput("echo mapnew(l, 'v:val * 2')", "[2, 0, 6]")
  }

  @Test
  fun `test mapnew locked List reports error without modifying original List`() {
    enterCommand("let l = [1, 2, 3]")
    enterCommand("lockvar l")
    enterCommand("call mapnew(l, 'v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: mapnew() argument")
    assertCommandOutput("echo l", "[1, 2, 3]")
  }

  @Disabled("IdeaVim does not support lockvar l[1] syntax")
  @Test
  fun `test mapnew locked List item reports error and modifies original List`() {
    enterCommand("let l = [1, 2, 3]")
    enterCommand("lockvar l[1]")
    enterCommand("call mapnew(l, 'v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: mapnew() argument")
    assertCommandOutput("echo l", "[2, 2, 3]")
  }

  // TODO: Support errors while calling functions
  // When this expression evaluates the `call()` function, the user-defined function handler is invoked and executes the
  // function body. It receives ExecutionResult and converts it to a Vim error. It can report the errors, but it doesn't
  // provide any way for the caller to know that there was an error - it just returns 0.
  // Perhaps function handlers should return ExecutionResult?
  // BUT, we're only using the `call()` function because I can't find a way for an expression to cause an error. And if
  // we can do that, then Expression.evaluate does not provide access to errors either
  @Disabled("IdeaVim does not let errors leak out of functions")
  @VimBehaviorDiffers(originalVimAfter = "Error detected while processing function Error[1]..function Error:" +
    "E605: Exception not caught: error")
  @Test
  fun `test mapnew stops processing after error`() {
    enterCommand("let l = [1, 2, 3]")
    enterCommand("function! Error() | throw 'error' | endfunction")
    enterCommand("call mapnew(l, 'v:val >= 2 ? call(\"Error\", []) : v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("error")
    assertCommandOutput("echo l", "[2, 4, 3]")
  }

  @Test
  fun `test mapnew List with Funcref`() {
    enterCommand("function! Double(idx, val) | return a:val * 2 | endfunction")
    assertCommandOutput("echo mapnew([1, 2, 3], function('Double'))", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew List to different type`() {
    enterCommand("function! Double(key, val) | return a:val . a:key | endfunction")
    assertCommandOutput("echo mapnew([1, 2, 3], function('Double'))", "['10', '21', '32']")
  }

  @Test
  fun `test mapnew List with Funcref with too few arguments`() {
    enterCommand("function! Double(val) | return a:val * 2 | endfunction")
    enterCommand("echo mapnew([1, 2, 3], function('Double'))")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: Double")
  }

  @Test
  fun `test mapnew List with Funcref with too many arguments`() {
    enterCommand("function! Double(idx, val, other) | return a:val * 2 | endfunction")
    enterCommand("echo mapnew([1, 2, 3], function('Double'))")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: Double")
  }

  @Test
  fun `test mapnew List with Funcref using value variable`() {
    enterCommand("function! Double(idx, val) | return v:val * 2 | endfunction")
    assertCommandOutput("echo mapnew([1, 2, 3], function('Double'))", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew List with Funcref using key variable`() {
    enterCommand("function! Double(idx, val) | return v:key | endfunction")
    assertCommandOutput("echo mapnew([1, 2, 3], function('Double'))", "[0, 1, 2]")
  }

  @Test
  fun `test mapnew List with Funcref variable`() {
    enterCommand("function! Double(idx, val) | return a:val * 2 | endfunction")
    enterCommand("let F = function('Double')")
    assertCommandOutput("echo mapnew([1, 2, 3], F)", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew List with dictionary function`() {
    // Note: brackets are required because IdeaVim can't handle Vim's ambiguous precedence of dict.value and expr . expr
    enterCommand("function! Multiply(idx, val) dict | return a:val * (self.value) | endfunction")
    enterCommand("let d={'value':3, 'func':function('Multiply')}")
    assertCommandOutput("echo mapnew([1, 2, 3], d.func)", "[3, 6, 9]")
  }

  @Test
  fun `test mapnew List with lambda`() {
    assertCommandOutput("echo mapnew([1, 2, 3], {idx, val -> val * 2})", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew List with lambda with too few arguments does not cause error`() {
    enterCommand("let l = [1, 2, 3]")
    assertCommandOutput("echo mapnew(l, {key -> key * 2})", "[0, 2, 4]") // only one argument is key
    assertPluginError(false)
    assertCommandOutput("echo l", "[1, 2, 3]")
  }

  @Test
  fun `test mapnew List with lambda with too many arguments`() {
    enterCommand("echo mapnew([1, 2, 3], {idx, val, other -> val * 2})")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: <lambda>0")
  }

  @Test
  fun `test mapnew List with lambda using index`() {
    assertCommandOutput("echo mapnew([1, 2, 3], {idx, val -> idx})", "[0, 1, 2]")
  }

  @Test
  fun `test mapnew List with lambda using value variable`() {
    assertCommandOutput("echo mapnew([1, 2, 3], {idx, val -> v:val * 2})", "[2, 4, 6]")
  }

  @Test
  fun `test mapnew List with lambda using key variable`() {
    assertCommandOutput("echo mapnew([1, 2, 3], {idx, val -> v:key})", "[0, 1, 2]")
  }

  @Test
  fun `test mapnew Dictionary with expression`() {
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, 'v:val * 2')", "{'a': 2, 'b': 4, 'c': 6}")
  }

  @Test
  fun `test mapnew empty Dictionary`() {
    assertCommandOutput("echo mapnew({}, 'v:val * 2')", "{}")
  }

  @Test
  fun `test mapnew Dictionary with expression and key variable`() {
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, 'v:key . v:val')", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew Dictionary does not modify original Dictionary`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    assertCommandOutput("echo mapnew(d, 'v:val * 2')", "{'a': 2, 'b': 4, 'c': 6}")
    assertCommandOutput("echo d", "{'a': 1, 'b': 2, 'c': 3}")
  }

  @Test
  fun `test mapnew locked Dictionary`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    enterCommand("lockvar d")
    enterCommand("call mapnew(d, 'v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: mapnew() argument")
  }

  @Disabled("IdeaVim does not support lockvar d['b'] syntax")
  @Test
  fun `test mapnew locked Dictionary item`() {
    enterCommand("let d = {'a': 1, 'b': 2, 'c': 3}")
    enterCommand("lockvar d['b']")
    enterCommand("call mapnew(d, 'v:val * 2')")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: mapnew() argument")
    assertCommandOutput("echo d", "{'a': 2, 'b': 2, 'c': 3}")
  }

  @Test
  fun `test mapnew Dictionary with Funcref`() {
    enterCommand("function! Process(key, val) | return a:key . a:val | endfunction")
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, function('Process'))", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew Dictionary with Funcref using key and value variables`() {
    enterCommand("function! Process(key, val) | return v:key . v:val | endfunction")
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, function('Process'))", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew Dictionary with Funcref variable`() {
    enterCommand("function! Process(key, val) | return a:key . a:val | endfunction")
    enterCommand("let F = function('Process')")
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, F)", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew Dictionary with Funcref with too few arguments`() {
    enterCommand("function! Process(val) | return a:val * 2 | endfunction")
    enterCommand("echo mapnew({'a': 1, 'b': 2, 'c': 3}, function('Process'))")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: Process")
  }

  @Test
  fun `test mapnew Dictionary with Funcref with too many arguments`() {
    enterCommand("function! Process(key, val, other) | return a:key . a:val | endfunction")
    enterCommand("echo mapnew({'a': 1, 'b': 2, 'c': 3}, function('Process'))")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: Process")
  }

  @Test
  fun `test mapnew Dictionary with dictionary function`() {
    // Note: brackets are required because IdeaVim can't handle Vim's ambiguous precedence of dict.value and expr . expr
    enterCommand("function! Multiply(idx, val) dict | return a:val * (self.value) | endfunction")
    enterCommand("let d={'value':3, 'func':function('Multiply')}")
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, d.func)", "{'a': 3, 'b': 6, 'c': 9}")
  }

  @Test
  fun `test mapnew Dictionary with lambda`() {
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, {key, val -> key . val})", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew Dictionary with lambda using key and value variables`() {
    assertCommandOutput("echo mapnew({'a': 1, 'b': 2, 'c': 3}, {key, val -> v:key . v:val})", "{'a': 'a1', 'b': 'b2', 'c': 'c3'}")
  }

  @Test
  fun `test mapnew String with expression`() {
    assertCommandOutput("echo string(mapnew('abc', 'v:val . v:key'))", "'a0b1c2'")
  }

  @Test
  fun `test mapnew empty String`() {
    assertCommandOutput("echo string(mapnew('', 'v:val . v:key'))", "''")
  }

  @Test
  fun `test mapnew String with empty expression reports error`() {
    enterCommand("echo mapnew('abc', '')")
    assertPluginError(true)
    assertPluginErrorMessage("E15: Invalid expression: \"\"")
  }

  @Test
  fun `test mapnew String does not modify original String`() {
    enterCommand("let s = 'abc'")
    enterCommand("call mapnew(s, 'v:val . v:key')")
    assertCommandOutput("echo s", "abc")
  }

  @Test
  fun `test mapnew String with Funcref`() {
    enterCommand("function! Process(key, val) | return a:val . a:key | endfunction")
    assertCommandOutput("echo mapnew('abc', function('Process'))", "a0b1c2")
  }

  @Test
  fun `test mapnew String with Funcref using value and key variables`() {
    enterCommand("function! Process(key, val) | return v:val . v:key | endfunction")
    assertCommandOutput("echo mapnew('abc', function('Process'))", "a0b1c2")
  }

  @Test
  fun `test mapnew String with Funcref variable`() {
    enterCommand("function! Process(key, val) | return a:val . a:key | endfunction")
    enterCommand("let F = function('Process')")
    assertCommandOutput("echo mapnew('abc', F)", "a0b1c2")
  }

  @Test
  fun `test mapnew String with lambda`() {
    assertCommandOutput("echo mapnew('abc', {key, val -> val . key})", "a0b1c2")
  }

  @Test
  fun `test mapnew String with lambda using value and key variables`() {
    assertCommandOutput("echo mapnew('abc', {key, val -> v:val . v:key})", "a0b1c2")
  }

  @Test
  fun `test mapnew String expression returning wrong type`() {
    enterCommand("echo mapnew('abc', {key, val -> 1})")
    assertPluginError(true)
    assertPluginErrorMessage("E928: String required")
  }
}
