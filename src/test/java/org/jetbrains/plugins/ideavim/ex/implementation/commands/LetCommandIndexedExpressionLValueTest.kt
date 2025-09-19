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
import org.junit.jupiter.api.Test

class LetCommandIndexedExpressionLValueTest : VimTestCase("\n") {
  @Test
  fun `test assigning to List item modifies list`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo string(s)", "[1, 2]")
  }

  @Test
  fun `test assigning to List item modifies referenced list`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let t = s")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo string(t)", "[1, 2]")
  }

  // TODO: Negative indexes, inc. out of range test

  // Compound assignment operators will try to convert the operands to Number, calculate the result and write back a
  // Number. The operator will coerce to Float if one side is a Float.
  // We assume the other compound assignment operators all behave the same, with an additional test for concatenation.
  @Test
  fun `test arithmetic compound assignment operator with Number rvalue on List item`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let s[1] += 2")
    assertCommandOutput("echo string(s)", "[1, 3]")
  }

  @Test
  fun `test arithmetic compound assignment operator converts String to Number`() {
    // Note that the List item is converted to a Number
    enterCommand("let s = ['1', '1']")
    enterCommand("let s[1] += 2")
    assertCommandOutput("echo string(s)", "['1', 3]")
  }

  @Test
  fun `test arithmetic compound assignment operator coerces operation to Float`() {
    // Note that the List item is converted to a Number
    enterCommand("let s = ['1', 1.5]")
    enterCommand("let s[1] += 2")
    assertCommandOutput("echo string(s)", "['1', 3.5]")
  }

  @Test
  fun `test string concatenation compound assignment operator converts Numbers to String`() {
    // The operands are converted from Number to String and concatenated
    enterCommand("let s = [1, 1]")
    enterCommand("let s[1] .= 2")
    assertCommandOutput("echo string(s)", "[1, '12']")
  }

  @Test
  fun `test string concatenation compound assignment operator cannot convert Float to String`() {
    // The operator tries to convert Float to String but cannot
    enterCommand("let s = [1, 1.5]")
    enterCommand("let s[1] .= '2'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for .=")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assigning to out of range List item raises error`() {
    enterCommand("let s = [1, 1]")
    enterCommand("let s[2] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 2")
  }

  @Test
  fun `test assigning to locked List variable with default lock depth`() {
    enterCommand("let s = [1, 1]")
    enterCommand("lockvar s")
    enterCommand("let s[1] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s[1] = 2")
  }

  @Test
  fun `test assigning to locked List variable with lock depth 0 allows modification`() {
    // Lock depth 0 means we can't change the variable, not its contents'
    enterCommand("let s = [1, 1]")
    enterCommand("lockvar 0 s")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo string(s)", "[1, 2]")
  }

  @Test
  fun `test assigning to locked List variable with lock depth 1 allows modification`() {
    // Allows modification, but not add/remove item
    enterCommand("let s = [1, 1]")
    enterCommand("lockvar 1 s")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo string(s)", "[1, 2]")
  }

  @Test
  fun `test assigning to locked List variable with lock depth 2 does not allow modification`() {
    enterCommand("let s = [1, 1]")
    enterCommand("lockvar 2 s")
    enterCommand("let s[1] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s[1] = 2")
  }

  @Test
  fun `test assign to new value in Dictionary adds pair`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key2'] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 1, 'key2': 2}")
  }

  @Test
  fun `test assign to existing value in Dictionary updates existing pair`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key1'] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 2}")
  }

  @Test
  fun `test assign to Dictionary with negative index treats Number as String`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s[-1] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 1, '-1': 2}")
  }

  // Compound assignment will convert a String to a Number to calculate the result and write back a Number. It will
  // coerce the operation to Float if one side is a Float.
  // We assume the other compound assignment operators all behave the same, with an additional test for concatenation.
  @Test
  fun `test arithmetic compound assignment operator on Dictionary item`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key1'] += 2")
    assertCommandOutput("echo string(s)", "{'key1': 3}")
  }

  @Test
  fun `test arithmetic compound assignment operator on Dictionary item converts String to Number`() {
    enterCommand("let s = {'key1' : '1'}")
    enterCommand("let s['key1'] += 2")
    assertCommandOutput("echo string(s)", "{'key1': 3}")
  }

  @Test
  fun `test arithmetic compound assignment operator on Dictionary item converts float String to Number`() {
    enterCommand("let s = {'key1' : '1.5'}")
    enterCommand("let s['key1'] += 2")
    assertCommandOutput("echo string(s)", "{'key1': 3}")
  }

  @Test
  fun `test arithmetic compound assignment operator on Dictionary item converts invalid String to Number as zero`() {
    enterCommand("let s = {'key1' : 'foo'}")
    enterCommand("let s['key1'] += 2")
    assertCommandOutput("echo string(s)", "{'key1': 2}")
  }

  @Test
  fun `test string concatenation compound assignment operator on Dictionary item converts Number to String`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s['key1'] .= 2")
    assertCommandOutput("echo string(s)", "{'key1': '12'}")
  }

  @Test
  fun `test string concatenation compound assignment operator on Dictionary item cannot convert Float to String`() {
    // The operator tries to convert Float to String, but cannot
    enterCommand("let s = {'key1': 1, 'key2': 1.5}")
    enterCommand("let s['key2'] .= '2'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for .=")
  }

  @VimBehaviorDiffers("{'1': 2, 'key1': 1}", description = "Order of items is undefined")
  @Test
  fun `test assigning to Dictionary item converts Number index to String key`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s[1] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 1, '1': 2}")
  }

  @VimBehaviorDiffers(
    "{'1.5': 2, 'key1': 1}",
    description = "Vim converts Float to String, which isn't a normal conversion"
  )
  @Test
  fun `test assigning to Dictionary item converts Float index to String key`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s[1.5] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E806: Using a Float as a String")
//    assertCommandOutput("echo string(s)", "{'key1': 1, '1.5': 2}")
  }

  @Test
  fun `test assigning to Dictionary item with List key raises error`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("let s[[1,2,3]] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test assigning to indexed String item is not valid lvalue`() {
    // Strings can be indexed, but not as an lvalue
    enterCommand("let s = 'abc'")
    enterCommand("let s[1] = 'd'")
    assertPluginError(true)
    assertPluginErrorMessage("E689: Index not allowed after a string: s[1] = 'd'")
  }

  @Test
  fun `test assigning to locked Dictionary variable with default lock depth`() {
    enterCommand("let s = {'key1' : 1}")
    enterCommand("lockvar s")
    enterCommand("let s['key1'] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s['key1'] = 2")
  }

  @Test
  fun `test assigning to locked Dictionary variable with lock depth 0 allows modification`() {
    // Lock depth 0 means we can't change the variable, not its contents
    enterCommand("let s = {'key1' : 1}")
    enterCommand("lockvar 0 s")
    enterCommand("let s['key1'] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 2}")
  }

  @Test
  fun `test assigning to locked Dictionary variable with lock depth 1 allows modification`() {
    // Lock depth 0 means we can't change the variable or add/remove items
    enterCommand("let s = {'key1' : 1}")
    enterCommand("lockvar 1 s")
    enterCommand("let s['key1'] = 2")
    assertCommandOutput("echo string(s)", "{'key1': 2}")
  }

  @Test
  fun `test assigning to locked Dictionary variable with lock depth 1 does not allow addition`() {
    // Lock depth 0 means we can't change the variable or add/remove items
    enterCommand("let s = {'key1' : 1}")
    enterCommand("lockvar 1 s")
    enterCommand("let s['key2'] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s['key2'] = 2")
  }

  @Test
  fun `test assigning to locked Dictionary variable with lock depth 2 does not allow addition`() {
    // Lock depth 0 means we can't change the variable or add/remove items
    enterCommand("let s = {'key1' : 1}")
    enterCommand("lockvar 2 s")
    enterCommand("let s['key1'] = 2")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s['key1'] = 2")
  }
}
