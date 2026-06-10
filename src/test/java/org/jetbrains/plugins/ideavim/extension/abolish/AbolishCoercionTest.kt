/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.abolish

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class AbolishCoercionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("abolish")
  }

  @Test
  fun `crs converts camelCase under cursor to snake_case`() {
    doTest(
      "crs",
      "let helloW${c}orld = 1",
      "let ${c}hello_world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crm converts snake_case under cursor to PascalCase`() {
    doTest(
      "crm",
      "let hello_${c}world = 1",
      "let ${c}HelloWorld = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crc converts snake_case under cursor to camelCase`() {
    doTest(
      "crc",
      "let hello_${c}world = 1",
      "let ${c}helloWorld = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cru converts snake_case under cursor to UPPER_SNAKE`() {
    doTest(
      "cru",
      "let hello_${c}world = 1",
      "let ${c}HELLO_WORLD = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cr- converts snake_case under cursor to kebab-case`() {
    doTest(
      "cr-",
      "let hello_${c}world = 1",
      "let ${c}hello-world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `cr-dot converts snake_case under cursor to dot-case`() {
    doTest(
      "cr.",
      "let hello_${c}world = 1",
      "let ${c}hello.world = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crt converts snake_case under cursor to Title Case`() {
    doTest(
      "crt",
      "let hello_${c}world = 1",
      "let ${c}Hello World = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crs recases each word independently in multi-caret mode`() {
    doTest(
      "crs",
      "fooBa${c}r and bazQu${c}x",
      "${c}foo_bar and ${c}baz_qux",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crs splits between a digit and an uppercase letter`() {
    doTest(
      "crs",
      "let foo2Ba${c}r = 1",
      "let ${c}foo2_bar = 1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `coerce-snake plug mapping accepts a motion target`() {
    configureByText("let helloW${c}orld = 1")
    enterCommand("nmap gs <Plug>(abolish-coerce-snake)")
    typeText("gsiw")
    assertState("let ${c}hello_world = 1")
  }

  @Test
  fun `coerce-snake plug mapping with end-of-word motion`() {
    configureByText("${c}fooBar baz")
    enterCommand("nmap gs <Plug>(abolish-coerce-snake)")
    typeText("gse")
    assertState("${c}foo_bar baz")
  }

  @Test
  fun `coerce-snake plug mapping in visual mode recases the selected text`() {
    configureByText("let helloW${c}orld = 1")
    enterCommand("xmap gs <Plug>(abolish-coerce-snake)")
    typeText("viwgs")
    assertState("let ${c}hello_world = 1")
  }

  @Test
  fun `coerce-pascal plug mapping in visual mode recases a multi-word selection`() {
    configureByText("${c}hello world end")
    enterCommand("xmap gm <Plug>(abolish-coerce-pascal)")
    typeText("v2egm")
    assertState("${c}HelloWorld end")
  }

  @Test
  fun `c in visual mode is not shadowed by a default coercion mapping`() {
    // Regression: a default Visual-mode crX mapping made c an ambiguous prefix, forcing a
    // timeoutlen wait before the builtin change fired. There must be no default Visual mapping.
    doTest(
      "viwc",
      "let helloW${c}orld = 1",
      "let $c = 1",
      Mode.INSERT,
    )
  }

  @Test
  fun `g abolish_no_mappings keeps plug mappings working without binding default keys`() {
    configureByText("let helloW${c}orld = 1")
    enterCommand("let g:abolish_no_mappings = 1")
    // Toggle the extension off and on so init() re-runs and re-reads the variable above.
    enterCommand("set noabolish")
    enterCommand("set abolish")
    // Defaults are suppressed, but the <Plug> mapping is still available for manual binding.
    enterCommand("nmap gs <Plug>(abolish-coerce-snake)")
    typeText("gsiw")
    assertState("let ${c}hello_world = 1")
  }

  @Test
  fun `crs with a count extends the inner-word range`() {
    doTest(
      "3crs",
      "${c}fooBar bazQux end",
      "${c}foo_bar_baz_qux end",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `crp is an alias for PascalCase coercion`() {
    doTest("crp", "let hello_${c}world = 1", "let ${c}HelloWorld = 1", Mode.NORMAL())
  }

  @Test
  fun `cr_ is an alias for snake_case coercion`() {
    doTest("cr_", "let helloW${c}orld = 1", "let ${c}hello_world = 1", Mode.NORMAL())
  }

  @Test
  fun `crk is an alias for kebab-case coercion`() {
    doTest("crk", "let hello_${c}world = 1", "let ${c}hello-world = 1", Mode.NORMAL())
  }

  @Test
  fun `crU is an alias for UPPER_SNAKE coercion`() {
    doTest("crU", "let hello_${c}world = 1", "let ${c}HELLO_WORLD = 1", Mode.NORMAL())
  }

  @Test
  fun `g abolish_coercions registers a user-defined coercion key`() {
    configureByText("let hello_${c}world = 1")
    enterCommand("let g:abolish_coercions = {'q': 'kebab'}")
    enterCommand("set noabolish")
    enterCommand("set abolish")
    typeText("crq")
    assertState("let ${c}hello-world = 1")
  }
}
