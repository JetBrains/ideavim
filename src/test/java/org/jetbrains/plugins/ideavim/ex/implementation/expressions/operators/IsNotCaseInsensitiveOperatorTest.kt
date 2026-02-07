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

class IsNotCaseInsensitiveOperatorTest : VimTestCase("\n") {
  @Test
  fun `test isnot case insensitive operator with Number values`() {
    assertCommandOutput("echo 1 isnot? 1", "0")
  }

  @Test
  fun `test isnot case insensitive operator with Float values`() {
    assertCommandOutput("echo 1.0 isnot? 1.0", "0")
    assertCommandOutput("echo 1.0 isnot? 2.0", "1")
  }

  @Test
  fun `test isnot case insensitive operator does not coerce Number and Float`() {
    assertCommandOutput("echo 1 isnot? 1.0", "1")
  }

  @Test
  fun `test isnot case insensitive operator with String`() {
    assertCommandOutput("echo 'abc' isnot? 'abc'", "0")
    assertCommandOutput("echo 'abc' isnot? 'ABC'", "0")
    assertCommandOutput("echo 'foo' isnot? 'bar'", "1")
  }

  @Test
  fun `test isnot case insensitive operator does not coerce String and Number`() {
    assertCommandOutput("echo '1' isnot? 1", "1")
  }

  @Test
  fun `test isnot case insensitive operator compares Lisnott references`() {
    assertCommandOutput("echo [] isnot? []", "1")
    assertCommandOutput("echo [1, 'abc', 3] isnot? [1, 'abc', 3]", "1")
    assertCommandOutput("echo [1, 'abc', 3] isnot? [1, 'ABC', 3]", "1")
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = a")
    assertCommandOutput("echo a isnot? a", "0")
    assertCommandOutput("echo b isnot? a", "0")
  }

  @Test
  fun `test isnot case insensitive operator compares Dictionary references`() {
    assertCommandOutput("echo {} isnot? {}", "1")
    assertCommandOutput("echo {'key1' : 1, 'key2' : 2} isnot? {'key1' : 1, 'key2' : 2}", "1")
    enterCommand("let a = {'key1' : 1, 'key2' : 2}")
    enterCommand("let b = a")
    assertCommandOutput("echo a isnot? a", "0")
    assertCommandOutput("echo b isnot? a", "0")
  }

  @Test
  fun `test isnot case insensitive operator treats different Funcref function references without arguments or dictionary as same`() {
    assertCommandOutput("echo function('abs') isnot? function('abs')", "0")
    enterCommand("let A = function('abs')")
    enterCommand("let B = function('abs')")
    assertCommandOutput("echo A isnot? B", "0")
    assertCommandOutput("echo A isnot? function('abs')", "0")
    assertCommandOutput("echo function('abs') isnot? B", "0")
  }

  @Test
  fun `test isnot case insensitive operator treats different Funcref funcref references as different`() {
    enterCommand("function Foo() | endfunction")
    assertCommandOutput("echo function('Foo') isnot? function('Foo')", "0")
    assertCommandOutput("echo funcref('Foo') isnot? funcref('Foo')", "1")
    enterCommand("let A = funcref('Foo')")
    enterCommand("let B = funcref('Foo')")
    assertCommandOutput("echo A isnot? B", "1")
  }

  @Test
  fun `test isnot case insensitive operator treats Funcref references with different arguments as different`() {
    enterCommand("let args = [1]")
    assertCommandOutput("echo function('abs', args) isnot? function('abs', args)", "1")
    enterCommand("let A = function('abs', [1])")
    enterCommand("let B = function('abs', [1])")
    assertCommandOutput("echo A isnot? B", "1")
  }

  @Test
  fun `test isnot case insensitive operator treats Funcref references with different dictionary as different`() {
    enterCommand("let d = {'key1':1}")
    assertCommandOutput("echo function('abs', d) isnot? function('abs', d)", "1")
    enterCommand("let A = function('abs', {'key1':1})")
    enterCommand("let B = function('abs', {'key1':1})")
    assertCommandOutput("echo A isnot? B", "1")
  }
}
