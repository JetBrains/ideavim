/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.varFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CallFunctionTest : VimTestCase("\n") {
  @Test
  fun `test call with invalid argument list parameter`() {
    enterCommand("echo call('Foo', 12)")
    assertPluginError(true)
    assertPluginErrorMessage("E1211: List required for argument 2")
  }

  @Test
  fun `test call with invalid dictionary parameter`() {
    enterCommand("echo call('Foo', [], 12)")
    assertPluginError(true)
    assertPluginErrorMessage("E1206: Dictionary required for argument 3")
  }

  @Test
  fun `test call with invalid func parameter`() {
    // Note that Vim seems to give its functions numbers, so it can actually call a function by number. However, I don't
    // know how to find the function's number
    enterCommand("echo call(12, [])")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: 12")
  }

  @Test
  fun `test call with invalid func parameter as Float`() {
    enterCommand("echo call(12.34, [])")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: 12.34")
  }

  @Test
  fun `test call with invalid func parameter as List`() {
    enterCommand("echo call([1,2,3], [])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test call with invalid func parameter as Dictionary`() {
    enterCommand("echo call({'a':1, 'b':2}, [])")
    assertPluginError(true)
    assertPluginErrorMessage("E731: Using a Dictionary as a String")
  }

  @Test
  fun `test call with unknown function name`() {
    enterCommand("echo call('Foo', [])")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: Foo")
  }

  @Test
  fun `test call with no parameters`() {
    enterCommand("function! Foo() | let g:called = 1 | endfunction")
    enterCommand("call call('Foo', [])")
    assertPluginError(false)
    assertCommandOutput("echo g:called", "1")
  }

  @Test
  fun `test call returns value`() {
    enterCommand("function! Foo() | return 123 | endfunction")
    enterCommand("let retval = call('Foo', [])")
    assertCommandOutput("echo retval", "123")
  }

  @Test
  fun `test call with arguments`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let retval = call('Foo', [1, 2])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with too many arguments reports error`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let retval = call('Foo', [1, 2, 3])")
    assertPluginError(true)
    assertPluginErrorMessage("E118: Too many arguments for function: Foo")
  }

  @Test
  fun `test call with too few arguments reports error`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let retval = call('Foo', [1])")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: Foo")
  }

  @VimBehaviorDiffers(originalVimAfter = "E605: Exception not caught: error")
  @Test
  fun `test call with function that throws error`() {
    enterCommand("function! Foo(a, b) | throw 'error' | endfunction")
    enterCommand("call call('Foo', [1, 2])")
    assertPluginError(true)
    assertPluginErrorMessage("error")
  }

  @Test
  fun `test call builtin function`() {
    enterCommand("let retval = call('len', ['foo'])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with function reference`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let retval = call(function('Foo'), [1, 2])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with variable function reference`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let F=function('Foo')")
    enterCommand("let retval = call(F, [1, 2])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with partial function reference`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    enterCommand("let F=function('Foo', [1])")
    enterCommand("let retval = call(F, [2])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with lambda`() {
    enterCommand("let retval = call({a, b -> a + b}, [1, 2])")
    assertCommandOutput("echo retval", "3")
  }

  @Test
  fun `test call with dictionary function without dictionary reports error`() {
    enterCommand("function! Foo() dict | return self.thing | endfunction")
    enterCommand("let retval = call('Foo', [])")
    assertPluginError(true)
    assertPluginErrorMessage("E725: Calling dict function without Dictionary: Foo")
  }

  @Test
  fun `test call with dictionary function`() {
    enterCommand("function! Foo() dict | return self.thing | endfunction")
    enterCommand("let retval = call('Foo', [], {'thing': 123})")
    assertCommandOutput("echo retval", "123")
  }

  @Test
  fun `test call with dictionary function variable`() {
    // d.func is an (implicit) partial function reference
    enterCommand("function! Foo() dict | return self.thing | endfunction")
    enterCommand("let d = {'thing': 123, 'func': function('Foo')}")
    enterCommand("let retval = call(d.func, [])")
    assertCommandOutput("echo retval", "123")
  }

  @Test
  fun `test call with dictionary function variable and different dictionary`() {
    // d.func is an (implicit) partial function reference, so can be overridden by the new dictionary
    enterCommand("function! Foo() dict | return self.thing | endfunction")
    enterCommand("let d = {'thing': 123, 'func': function('Foo')}")
    enterCommand("let retval = call(d.func, [], {'thing': 456})")
    assertCommandOutput("echo retval", "456")
  }

  @Test
  fun `test call with partial dictionary function variable`() {
    // F is an explicit partial function reference, so cannot be overridden by the new dictionary
    enterCommand("function! Foo() dict | return self.thing | endfunction")
    enterCommand("let d = {'thing': 123}")
    enterCommand("let F = function('Foo', d)")
    enterCommand("let retval = call(F, [], {'thing': 456})")
    assertCommandOutput("echo retval", "123")
  }

  @Test
  fun `test call sets range to current line`() {
    configureByText(
      """
      |one
      |t${c}wo
      |three
      |four
    """.trimMargin()
    )
    enterCommand("function! Foo() | return a:firstline . ' ' . a:lastline | endfunction")
    assertCommandOutput("echo call('Foo', [])", "2 2")
  }

  @Test
  fun `test call with Visual selection`() {
    configureByText(
      """
      |one
      |t${c}wo
      |three
      |four
    """.trimMargin()
    )
    enterCommand("function! Foo() | echo a:firstline . ' ' . a:lastline | endfunction")
    typeText("Vjj")
    assertCommandOutput(
      "call call('Foo', [])", """
      |2 2
      |3 3
      |4 4
    """.trimMargin()
    )
  }

  @Test
  fun `test call with explicit range`() {
    configureByText(
      """
      |one
      |t${c}wo
      |three
      |four
    """.trimMargin()
    )
    enterCommand("function! Foo() | echo a:firstline . ' ' . a:lastline | endfunction")
    assertCommandOutput(
      "2,4call call('Foo', [])", """
      |2 2
      |3 3
      |4 4
    """.trimMargin()
    )
  }

  @Test
  fun `test nested call`() {
    enterCommand("function! Foo(a, b) | return a:a + a:b | endfunction")
    assertCommandOutput("echo call('call', ['Foo', [1,2]])", "3")
  }
}
