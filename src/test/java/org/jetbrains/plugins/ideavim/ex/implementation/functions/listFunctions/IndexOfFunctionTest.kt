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
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class IndexOfFunctionTest : VimTestCase("\n") {
  @Test
  fun `test indexof with too few arguments reports error`() {
    enterCommand("echo indexof([1, 2, 3])")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: indexof")
  }

  @Test
  fun `test indexof with too many arguments reports error`() {
    enterCommand("echo indexof([1, 2, 3], 'v:val == 1', {}, 'extra')")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: indexof")
  }

  @Test
  fun `test indexof with invalid first argument type`() {
    enterCommand("echo indexof(42, 'v:val == 1')")
    assertPluginError(true)
    assertPluginErrorMessage("E1226: List or Blob required for argument 1")
  }

  @Test
  fun `test indexof with invalid second argument type`() {
    enterCommand("echo indexof([1, 2, 3], 42)")
    assertPluginError(true)
    assertPluginErrorMessage("E1256: String or function required for argument 2")
  }

  @Test
  fun `test indexof with invalid third argument type`() {
    enterCommand("echo indexof([1, 2, 3], 'v:val == 1', 'not a dict')")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @Test
  fun `test indexof finds first matching element with String expression`() {
    assertCommandOutput("echo indexof([1, 2, 3, 4], 'v:val == 3')", "2")
  }

  @Test
  fun `test indexof returns -1 when no match found`() {
    assertCommandOutput("echo indexof([1, 2, 3], 'v:val == 5')", "-1")
  }

  @Test
  fun `test indexof with empty List returns -1`() {
    assertCommandOutput("echo indexof([], 'v:val == 1')", "-1")
  }

  @Test
  fun `test indexof finds first element when multiple matches exist`() {
    assertCommandOutput("echo indexof([1, 2, 3, 2, 5], 'v:val == 2')", "1")
  }

  @Test
  fun `test indexof with key variable`() {
    assertCommandOutput("echo indexof([10, 20, 30], 'v:key == 2')", "2")
  }

  @Test
  fun `test indexof with both key and val variables`() {
    assertCommandOutput("echo indexof(['a', 'b', 'c'], 'v:key == 2 && v:val == \"c\"')", "2")
  }

  @Test
  fun `test indexof with complex expression`() {
    assertCommandOutput("echo indexof([1, 2, 3, 4, 5], 'v:val > 2 && v:val < 5')", "2")
  }

  @Test
  fun `test indexof with String List`() {
    assertCommandOutput("echo indexof(['foo', 'bar', 'baz'], 'v:val == \"bar\"')", "1")
  }

  @Test
  fun `test indexof with mixed type List`() {
    assertCommandOutput("echo indexof([1, 'two', 3], 'v:val == \"two\"')", "1")
  }

  @Test
  fun `test indexof with Dictionary but no startidx entry`() {
    assertCommandOutput("echo indexof([1, 2, 3, 2, 5], 'v:val == 2', {'whatever': 2})", "1")
  }

  @Test
  fun `test indexof with startidx finds element after start`() {
    assertCommandOutput("echo indexof([1, 2, 3, 2, 5], 'v:val == 2', {'startidx': 2})", "3")
  }

  @Test
  fun `test indexof with startidx at 0`() {
    assertCommandOutput("echo indexof([1, 2, 3], 'v:val == 2', {'startidx': 0})", "1")
  }

  @Test
  fun `test indexof with negative startidx starts from end of List`() {
    assertCommandOutput("echo indexof([1, 2, 3, 2, 5], 'v:val == 2', {'startidx': -3})", "3")
  }

  @Test
  fun `test indexof with startidx beyond list size returns -1`() {
    assertCommandOutput("echo indexof([1, 2, 3], 'v:val == 2', {'startidx': 10})", "-1")
  }

  @Test
  fun `test indexof with startidx at last element`() {
    assertCommandOutput("echo indexof([1, 2, 3], 'v:val == 3', {'startidx': 2})", "2")
  }

  @Test
  fun `test indexof with Funcref`() {
    enterCommand("function! IsEven(idx, val) | return a:val % 2 == 0 | endfunction")
    assertCommandOutput("echo indexof([1, 3, 4, 5], function('IsEven'))", "2")
  }

  @Test
  fun `test indexof with Funcref using both parameters`() {
    enterCommand("function! Check(idx, val) | return a:idx > 0 && a:val > 5 | endfunction")
    assertCommandOutput("echo indexof([1, 2, 10], function('Check'))", "2")
  }

  @Test
  fun `test indexof with Funcref and startidx`() {
    enterCommand("function! IsOdd(idx, val) | return a:val % 2 == 1 | endfunction")
    assertCommandOutput("echo indexof([2, 3, 4, 5], function('IsOdd'), {'startidx': 2})", "3")
  }

  @VimBehaviorDiffers(originalVimAfter = "Error detected while processing function ThrowError[1]..function ThrowError:" +
    "E605: Exception not caught: error")
  @Test
  fun `test indexof with Funcref that throws error`() {
    enterCommand("function! ThrowError(idx, val) | throw \"error\" | endfunction")
    enterCommand("echo indexof([1, 2, 3], function('ThrowError'))")
    assertPluginError(true)
    assertPluginErrorMessage("error")
  }

  @Test
  fun `test indexof with dictionary function`() {
    enterCommand("function! FindIt(idx, val) dict | return a:val == self.target | endfunction")
    enterCommand("let d={'target':2, 'func':function('FindIt')}")
    assertCommandOutput("echo indexof([1, 2, 3], d.func)", "1")
  }

  @Test
  fun `test indexof with lambda`() {
    assertCommandOutput("echo indexof([1, 2, 3, 4], {idx, val -> val > 2})", "2")
  }

  @Test
  fun `test indexof with lambda using index parameter`() {
    assertCommandOutput("echo indexof([10, 20, 30], {idx, val -> idx == 1})", "1")
  }

  @Test
  fun `test indexof with lambda and startidx`() {
    assertCommandOutput("echo indexof([1, 2, 3, 4, 5], {i, v -> v % 2 == 0}, {'startidx': 2})", "3")
  }

  @Test
  fun `test indexof with List of Lists`() {
    assertCommandOutput("echo indexof([[1, 2], [3, 4], [5, 6]], 'v:val[0] == 3')", "1")
  }

  @Test
  fun `test indexof with List of Dictionaries`() {
    assertCommandOutput("echo indexof([{'a': 1}, {'a': 2}, {'a': 3}], 'v:val.a == 2')", "1")
  }

  @Test
  fun `test indexof as method`() {
    assertCommandOutput("echo [1, 2, 3, 4]->indexof('v:val == 3')", "2")
  }

  @Test
  fun `test indexof as method with startidx`() {
    assertCommandOutput("echo [1, 2, 3, 2, 5]->indexof('v:val == 2', {'startidx': 2})", "3")
  }

  @Test
  fun `test indexof as method with lambda`() {
    assertCommandOutput("echo [1, 2, 3, 4]->indexof({i, v -> v > 2})", "2")
  }

  @Test
  fun `test v key and v val are restored after indexof`() {
    enterCommand("call indexof([1, 2, 3], 'v:val == 2')")

    enterCommand("echo v:key")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: v:key")

    enterCommand("echo v:val")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: v:val")
  }

  @Test
  fun `test nested indexof with independent v key v val`() {
    // Find the index of the entry that is a List that contains 2
    enterCommand("function! Check(idx, val) | return indexof(v:val, 'v:val == 2') != -1 | endfunction")
    assertCommandOutput("echo indexof([[1,1], [1,2], [1,3]], function('Check'))", "1")
  }
}
