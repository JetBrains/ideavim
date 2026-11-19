/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjquote

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjQuoteEducateTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("textobj-quote")
  }

  @Test
  fun `test educate turns straight double quotes into curly ones`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("i\"hello\"")
    assertState("“hello”${c}")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test straight double quotes are kept without educate`() {
    configureByText("${c}")
    typeText("i\"hello\"")
    assertState("\"hello\"${c}")
  }

  @Test
  fun `test double quote after closing parenthesis is a closing one`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("i(a)\"")
    assertState("(a)”${c}")
  }

  @Test
  fun `test double quote after straight single quote is an opening one`() {
    configureByText("${c}x")
    enterCommand("Educate")
    typeText("i<C-V>'\"")
    assertState("'“${c}x")
  }

  @Test
  fun `test double quote after curly single quote is an opening one`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("i'\"")
    assertState("‘“${c}")
  }

  @Test
  fun `test single quote after curly double quote is an opening one`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("i\"'")
    assertState("“‘${c}")
  }

  @Test
  fun `test educate turns straight single quotes into curly ones`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("i'hello'")
    assertState("‘hello’${c}")
  }

  @Test
  fun `test educate turns apostrophe into curly one`() {
    configureByText("${c}")
    enterCommand("Educate")
    typeText("idon't")
    assertState("don’t${c}")
  }

  @Test
  fun `test NoEducate keeps straight quotes`() {
    configureByText("${c}x")
    enterCommand("Educate")
    enterCommand("NoEducate")
    typeText("i\"a'")
    assertState("\"a'${c}x")
  }

  @Test
  fun `test ToggleEducate switches educating on and off`() {
    configureByText("${c}")
    enterCommand("ToggleEducate")
    typeText("i\"<Esc>")
    enterCommand("ToggleEducate")
    typeText("a\"")
    assertState("“\"${c}")
  }

  @Test
  fun `test global variable turns educate on`() {
    configureByText("${c}")
    executeVimscript("let g:textobj#quote#educate = 1")
    typeText("i\"a\"")
    assertState("“a”${c}")
  }

  @Test
  fun `test educate with custom quotes`() {
    configureByText("${c}")
    executeVimscript("let g:textobj#quote#doubleDefault = '„“'")
    enterCommand("Educate")
    typeText("i\"a\"")
    assertState("„a“${c}")
  }

  @Test
  fun `test educate with multiple carets`() {
    configureByText("a${c}b\n ${c}c")
    enterCommand("Educate")
    typeText("i\"")
    assertState("a”${c}b\n “${c}c")
  }

  @Test
  fun `test educate with multiple carets at line start`() {
    configureByText("${c}a\n${c}b")
    enterCommand("Educate")
    typeText("i\"")
    assertState("“${c}a\n“${c}b")
  }

  @Test
  fun `test educate in Replace mode`() {
    configureByText("${c}abc")
    enterCommand("Educate")
    typeText("R\"")
    assertState("“${c}bc")
  }

  @Test
  fun `test CTRL-V inserts a straight double quote while educating`() {
    configureByText("${c}x")
    enterCommand("Educate")
    typeText("i<C-V>\"")
    assertState("\"${c}x")
  }

  @Test
  fun `test repeat insert with educated quotes`() {
    configureByText("${c}\nx")
    enterCommand("Educate")
    typeText("i\"a\"<Esc>j0.")
    assertState("“a”\n“a${c}”x")
  }
}
