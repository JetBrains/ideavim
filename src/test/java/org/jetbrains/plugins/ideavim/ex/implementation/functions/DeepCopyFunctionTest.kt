/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DeepCopyFunctionTest : VimTestCase("\n") {
  @Test
  fun `test deepcopy Number returns same value`() {
    assertCommandOutput("echo deepcopy(12)", "12")
    assertCommandOutput("echo 12 is 12->deepcopy()", "1")
  }

  @Test
  fun `test deepcopy Float returns same value`() {
    assertCommandOutput("echo deepcopy(12.23)", "12.23")
    assertCommandOutput("echo 12.23 is 12.23->deepcopy()", "1")
  }

  @Test
  fun `test deepcopy String returns same value`() {
    assertCommandOutput("echo deepcopy('hello')", "hello")
    assertCommandOutput("echo 'hello' is 'hello'->deepcopy()", "1")
  }

  @Test
  fun `test deepcopy List returns new instance with new instances of reference items`() {
    enterCommand("let a = [1, [2], 3]")
    assertCommandOutput("echo deepcopy(a)", "[1, [2], 3]")
    assertCommandOutput("echo a is a->deepcopy()", "0")
    assertCommandOutput("echo a[0] is a->deepcopy()[0]", "1") // Number is a value type, `is` returns true
    assertCommandOutput("echo a[1] is a->deepcopy()[1]", "0") // List is a reference type
    enterCommand("let b = deepcopy(a)")

    // Affects the value in `a` but not the copy of the value in `b`
    enterCommand("let a[0] = 9")
    assertCommandOutput("echo a b", "[9, [2], 3] [1, [2], 3]")

    // Affects the List in `a` but not the copy in `b`
    enterCommand("let a[1][0] = 9")
    assertCommandOutput("echo a b", "[9, [9], 3] [1, [2], 3]")
  }

  @Test
  fun `test deepcopy List shares copied references by default`() {
    enterCommand("let z = [1, 2, 3]")
    enterCommand("let a = [z, z, z, [1, 2, 3]]")
    enterCommand("let b = deepcopy(a)") // noref=0 if not specified
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b[0] is z", "0")
    assertCommandOutput("echo b[0] is b[1]", "1") // The copy of `z` is reused
    assertCommandOutput("echo b[1] is b[2]", "1")
    assertCommandOutput("echo b[2] is b[3]", "0") // The final list is a new copy
  }

  @Test
  fun `test deepcopy List shares copied references when passing 0 for noref parameter`() {
    enterCommand("let z = [1, 2, 3]")
    enterCommand("let a = [z, z, z, [1, 2, 3]]")
    enterCommand("let b = deepcopy(a, 0)")  // noref=0
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b[0] is z", "0")
    assertCommandOutput("echo b[0] is b[1]", "1") // The copy of `z` is reused
    assertCommandOutput("echo b[1] is b[2]", "1")
    assertCommandOutput("echo b[2] is b[3]", "0") // The final list is a new copy
  }

  @Test
  fun `test deepcopy List creates new copies for same references when passing 1 for noref parameter`() {
    enterCommand("let z = [1, 2, 3]")
    enterCommand("let a = [z, z, z, [1, 2, 3]]")
    enterCommand("let b = deepcopy(a, 1)")  // noref=1
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b[0] is z", "0")
    assertCommandOutput("echo b[0] is b[1]", "0") // The copy of `z` is NOT reused!
    assertCommandOutput("echo b[1] is b[2]", "0")
    assertCommandOutput("echo b[2] is b[3]", "0") // The final list is also a new copy
  }

  @Test
  fun `test deepcopy List with recursive structure reuses copied reference by default`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    enterCommand("let b = deepcopy(a)") // noref=0 if not specified
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b[1] isnot a", "1")
    assertCommandOutput("echo b[1] is b", "1")
    assertCommandOutput("echo b[1][1] is b", "1")
    // Etc...
  }

  @Test
  fun `test deepcopy List with recursive structure reuses copied reference when passing 0 as noref parameter`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    enterCommand("let b = deepcopy(a, 0)")  // noref=0
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b[1] is b", "1")
    assertCommandOutput("echo b[1][1] is b", "1")
    // Etc...
  }

  @Test
  fun `test deepcopy List with recursive structure reports error when not sharing references`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let a[1] = a")
    enterCommand("let b = deepcopy(a, 1)")  // noref=1
    assertPluginError(true)
    assertPluginErrorMessage("E698: Variable nested too deep for making a copy")
  }

  @Test
  fun `test deepcopy Dictionary returns new instance with new instances of reference items`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    assertCommandOutput("echo deepcopy(a)", "{'foo': [1], 'bar': 2}")
    assertCommandOutput("echo a is a->deepcopy()", "0")
    assertCommandOutput("echo a['foo'] is a->deepcopy()['foo']", "0")
    assertCommandOutput("echo a['bar'] is a->deepcopy()['bar']", "1") // Number is a value type, `is` returns true

    enterCommand("let b = deepcopy(a)")

    // Affects the value in `a` but not the copy of the value in `b`
    enterCommand("let a['bar'] = 9")
    assertCommandOutput("echo a b", "{'foo': [1], 'bar': 9} {'foo': [1], 'bar': 2}")

    // Affects the List in `a` but not the copy in `b`
    enterCommand("let a['foo'][0] = 9")
    assertCommandOutput("echo a b", "{'foo': [9], 'bar': 9} {'foo': [1], 'bar': 2}")
  }

  @Test
  fun `test deepcopy Dictionary shares copied references by default`() {
    enterCommand("let z = {'foo': 1}")
    enterCommand("let a = {'foo': z, 'bar': z, 'baz': {'foo': 1}}")
    enterCommand("let b = deepcopy(a)") // noref=0 if not specified
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b['foo'] is z", "0")
    assertCommandOutput("echo b['bar'] is b['foo']", "1") // The copy of `z` is reused
    assertCommandOutput("echo b['baz'] is b['foo']", "0") // The final value is a new copy
  }

  @Test
  fun `test deepcopy Dictionary shares copied references when passing 0 for noref parameter`() {
    enterCommand("let z = {'foo': 1}")
    enterCommand("let a = {'foo': z, 'bar': z, 'baz': {'foo': 1}}")
    enterCommand("let b = deepcopy(a, 0)") // noref=0
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b['foo'] is z", "0")
    assertCommandOutput("echo b['bar'] is b['foo']", "1") // The copy of `z` is reused
    assertCommandOutput("echo b['baz'] is b['foo']", "0") // The final value is a new copy
  }

  @Test
  fun `test deepcopy Dictionary creates new copies for same references when passing 1 for noref parameter`() {
    enterCommand("let z = {'foo': 1}")
    enterCommand("let a = {'foo': z, 'bar': z, 'baz': {'foo': 1}}")
    enterCommand("let b = deepcopy(a, 1)") // noref=1
    assertCommandOutput("echo b is a", "0")
    assertCommandOutput("echo b['foo'] is z", "0")
    assertCommandOutput("echo b['bar'] is b['foo']", "0") // The copy of `z` is NOT reused!
    assertCommandOutput("echo b['baz'] is b['foo']", "0") // The final value is a new copy
  }

  @Test
  fun `test deepcopy Dictionary with recursive structure reuses copied reference by default`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    enterCommand("let a['foo'] = a")
    enterCommand("let b = deepcopy(a)") // noref=0 if not specified
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b['foo'] isnot a", "1")
    assertCommandOutput("echo b['foo'] is b", "1")
    assertCommandOutput("echo b['foo']['foo'] is b", "1")
    // Etc...
  }

  @Test
  fun `test deepcopy Dictionary with recursive structure reuses copied when passing 0 as noref parameter`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    enterCommand("let a['foo'] = a")
    enterCommand("let b = deepcopy(a, 0)") // noref=0
    assertCommandOutput("echo b isnot a", "1")
    assertCommandOutput("echo b['foo'] isnot a", "1")
    assertCommandOutput("echo b['foo'] is b", "1")
    assertCommandOutput("echo b['foo']['foo'] is b", "1")
    // Etc...
  }

  @Test
  fun `test deepcopy Dictionary with recursive structure reports error when not sharing references`() {
    enterCommand("let a = {'foo': [1], 'bar': 2}")
    enterCommand("let a['foo'] = a")
    enterCommand("let b = deepcopy(a, 1)") // noref=1
    assertPluginError(true)
    assertPluginErrorMessage("E698: Variable nested too deep for making a copy")
  }
}
