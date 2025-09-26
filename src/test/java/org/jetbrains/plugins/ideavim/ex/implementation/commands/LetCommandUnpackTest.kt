/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class LetCommandUnpackTest : VimTestCase("\n") {
  @Test
  fun `test unpack into variables`() {
    enterCommand("let [a, b, c] = [1, '2', 3.0]")
    assertCommandOutput("echo string(a)", "1")
    assertCommandOutput("echo string(b)", "'2'")
    assertCommandOutput("echo string(c)", "3.0")
  }

  @Test
  fun `test unpack into existing variables`() {
    enterCommand("let a='hello'")
    enterCommand("let b=42")
    enterCommand("let c=3.14")
    enterCommand("let [a, b, c] = [1, '2', 3.0]")
    assertCommandOutput("echo string(a)", "1")
    assertCommandOutput("echo string(b)", "'2'")
    assertCommandOutput("echo string(c)", "3.0")
  }

  @Test
  fun `test unpack into the same variable saves last value`() {
    enterCommand("let [a, a, a] = [1, '2', 3.0]")
    assertCommandOutput("echo string(a)", "3.0")
  }

  @Test
  fun `test unpack into indexed expression`() {
    enterCommand("let a = [10, 20, 30]")
    enterCommand("let [a[0], a[1], a[2]] = [1, '2', 3.0]")
    assertCommandOutput("echo string(a)", "[1, '2', 3.0]")
  }

  @Test
  fun `test unpack into sublist expression`() {
    enterCommand("let a = [10, 20, 30]")
    enterCommand("let [a[0:1]] = [[1,2]]")
    assertCommandOutput("echo string(a)", "[1, 2, 30]")
  }

  @Test
  fun `test unpack into dictionary expression`() {
    enterCommand("let d = {'a': 10, 'b': 20, 'c': 30}")
    enterCommand("let [d.a] = [42]")
    assertCommandOutput("echo string(d)", "{'a': 42, 'b': 20, 'c': 30}")
  }

  @Test
  fun `test unpack into variables with rest`() {
    enterCommand("let [a, b; rest] = [1, '2', 3.0, 'four', 5]")
    assertCommandOutput("echo string(a)", "1")
    assertCommandOutput("echo string(b)", "'2'")
    assertCommandOutput("echo string(rest)", "[3.0, 'four', 5]")
  }

  @Test
  fun `test unpack into variables with no rest assigns empty list`() {
    enterCommand("let [a, b; rest] = [1, '2']")
    assertCommandOutput("echo string(a)", "1")
    assertCommandOutput("echo string(b)", "'2'")
    assertCommandOutput("echo string(rest)", "[]")
  }

  @Test
  fun `test unpack into locked variable raises error`() {
    enterCommand("let a = 10")
    enterCommand("let b = 20")
    enterCommand("lockvar a")
    enterCommand("let [a, b] = [1, 2]")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: a")
  }

  @Test
  fun `test unpack into too few variables raises error`() {
    enterCommand("let [a, b] = [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E687: Less targets than List items")
  }

  @Test
  fun `test unpack into too many variables raises error`() {
    enterCommand("let [a, b, c, d] = [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E688: More targets than List items")
  }

  @Test
  fun `test unpack into too few variables with rest raises error`() {
    enterCommand("let [a, b, c; rest] = [1, 2]")
    assertPluginError(true)
    assertPluginErrorMessage("E688: More targets than List items")
  }

  @Test
  fun `test unpack with non-List rvalue raises error`() {
    enterCommand("let [a, b] = 42")
    assertPluginError(true)
    assertPluginErrorMessage("E714: List required")
  }

  @VimBehaviorDiffers(
    originalVimAfter = "E475: Invalid argument: 'a'] = [1]",
    description = "Looks like Vim reports the error while parsing, rather than parsing and then reporting the error"
  )
  @Test
  fun `test unpack into non-lvalue expression raises error`() {
    enterCommand("let ['a'] = [1]")
    assertPluginError(true)
    assertPluginErrorMessage("E475: Invalid argument: 'a'")
  }

  @Test
  fun `test unpack into invalid lvalue expression raises error`() {
    enterCommand("let [a[0]] = [1]")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: a")
  }

  @Test
  fun `test unpack validates lvalues before assigning anything`() {
    enterCommand("let [a, b, 'hello'] = [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E475: Invalid argument: 'hello'")
    assertCommandOutput("echo exists('a')", "0")
    assertCommandOutput("echo exists('b')", "0")
  }

  @Test
  fun `test unpack with compound assignment`() {
    enterCommand("let a = 10")
    enterCommand("let b = 20")
    enterCommand("let [a, b] += [1, 2]")
    assertCommandOutput("echo string(a)", "11")
    assertCommandOutput("echo string(b)", "22")
  }

  @Test
  fun `test unpack with compound assignment and too few variables raises error`() {
    enterCommand("let a = 10")
    enterCommand("let b = 20")
    enterCommand("let [a, b] += [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E687: Less targets than List items")
  }

  @Test
  fun `test unpack with compound assignment and too many variables raises error`() {
    enterCommand("let a = 10")
    enterCommand("let b = 20")
    enterCommand("let c = 20")
    enterCommand("let [a, b, c] += [1, 2]")
    assertPluginError(true)
    assertPluginErrorMessage("E688: More targets than List items")
  }

  @Test
  fun `test unpack with compound assignment with List values`() {
    enterCommand("let a=[1,2]")
    enterCommand("let b=[6,7]")
    enterCommand("let [a, b] += [[3,4], [8,9]]")
    assertCommandOutput("echo string(a)", "[1, 2, 3, 4]")
    assertCommandOutput("echo string(b)", "[6, 7, 8, 9]")
  }

  @Test
  fun `test unpack with arithmetic compound assignment and String values`() {
    enterCommand("let a='hello'")
    enterCommand("let [a] += ['world']")
    assertCommandOutput("echo string(a)", "0")
  }

  @Test
  fun `test unpack with concatenation compound assignment and String values`() {
    enterCommand("let a='hello'")
    enterCommand("let [a] .= [' world']")
    assertCommandOutput("echo string(a)", "'hello world'")
  }

  @VimBehaviorDiffers(description = "Vim also reports E121 for variable b")
  @Test
  fun `test unpack with compound assignment with undefined variables reports errors`() {
    enterCommand("let [a, b] += [1, 2]")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: a")
  }

  @Test
  fun `test unpack with compound assignment and rest`() {
    enterCommand("let a=10")
    enterCommand("let b=20")
    enterCommand("let rest=[100,200]")
    enterCommand("let [a, b; rest] += [1, 2, 3, 4]")
    assertCommandOutput("echo string(a)", "11")
    assertCommandOutput("echo string(b)", "22")
    assertCommandOutput("echo string(rest)", "[100, 200, 3, 4]")
  }

  @Test
  fun `test unpack with compound assignment and incorrect lvalue type for rest`() {
    enterCommand("let a=10")
    enterCommand("let b=20")
    enterCommand("let rest=100")
    enterCommand("let [a, b; rest] += [1, 2, 3, 4]")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for +=")
  }

  @Test
  fun `test unpack with compound assignment and incorrect lvalue type for rest assigns values up to error`() {
    enterCommand("let a=10")
    enterCommand("let b=20")
    enterCommand("let rest=100")
    enterCommand("let [a, b; rest] += [1, 2, 3, 4]")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for +=")
    assertCommandOutput("echo string(a)", "11")
    assertCommandOutput("echo string(b)", "22")
    assertCommandOutput("echo string(rest)", "100")
  }
}
