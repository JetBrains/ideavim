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

class IsOperatorTest : VimTestCase("\n") {
  @Test
  fun `test is operator with Number values`() {
    assertCommandOutput("echo 1 is 1", "1")
  }

  @Test
  fun `test is operator with Float values`() {
    assertCommandOutput("echo 1.0 is 1.0", "1")
    assertCommandOutput("echo 1.0 is 2.0", "0")
  }

  @Test
  fun `test is operator does not coerce Number and Float`() {
    assertCommandOutput("echo 1 is 1.0", "0")
  }

  @Test
  fun `test is operator compares String according to 'ignorecase'`() {
    enterCommand("set noignorecase")
    assertCommandOutput("echo 'abc' is 'abc'", "1")
    assertCommandOutput("echo 'abc' is 'ABC'", "0")
    assertCommandOutput("echo 'foo' is 'bar'", "0")
    enterCommand("set ignorecase")
    assertCommandOutput("echo 'abc' is 'ABC'", "1")
    assertCommandOutput("echo 'ABC' is 'abc'", "1")
  }

  @Test
  fun `test is operator does not coerce String and Number`() {
    assertCommandOutput("echo '1' is 1", "0")
  }

  @Test
  fun `test is operator compares List references`() {
    assertCommandOutput("echo [] is []", "0")
    assertCommandOutput("echo [1, 2, 3] is [1, 2, 3]", "0")
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = a")
    assertCommandOutput("echo a is a", "1")
    assertCommandOutput("echo b is a", "1")
  }

  @Test
  fun `test is operator compares Dictionary references`() {
    assertCommandOutput("echo {} is {}", "0")
    assertCommandOutput("echo {'key1' : 1, 'key2' : 2} is {'key1' : 1, 'key2' : 2}", "0")
    enterCommand("let a = {'key1' : 1, 'key2' : 2}")
    enterCommand("let b = a")
    assertCommandOutput("echo a is a", "1")
    assertCommandOutput("echo b is a", "1")
  }

  @Test
  fun `test is operator treats different Funcref function references without arguments or dictionary as same`() {
    assertCommandOutput("echo function('abs') is function('abs')", "1")
    enterCommand("let A = function('abs')")
    enterCommand("let B = function('abs')")
    assertCommandOutput("echo A is B", "1")
    assertCommandOutput("echo A is function('abs')", "1")
    assertCommandOutput("echo function('abs') is B", "1")
  }

  @Test
  fun `test is operator treats different Funcref funcref references as different`() {
    enterCommand("function Foo() | endfunction")
    assertCommandOutput("echo function('Foo') is function('Foo')", "1")
    assertCommandOutput("echo funcref('Foo') is funcref('Foo')", "0")
    enterCommand("let A = funcref('Foo')")
    enterCommand("let B = funcref('Foo')")
    assertCommandOutput("echo A is B", "0")
  }

  @Test
  fun `test is operator treats Funcref references with different arguments as different`() {
    enterCommand("let args = [1]")
    assertCommandOutput("echo function('abs', args) is function('abs', args)", "0")
    enterCommand("let A = function('abs', [1])")
    enterCommand("let B = function('abs', [1])")
    assertCommandOutput("echo A is B", "0")
  }

  @Test
  fun `test is operator treats Funcref references with different dictionary as different`() {
    enterCommand("let d = {'key1':1}")
    assertCommandOutput("echo function('abs', d) is function('abs', d)", "0")
    enterCommand("let A = function('abs', {'key1':1})")
    enterCommand("let B = function('abs', {'key1':1})")
    assertCommandOutput("echo A is B", "0")
  }

  @Test
  fun `tset pathological variable names`() {
    enterCommand("let is = 12")
    assertCommandOutput("echo is is is", "1")
  }
}
