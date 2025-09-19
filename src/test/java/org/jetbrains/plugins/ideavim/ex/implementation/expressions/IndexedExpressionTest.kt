/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class IndexedExpressionTest : VimTestCase("\n") {
  @Test
  fun `test indexed List expression`() {
    assertCommandOutput("echo string([1, 2, 3][1])", "2")
  }

  @Test
  fun `test indexed List variable`() {
    enterCommand("let l = [1, 2, 3]")
    assertCommandOutput("echo string(l[1])", "2")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test empty list access raises error`() {
    enterCommand("echo string([][0])")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 0")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed List expression with invalid index reports error`() {
    enterCommand("echo string([1, 2, 3][3])")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 3")
  }

  @Test
  fun `test indexed List expression with negative index counts from end of List`() {
    assertCommandOutput("echo string([1, 2, 3][-1])", "3")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed List expression with invalid negative index reports error`() {
    enterCommand("echo string([1, 2, 3][-4])")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: -4")
  }

  @Test
  fun `test indexed List expression with String index converts to Number`() {
    assertCommandOutput("echo string([1, 2, 3]['2.5'])", "3")
  }

  @Test
  fun `test indexed List expression with String index converts to Number 0`() {
    assertCommandOutput("echo string([1, 2, 3]['foo'])", "1")
  }

  @VimBehaviorDiffers(description = "Vim also outputs the value: 1")
  @Test
  fun `test indexed List expression with Float index reports error`() {
    enterCommand("echo string([1, 2, 3][2.5])")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed List expression with invalid List index reports error`() {
    enterCommand("echo string([1, 2, 3][ [2] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  // Dictionary
  @Test
  fun `test indexed Dictionary expression`() {
    assertCommandOutput("echo string({'foo': 1, 'bar': 2}['foo'])", "1")
  }

  @Test
  fun `test indexed Dictionary variable`() {
    enterCommand("let d = {'foo': 1, 'bar': 2}")
    assertCommandOutput("echo string(d['foo'])", "1")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed Dictionary expression with invalid key returns empty String`() {
    enterCommand("let d = {'foo': 1, 'bar': 2}")
    enterCommand("echo string(d['nope'])")
    assertPluginError(true)
    assertPluginErrorMessage("E716: Key not present in Dictionary: \"nope\"")
  }

  @Test
  fun `test indexed Dictionary expression with Number key converts to String`() {
    enterCommand("let d = {'1': \"one\", '2': \"two\"}")
    assertCommandOutput("echo string(d[1])", "'one'")
  }

  @Test
  fun `test indexed Dictionary expression with negative Number key converts to String`() {
    enterCommand("let d = {'-1': \"minus one\", '2': \"two\"}")
    assertCommandOutput("echo string(d[-1])", "'minus one'")
  }

  @Test
  fun `test indexed Dictionary expression with Float key converts to String`() {
    enterCommand("let d = {'1.5': \"one and a half\", '2': \"two\"}")
    assertCommandOutput("echo string(d[1.5])", "'one and a half'")
  }

  @Test
  fun `test indexed Dictionary expression with List key reports errors`() {
    enterCommand("let d = {'foo': 1, 'bar': 2}")
    enterCommand("echo string(d[[1, 2, 3]])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  // String can be indexed
  @Test
  fun `test indexed String expression`() {
    assertCommandOutput("echo string('hello'[2])", "'l'")
  }

  @Test
  fun `test indexed String variable`() {
    enterCommand("let s = 'hello'")
    assertCommandOutput("echo string(s[2])", "'l'")
  }

  @Test
  fun `test indexed String expression with invalid index returns empty String`() {
    assertCommandOutput("echo string('hello'[20])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed String expression with negative index returns empty String`() {
    // Surprisingly not the same as List
    assertCommandOutput("echo string('hello'[-2])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed String expression with invalid negative index returns empty String`() {
    // Surprisingly not the same as List
    assertCommandOutput("echo string('hello'[-20])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed String expression with String index converts to Number`() {
    assertCommandOutput("echo string('hello'['2.5'])", "'l'")
  }

  @Test
  fun `test indexed String expression with String index converts to Number 0`() {
    assertCommandOutput("echo string('hello'['foo'])", "'h'")
  }

  @VimBehaviorDiffers(description = "Vim also outputs 'h'")
  @Test
  fun `test indexed String expression with Float index reports error`() {
    enterCommand("echo string('hello'[2.5])")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed String expression with List index reports error`() {
    enterCommand("echo string('hello'[ [2] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  // Number can be indexed! It's treated as a String
  @Test
  fun `test indexed Number expression treated as String`() {
    assertCommandOutput("echo string(123456789[2])", "'3'")
  }

  @Test
  fun `test indexed Number variable treated as String`() {
    enterCommand("let n = 123456789")
    assertCommandOutput("echo string(n[2])", "'3'")
  }

  @Test
  fun `test indexed Number expression with invalid index returns empty String`() {
    assertCommandOutput("echo string(123456789[20])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed Number expression with negative index returns empty String`() {
    assertCommandOutput("echo string(123456789[-2])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed Number expression with negative invalid index returns empty String`() {
    assertCommandOutput("echo string(123456789[-20])", "''")
    assertPluginError(false)
  }

  @Test
  fun `test indexed Number expression with String index converts to Number`() {
    assertCommandOutput("echo string(123456789['2.5'])", "'3'")
    assertPluginError(false)
  }

  @Test
  fun `test indexed Number expression with String index converts to Number 0`() {
    assertCommandOutput("echo string(123456789['foo'])", "'1'")
    assertPluginError(false)
  }

  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed Number expression with invalid List index reports error`() {
    enterCommand("echo string(123456789[ [2] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  // Float can't be indexed. Of course
  @VimBehaviorDiffers(description = "Vim also outputs E116: Invalid arguments for function string...")
  @Test
  fun `test indexed Float expression reports errors`() {
    enterCommand("echo string(1.5[2])")
    assertPluginError(true)
    // If it's not a List or Dictionary, it's treated as a String
    assertPluginErrorMessage("E806: Using a Float as a String")
  }
}
