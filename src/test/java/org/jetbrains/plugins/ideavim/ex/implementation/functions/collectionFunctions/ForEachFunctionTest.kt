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
import org.junit.jupiter.api.Test

class ForEachFunctionTest : VimTestCase("\n") {
  @Test
  fun `test foreach does not modify original List`() {
    enterCommand("let mylist = [1, 2, 3]")
    enterCommand("call foreach(mylist, 'let x = v:val * 2')")
    assertCommandOutput("echo mylist", "[1, 2, 3]")
  }

  @Test
  fun `test foreach modifies external variable`() {
    enterCommand("let counter = 0")
    enterCommand("call foreach([1, 2, 3], 'let counter = counter + v:val')")
    assertCommandOutput("echo counter", "6")
  }

  @Test
  fun `test foreach modifies external variable with val and key variables`() {
    enterCommand("let used = {}")
    enterCommand("call foreach([1, 2, 3], 'let used[v:val] = v:key')")
    assertCommandOutput("echo used", "{'1': 0, '2': 1, '3': 2}")
  }


  // List
  @Test
  fun `test foreach with List using string expression and val variable`() {
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], 'call add(result, v:val * 2)')")
    assertCommandOutput("echo result", "[2, 4, 6]")
  }

  @Test
  fun `test foreach with List using string expression and key variable`() {
    enterCommand("let result = []")
    enterCommand("call foreach([10, 20, 30], 'call add(result, v:key)')")
    assertCommandOutput("echo result", "[0, 1, 2]")
  }

  @Test
  fun `test foreach with List using string expression with both key and val variables`() {
    enterCommand("let result = []")
    enterCommand("call foreach(['a', 'b', 'c'], 'call add(result, v:key . v:val)')")
    assertCommandOutput("echo result", "['0a', '1b', '2c']")
  }

  @Test
  fun `test foreach with List returns original List`() {
    enterCommand("let mylist = [1, 2, 3]")
    assertCommandOutput("echo foreach(mylist, 'let x = v:val')", "[1, 2, 3]")
  }

  @Test
  fun `test foreach with empty List`() {
    enterCommand("let result = []")
    enterCommand("call foreach([], 'call add(result, v:val)')")
    assertCommandOutput("echo result", "[]")
  }

  @Test
  fun `test foreach with List using Funcref`() {
    enterCommand("function! Process(idx, val) | call add(g:result, a:idx * a:val) | endfunction")
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], function('Process'))")
    assertCommandOutput("echo result", "[0, 2, 6]")
  }

  @Test
  fun `test foreach with List using lambda`() {
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], {idx, val -> add(g:result, idx * val)})")
    assertCommandOutput("echo result", "[0, 2, 6]")
  }

  @Test
  fun `test foreach with mixed type List`() {
    enterCommand("let result = []")
    enterCommand("call foreach([1, 'two', [3]], 'call add(result, v:val)')")
    assertCommandOutput("echo result", "[1, 'two', [3]]")
  }


  // Dictionary
  @Test
  fun `test foreach with Dictionary using string expression and key and val variables`() {
    enterCommand("let result = []")
    enterCommand("call foreach({'a': 1, 'b': 2}, 'call add(result, v:key . v:val)')")
    assertCommandOutput("echo result", "['a1', 'b2']")
  }

  @Test
  fun `test foreach with Dictionary returns original Dictionary`() {
    enterCommand("let mydict = {'a': 1, 'b': 2}")
    assertCommandOutput("echo foreach(mydict, 'let x = v:val')", "{'a': 1, 'b': 2}")
  }

  @Test
  fun `test foreach with empty Dictionary`() {
    enterCommand("let result = []")
    enterCommand("call foreach({}, 'call add(result, v:val)')")
    assertCommandOutput("echo result", "[]")
  }

  @Test
  fun `test foreach with Dictionary using Funcref`() {
    enterCommand("function! Process(idx, val) | call add(g:result, a:idx . a:val) | endfunction")
    enterCommand("let result = []")
    enterCommand("call foreach({'a': 1, 'b': 2}, function('Process'))")
    assertCommandOutput("echo result", "['a1', 'b2']")
  }

  @Test
  fun `test foreach with Dictionary using lambda`() {
    enterCommand("let result = []")
    enterCommand("call foreach({'a': 1, 'b': 2}, {k, v -> add(g:result, k . v)})")
    assertCommandOutput("echo result", "['a1', 'b2']")
  }


  // String
  @Test
  fun `test foreach with String using string expression and key and val variables`() {
    enterCommand("let result = []")
    enterCommand("call foreach('abc', 'call add(result, v:key . v:val)')")
    assertCommandOutput("echo result", "['0a', '1b', '2c']")
  }

  @Test
  fun `test foreach with String returns original String`() {
    assertCommandOutput("echo foreach('hello', 'let x = v:val')", "hello")
  }

  @Test
  fun `test foreach with empty String`() {
    enterCommand("let result = []")
    enterCommand("call foreach('', 'call add(result, v:val)')")
    assertCommandOutput("echo result", "[]")
  }

  @Test
  fun `test foreach with String using Funcref`() {
    enterCommand("function! Process(idx, val) | call add(g:result, a:idx . a:val) | endfunction")
    enterCommand("let result = []")
    enterCommand("call foreach('hello', function('Process'))")
    assertCommandOutput("echo result", "['0h', '1e', '2l', '3l', '4o']")
  }

  @Test
  fun `test foreach with String using lambda`() {
    enterCommand("let result = []")
    enterCommand("call foreach('hello', {idx, ch -> add(g:result, idx . ch)})")
    assertCommandOutput("echo result", "['0h', '1e', '2l', '3l', '4o']")
  }


  // Tests for method syntax
  @Test
  fun `test foreach as method with List`() {
    enterCommand("let result = []")
    enterCommand("call [1, 2, 3]->foreach('call add(result, v:val * 2)')")
    assertCommandOutput("echo result", "[2, 4, 6]")
  }

  @Test
  fun `test foreach as method with Dictionary`() {
    enterCommand("let result = []")
    enterCommand("call {'a': 1, 'b': 2}->foreach('call add(result, v:val)')")
    assertCommandOutput("echo result", "[1, 2]")
  }

  @Test
  fun `test foreach as method with string`() {
    enterCommand("let result = []")
    enterCommand("call 'hello'->foreach('call add(result, v:val)')")
    assertCommandOutput("echo result", "['h', 'e', 'l', 'l', 'o']")
  }


  // Tests for chaining
  @Test
  fun `test foreach can be chained`() {
    enterCommand("let result1 = []")
    enterCommand("let result2 = []")
    enterCommand("call [1, 2]->foreach('call add(result1, v:val)')->foreach('call add(result2, v:val * 10)')")
    assertCommandOutput("echo result1", "[1, 2]")
    assertCommandOutput("echo result2", "[10, 20]")
  }


  // Error tests
  @Test
  fun `test foreach with wrong argument count`() {
    enterCommand("call foreach([1, 2, 3])")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: foreach")
  }

  @Test
  fun `test foreach with invalid first argument`() {
    enterCommand("call foreach(42, 'let x = v:val')")
    assertPluginError(true)
    assertPluginErrorMessage("E1250: Argument of foreach() must be a List, String, Dictionary or Blob")
  }

  @VimBehaviorDiffers(description = "Error detected while processing :" +
    "E605: Exception not caught: error")
  @Test
  fun `test foreach stops on error in expression`() {
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], 'call add(result, v:val) | throw \"error\"')")
    assertPluginError(true)
    assertPluginErrorMessage("error")
    assertCommandOutput("echo result", "[1]")
  }

  // Not entirely sure about the contents of this error message preamble
  @VimBehaviorDiffers(description = "Error detected while processing function Process[2]..function Process:" +
    "E605: Exception not caught: error")
  @Test
  fun `test foreach stops on error in Funcref`() {
    enterCommand("function! Process(idx, val) | call add(g:result, a:idx . a:val) | throw \"error\" | endfunction")
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], function('Process'))")
    assertPluginError(true)
    assertPluginErrorMessage("error")
    assertCommandOutput("echo result", "['01']")
  }

  // Not entirely sure about the contents of this error message preamble
  @VimBehaviorDiffers(description = "Error detected while processing function Process[2]..function Process:" +
    "E605: Exception not caught: error")
  @Test
  fun `test foreach stops on error in Funcref for function with abort`() {
    // The docs state that we should ignore errors unless the function is declared with the 'abort' modifier. There is
    // no difference in behaviour in Vim if function has 'abort' or not.
    enterCommand("function! Process(idx, val) abort | call add(g:result, a:idx . a:val) | throw \"error\" | endfunction")
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2, 3], function('Process'))")
    assertPluginError(true)
    assertPluginErrorMessage("error")
    assertCommandOutput("echo result", "['01']")
  }


  // Tests with nested structures
  @Test
  fun `test foreach with nested Lists`() {
    enterCommand("let result = []")
    enterCommand("call foreach([[1, 2], [3, 4]], 'call add(result, v:val[0])')")
    assertCommandOutput("echo result", "[1, 3]")
  }

  @Test
  fun `test foreach with List of Dictionaries`() {
    enterCommand("let result = []")
    enterCommand("call foreach([{'a': 1}, {'a': 2}], 'call add(result, v:val.a)')")
    assertCommandOutput("echo result", "[1, 2]")
  }


  // Tests for v:key and v:val scope
  @Test
  fun `test v key and v val are restored after foreach`() {
    enterCommand("call foreach([1], 'let x = v:val')")
    // v:key and v:val should be cleared/restored after foreach

    enterCommand("echo v:key")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: v:key")

    enterCommand("echo v:val")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: v:val")
  }

  @Test
  fun `test nested foreach with independent v key v val`() {
    enterCommand("let result = []")
    enterCommand("call foreach([1, 2], 'call add(result, v:val) | call foreach([\"a\", \"b\"], \"call add(result, v:val)\")')")
    assertCommandOutput("echo result", "[1, 'a', 'b', 2, 'a', 'b']")
  }

  // Tests with special characters in strings
  @Test
  fun `test foreach with string containing special characters`() {
    enterCommand("let result = []")
    enterCommand("call foreach(\"a\\nb\", 'call add(result, v:val)')")
    assertCommandOutput("echo len(result)", "3")
  }
}
