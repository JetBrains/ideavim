/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class LetCommandSublistExpressionLValueTest : VimTestCase("\n") {
  @Test
  fun `test assign to sublist expression modifies List`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4]")
    assertCommandOutput("echo string(s)", "[5, 4, 3]")
  }

  @Test
  fun `test assign to sublist expression modifies referenced List`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let t = s")
    enterCommand("let s[0:1] = [5, 4]")
    assertCommandOutput("echo string(t)", "[5, 4, 3]")
  }

  @Test
  fun `test assign to sublist expression changes datatypes`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = ['hello', 'world']")
    assertCommandOutput("echo string(s)", "['hello', 'world', 3]")
  }

  @Test
  fun `test assign single element to sublist expression`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[1:1] = [5]")
    assertCommandOutput("echo string(s)", "[1, 5, 3]")
  }

  @Test
  fun `test assign to sublist expression treats empty start index as 0`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[:1] = [5, 4]")
    assertCommandOutput("echo string(s)", "[5, 4, 3]")
  }

  @Test
  fun `test assign to sublist expression treats empty end index as -1`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:] = [9, 8, 7]")
    assertCommandOutput("echo string(s)", "[9, 8, 7]")
  }

  // TODO: Fix this
  @Disabled
  @Test
  fun `test assign to sublist expression with negative start index`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[-3:2] = [9, 4, 7]")
    assertCommandOutput("echo string(s)", "[9, 8, 7]")
  }

  // TODO: More negative indexes

  @Test
  fun `test assign to sublist expression converts String indexes to Number`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s['0':'1'] = [5, 6]")
    assertCommandOutput("echo string(s)", "[5, 6, 3]")
  }

  @Test
  fun `test assign to sublist expression cannot convert Float indexes to Number`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[1.0:2.5] = [5, 6]")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @Disabled("Unhandled OutOfBoundsException")
  @Test
  fun `test assign to sublist expression with out of range start index raises error`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[3:4] = [5, 4]")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 3")
  }

  @Disabled("Unhandled OutOfBoundsException")
  @Test
  fun `test assign to sublist expression with out of range end index extends List`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[2:4] = [9, 8, 7]")
    assertCommandOutput("echo string(s)", "[1, 2, 9, 8, 7]")
  }

  @Test
  fun `test assign to sublist expression with missing end index and large List extends List`() {
    // Missing end index should mean -1 i.e., end of the current List but the larger rvalue List extends the lvalue List
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[1:] = [5, 5, 5, 5]")
    assertCommandOutput("echo s", "[1, 5, 5, 5, 5]")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assign List with too many items to sublist expression raises error`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5, 4, 3, 2, 1]")
    assertPluginError(true)
    assertPluginErrorMessage("E710: List value has more items than targets")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assign List with too few items to sublist expression raises error`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = [5]")
    assertPluginError(true)
    assertPluginErrorMessage("E711: List value does not have enough items")
  }

  @Test
  fun `test assign to sublist expression requires List rvalue`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = 5")
    assertPluginError(true)
    assertPluginErrorMessage("E709: [:] requires a List or Blob value")
  }

  @Test
  fun `test assign to sublist expression requires List rvalue 2`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s[0:1] = 'hello'")
    assertPluginError(true)
    assertPluginErrorMessage("E709: [:] requires a List or Blob value")
  }

  @VimBehaviorDiffers("E719: Cannot slice a Dictionary")
  @Test
  fun `test assign to sublist indexed expression on a Dictionary variable raises error`() {
    enterCommand("let s = {'key1' : 1, 'key2' : 2}")
    enterCommand("let s[1:2] = 'xy'")
    assertPluginError(true)
    assertPluginErrorMessage("wrong variable type")
  }

  @VimBehaviorDiffers("E689: Index not allowed after a string: s[1:2] = 'xy'")
  @Test
  fun `test assign to sublist indexed expression on a String variable raises error`() {
    enterCommand("let s = 'abcde'")
    enterCommand("let s[1:2] = 'xy'")
    assertPluginError(true)
    assertPluginErrorMessage("wrong variable type")
  }

  @VimBehaviorDiffers("E689: Index not allowed after a number: s[1:2] = 'xy'")
  @Test
  fun `test assign to sublist indexed expression on a Number variable raises error`() {
    enterCommand("let s = 12")
    enterCommand("let s[1:2] = 'xy'")
    assertPluginError(true)
    assertPluginErrorMessage("wrong variable type")
  }

  // Compound assignment operators apply to each element in the sublist. We test addition + subtraction and assume the
  // other operators work as expected. We've already tested converting between datatypes in LetCommandOperatorsTest.
  @VimBehaviorDiffers("[6, 8.2, 10.4]")
  @Test
  fun `test arithmetic compound assignment operator applies to each element in sublist expression`() {
    enterCommand("let s = [1, '2.5', 3]")
    enterCommand("let s[0:2] += [5, 6.2, '7']")
//    assertCommandOutput("echo string(s)", "[6, 8.2, 10.4]")
    assertCommandOutput("echo string(s)", "[5, 6.2, '7']")
  }

  @VimBehaviorDiffers("[-9, -10.4, -3.6]")
  @Test
  fun `test arithmetic compound assignment operator applies to each element in sublist expression 2`() {
    enterCommand("let s = [1, '2.5', 3.4]")
    enterCommand("let s[0:2] -= [5, 6.2, '7']")
    assertCommandOutput("echo string(s)", "[5, 6.2, '7']")
//    assertCommandOutput("echo string(s)", "[-9, -10.4, -3.6]")
  }

  // Test concatenation compound assignment operator also applies to each element in sublist expression. It converts the
  // items to string and concatenates.
  @VimBehaviorDiffers("[15, 2.56, 37]")
  @Test
  fun `test string concatenation compound assignment operator applies to each element in sublist expression`() {
    enterCommand("let s = [1, '2.5', 3]")
    enterCommand("let s[0:1] .= [5, 6, '7']")
    assertCommandOutput("echo string(s)", "[1, '2.5', 3]")
//    assertCommandOutput("echo string(s)", "['15', '2.56', '37']")
  }

  @VimBehaviorDiffers("E734: Wrong variable type for .=")
  @Test
  fun `test string concatenation compound assignment operator cannot convert Float lvalue`() {
    enterCommand("let s = [1.2]")
    enterCommand("let s[0:0] .= ['x']")
//    assertPluginError(true)
//    assertPluginErrorMessage("E734: Wrong variable type for .=")
    assertPluginError(false)
    assertCommandOutput("echo string(s)", "['x']")
  }

  @VimBehaviorDiffers("E734: Wrong variable type for .=")
  @Test
  fun `test string concatenation compound assignment operator cannot convert Float rvalue`() {
    enterCommand("let s = ['x']")
    enterCommand("let s[0:0] .= [1.2]")
//    assertPluginError(true)
//    assertPluginErrorMessage("E734: Wrong variable type for .=")
    assertPluginError(false)
    assertCommandOutput("echo string(s)", "[1.2]")
  }
}
