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

@Suppress("SpellCheckingInspection")
class LetCommandRegisterLValueTest : VimTestCase("\n") {
  @Test
  fun `test assign to numbered register`() {
    enterCommand("let @4 = 'inumber register works'")
    assertCommandOutput("echo string(@4)", "'inumber register works'")

    typeText("@4")
    assertState("number register works\n")
  }

  @Test
  fun `test assign to lowercase letter register`() {
    enterCommand("let @o = 'ilowercase letter register works'")
    assertCommandOutput("echo string(@o)", "'ilowercase letter register works'")

    typeText("@o")
    assertState("lowercase letter register works\n")
  }

  @Test
  fun `test assign to uppercase letter register appends register`() {
    enterCommand("let @O = 'iuppercase letter register works'")
    assertCommandOutput("echo string(@O)", "'iuppercase letter register works'")

    typeText("@O")
    assertState("uppercase letter register works\n")
    typeText("<Esc>")

    enterCommand("let @O = '!'")
    assertCommandOutput("echo string(@O)", "'iuppercase letter register works!'")
  }

  @Test
  fun `test assign to unnamed register`() {
    enterCommand("let @\" = 'iunnamed register works'")
    assertCommandOutput("echo string(@\")", "'iunnamed register works'")

    typeText("@\"")
    assertState("unnamed register works\n")
  }

  @Test
  fun `test assign Number to register is converted to String`() {
    enterCommand("let @a = 123")
    assertCommandOutput("echo string(@a)", "'123'")
  }

  @VimBehaviorDiffers("Vim converts Float to String")
  @Test
  fun `test assign Float to register is converted to String`() {
    enterCommand("let @a = 12.34")
    assertPluginError(true)
    assertPluginErrorMessage("E806: Using a Float as a String")
//    assertCommandOutput("echo string(@a)", "'12.34'")
  }

  @Test
  fun `test assign expression to register converts to String`() {
    enterCommand("let @a = 100 + 23")
    assertCommandOutput("echo string(@a)", "'123'")
  }

  @Test
  fun `test assign List to register reports an error`() {
    enterCommand("let @a = [1, 2, 3]")
    assertPluginError(true)
    assertPluginErrorMessage("E730: Using a List as a String")
  }

  @Test
  fun `test assign Dictionary to register reports an error`() {
    enterCommand("let @a = {'key': 'value'}")
    assertPluginError(true)
    assertPluginErrorMessage("E731: Using a Dictionary as a String")
  }

  // The result of the compound operator is a Number, which cannot be assigned to a String register. Even though the
  // simple assignment operator can convert Number to String, Vim doesn't appear to do that for compound assignments.
  // We test `+=` (in case it works like concatenation - it doesn't) and `-=`. The other compound operators are assumed
  // to behave the same way.
  @Test
  fun `test arithmetic compound assignment operator to register requires Number lvalue`() {
    enterCommand("let @a='hello'")
    enterCommand("let @a+=' world'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for +=")
  }

  @Test
  fun `test arithmetic compound assignment operator to register requires Number lvalue 2`() {
    enterCommand("let @a='hello'")
    enterCommand("let @a-=' world'")
    assertPluginError(true)
    assertPluginErrorMessage("E734: Wrong variable type for -=")
  }

  @Test
  fun `test string concatenation compound assignment operator to register`() {
    enterCommand("let @a='hello'")
    enterCommand("let @a.=' world'")
    assertCommandOutput("echo string(@a)", "'hello world'")
  }

  @Test
  fun `test string concatenation compound assignment operator converts Number rvalue to String`() {
    enterCommand("let @a='hello'")
    enterCommand("let @a.=12")
    assertCommandOutput("echo string(@a)", "'hello12'")
  }

  @VimBehaviorDiffers("'hello1.23'")
  @Test
  fun `test string concatenation compound assignment operator converts Float rvalue to String`() {
    enterCommand("let @a='hello'")
    enterCommand("let @a.=1.23'")
    assertPluginError(true) // TODO: Wrong!
    assertCommandOutput("echo string(@a)", "'hello'")
  }
}
