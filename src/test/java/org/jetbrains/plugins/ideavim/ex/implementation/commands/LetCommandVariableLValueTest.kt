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
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests the `:let` command assigning to a variable lvalue expression
 *
 * This tests assigning and reassigning different datatypes to variables. It ensures compound assignment works (i.e.,
 * calculates a new value and assigns), but does not test all compound assignment operators. They are tested
 * exhaustively in [LetCommandOperatorsTest].
 *
 * Also tests variable scope.
 */
class LetCommandVariableLValueTest : VimTestCase("\n") {
  @Test
  fun `test assign String to variable`() {
    enterCommand("let s = \"foo\"")
    assertCommandOutput("echo string(s)", "'foo'")
  }

  @Test
  fun `test assign Number to variable`() {
    enterCommand("let s = 100")
    assertCommandOutput("echo string(s)", "100")
  }

  @Test
  fun `test assign Float to variable`() {
    enterCommand("let s = 1.23")
    assertCommandOutput("echo string(s)", "1.23")
  }

  @Test
  fun `test assign List to variable`() {
    enterCommand("let s = [1, 2, 3]")
    assertCommandOutput("echo string(s)", "[1, 2, 3]")
  }

  @Test
  fun `test assign Dictionary to variable`() {
    enterCommand("let s = {'key1' : 1, 'key2' : 2}")
    assertCommandOutput("echo string(s)", "{'key1': 1, 'key2': 2}")
  }

  // TODO: Assign Funcref

  @Test
  fun `test assign expression to variable`() {
    enterCommand("let s = 10 + 20 * 4")
    assertCommandOutput("echo string(s)", "90")
  }

  @Test
  fun `test assign variable to variable`() {
    enterCommand("let s = 10")
    enterCommand("let t = s")
    assertCommandOutput("echo string(t)", "10")
  }

  @Test
  fun `test reassign String variable`() {
    enterCommand("let s = \"foo\"")
    enterCommand("let s = \"bar\"")
    assertCommandOutput("echo string(s)", "'bar'")
  }

  @Test
  fun `test reassign Number variable`() {
    enterCommand("let s = 100")
    enterCommand("let s = 200")
    assertCommandOutput("echo string(s)", "200")
  }

  @Test
  fun `test reassign Float variable`() {
    enterCommand("let s = 1.23")
    enterCommand("let s = 2.23")
    assertCommandOutput("echo string(s)", "2.23")
  }

  @Test
  fun `test reassign List variable`() {
    enterCommand("let s = [1, 2, 3]")
    enterCommand("let s = [4, 5, 6]")
    assertCommandOutput("echo string(s)", "[4, 5, 6]")
  }

  @Test
  fun `test reassign Dictionary variable`() {
    enterCommand("let s = {'key1' : 1, 'key2' : 2}")
    enterCommand("let s = {'key3' : 3, 'key4' : 4}")
    assertCommandOutput("echo string(s)", "{'key3': 3, 'key4': 4}")
  }

  @Test
  fun `test compound assignment to variable`() {
    enterCommand("let s = 10")
    enterCommand("let s += 5")
    assertCommandOutput("echo string(s)", "15")
  }

  @Test
  fun `test assign to globally scoped variable from command line context`() {
    enterCommand("let g:my_var = 'oh, hi Mark'")
    assertCommandOutput("echo string(g:my_var)", "'oh, hi Mark'")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assign to script scoped variable from command line context reports error`() {
    enterCommand("let s:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessage("E461: Illegal variable name: s:my_var")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assign to function-local scoped variable from command line context reports error`() {
    enterCommand("let l:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessage("E461: Illegal variable name: l:my_var")
  }

  @Test
  fun `test assign to function-local scoped variable from function context`() {
    val function = """|
      |function! MyFunc()
      |  let l:my_var = 'oh, hi Mark'
      |  echo string(l:my_var)
      |endfunction
    """.trimMargin()
    executeVimscript(function)
    assertCommandOutput("call MyFunc()", "'oh, hi Mark'")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test assign to function argument variable from command line context reports error`() {
    enterCommand("let a:my_var = 'oh, hi Mark'")
    assertPluginError(true)
    assertPluginErrorMessage("E461: Illegal variable name: a:my_var")
  }

  @Test
  fun `test assign to function argument variable from function context reports error`() {
    val function = """|
      |function! MyFunc(my_var)
      |  let a:my_var = 'oh, hi Mark'
      |  echo string(a:my_var)
      |endfunction
    """.trimMargin()
    executeVimscript(function)
    enterCommand("call MyFunc('foo')")
    assertPluginError(true)
    assertPluginErrorMessage("E46: Cannot change read-only variable \"a:my_var\"")
  }

  @Test
  fun `test assign to locked variable reports error`() {
    enterCommand("let s=12")
    enterCommand("lockvar s")
    enterCommand("let s=13")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s")
  }

  // We assume that compound assignment operators all behave the same with respect to locked variables, so we only test one
  @Test
  fun `test compound assignment to locked variable reports error`() {
    enterCommand("let s=12")
    enterCommand("lockvar s")
    enterCommand("let s+=1")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: s")
  }
}
