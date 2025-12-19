/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ReduceFunctionTest : VimTestCase("\n") {
  @Test
  fun `test reduce with Number reports error`() {
    enterCommand("echo reduce(12, { acc, val -> acc + val })")
    assertPluginError(true)
    assertPluginErrorMessage("E1098: String, List or Blob required")
  }

  @Test
  fun `test reduce with Float reports error`() {
    enterCommand("echo reduce(12.34, { acc, val -> acc + val })")
    assertPluginError(true)
    assertPluginErrorMessage("E1098: String, List or Blob required")
  }

  @Test
  fun `test reduce with Dictionary reports error`() {
    enterCommand("echo reduce({'key1': 1, 'key2': 2}, { acc, val -> acc + val })")
    assertPluginError(true)
    assertPluginErrorMessage("E1098: String, List or Blob required")
  }

  @Test
  fun `test reduce with unknown function name reports error`() {
    enterCommand("echo reduce([1, 2, 3], 'Foo')")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: Foo")
  }

  @Test
  fun `test reduce with empty function name reports error`() {
    enterCommand("echo reduce([1, 2, 3], '')")
    assertPluginError(true)
    assertPluginErrorMessage("E1132: Missing function argument")
  }

  @Test
  fun `test reduce with Number as function reports error`() {
    // Note that this might work on Vim! Each function gets a numeric ID, and it is possible (but awkward) to get the
    // function ID programmatically. If you get the right ID, you can call the function with a Number.
    // We don't support this.
    enterCommand("echo reduce([1, 2, 3], 12)")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: 12")
  }

  @Test
  fun `test reduce with Float as function reports error`() {
    enterCommand("echo reduce([1, 2, 3], 12.34)")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: 12.34")
  }

  @VimBehaviorDiffers(description = "Vim reports two errors: E730 and then 'E1132: Missing function argument'")
  @Test
  fun `test reduce with List as function reports error`() {
    enterCommand("echo reduce([1, 2, 3], [1, 2, 3])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
//    assertPluginErrorMessage("E1132: Missing function argument")
  }

  @VimBehaviorDiffers(description = "Vim reports two errors: E731 and then 'E1132: Missing function argument'")
  @Test
  fun `test reduce with Dictionary as function reports error`() {
    enterCommand("echo reduce([1, 2, 3], {'key1': 1, 'key2': 2})")
    assertPluginError(true)
    assertPluginErrorMessage("E731: Using a Dictionary as a String")
//    assertPluginErrorMessage("E1132: Missing function argument")
  }

  @Test
  fun `test reduce with List and lambda expression`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo reduce(a, { acc, val -> acc + val })", "6")
  }

  @Test
  fun `test reduce with List and lambda expression with too few parameters`() {
    // Only uses the accumulator. No initial value is given, so the first item is used as the initial accumulator value.
    // We start with the second item. We pass 1, add 1, return 2. Next round is for the last item, passes 2, returns 3
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo reduce(a, { acc -> acc + 1 })", "3")
  }

  @Test
  fun `test reduce with List and lambda expression with too many parameters`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("call reduce(a, { acc, val, other -> acc + val })")
    assertPluginError(true)
    assertPluginErrorMessage("E119: Not enough arguments for function: <lambda>0")
  }

  @Test
  fun `test reduce with List and initial value`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo reduce(a, { acc, val -> acc + val }, 10)", "16")
  }

  @Test
  fun `test reduce with List returning different datatype`() {
    enterCommand("let a = [1, 2, 3]")
    assertCommandOutput("echo string(reduce(a, { acc, val -> acc . val }))", "'123'")
  }

  @Test
  fun `test reduce with List and function name`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("""
      |function! Foo(acc, val) |
      |  return a:acc + a:val |
      |endfunction
    """.trimMargin())
    assertCommandOutput("echo reduce(a, 'Foo')", "6")
  }

  @Test
  fun `test reduce with List and function variable`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("""
      |function! Foo(acc, val) |
      |  return a:acc + a:val |
      |endfunction
    """.trimMargin())
    enterCommand("let F = function('Foo')")
    assertCommandOutput("echo reduce(a, F)", "6")
  }

  @Test
  fun `test reduce with List and partial function`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("""
      |function! Foo(multiplier, acc, val) |
      |  return a:acc + (a:val * a:multiplier) |
      |endfunction
    """.trimMargin())
    enterCommand("let Foo2 = funcref('Foo', [2])")
    assertCommandOutput("echo reduce(a, Foo2)", "11")
  }

  @Test
  fun `test reduce with String and lambda expression`() {
    // This is closer to a map than a reduce, but it's a test. The first value is used as the accumulator for the second
    // value. It's not passed to the function.
    assertCommandOutput("echo string(reduce('abc', { acc, val -> acc . toupper(val) }))", "'aBC'")
  }

  @Test
  fun `test reduce with String and lambda expression returning different datatype`() {
    // Again, the first value is not passed to the accumulator function as a value, only as the first accumulator value.
    // So this becomes 'a' + 98 + 99 = 197
    assertCommandOutput("echo string(reduce('abc', { acc, val -> acc + char2nr(val) }))", "197")
  }

  @Test
  fun `test reduce with String and lambda expression and initial value`() {
    // 100 + 97('a') + 98('b') + 99('c') = 394
    assertCommandOutput("echo string(reduce('abc', { acc, val -> acc + char2nr(val) }, 100))", "394")
  }

  @Test
  fun `test reduce with String and named function`() {
    enterCommand("let a = 'abc'")
    enterCommand("""
      |function! Foo(acc, val) |
      |  return a:acc + char2nr(a:val) |
      |endfunction
    """.trimMargin())
    assertCommandOutput("echo reduce(a, 'Foo', 100)", "394")
  }

  @VimBehaviorDiffers(originalVimAfter = "Error detected while processing function Foo[1]..function Foo:" +
    "E605: Exception not caught: error")
  @Test
  fun `test reduce with function throwing error`() {
    enterCommand("let a = 'abc'")
    enterCommand("""
      |function! Foo(acc, val) |
      |  throw 'error' |
      |endfunction
    """.trimMargin())
    enterCommand("call reduce(a, 'Foo', 100)")
    assertPluginError(true)
    assertPluginErrorMessage("error")
  }
}
