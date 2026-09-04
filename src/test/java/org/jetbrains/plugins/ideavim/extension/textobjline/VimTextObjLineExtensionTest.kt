/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjline

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the port of [vim-textobj-line](https://github.com/kana/vim-textobj-line).
 *
 * `al` selects every character of the current line, excluding the end of line character.
 * `il` selects the same, but without the leading and trailing whitespace.
 * Both fail on an empty line, and `il` also fails on a line that contains whitespace only.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimTextObjLineExtensionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("textobj-line")
  }

  // |d| |al|
  @Test
  fun `test delete a line`() {
    doTest(
      "dal",
      "one\ntw${c}o\nthree",
      "one\n$c\nthree",
    )
  }

  // |d| |al|
  @Test
  fun `test delete a line keeps surrounding whitespace out of the way`() {
    doTest(
      "dal",
      "one\n  tw${c}o three  \nfour",
      "one\n$c\nfour",
    )
  }

  // |d| |al|
  @Test
  fun `test delete a line with caret in leading whitespace`() {
    doTest(
      "dal",
      "one\n$c  two three  \nfour",
      "one\n$c\nfour",
    )
  }

  // |d| |al|
  @Test
  fun `test delete a line on the last line without trailing newline`() {
    doTest(
      "dal",
      "one\n  la${c}st  ",
      "one\n$c",
    )
  }

  // |d| |al|
  @Test
  fun `test delete a line on a whitespace only line`() {
    doTest(
      "dal",
      "one\n $c  \nfour",
      "one\n$c\nfour",
    )
  }

  // |d| |al|
  @Test
  fun `test delete a line does nothing on an empty line`() {
    doTest(
      "dal",
      "one\n$c\nfour",
      "one\n$c\nfour",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line`() {
    doTest(
      "dil",
      "one\n  tw${c}o three  \nfour",
      "one\n  $c  \nfour",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line with caret in leading whitespace`() {
    doTest(
      "dil",
      "one\n$c  two three  \nfour",
      "one\n  $c  \nfour",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line with tabs`() {
    doTest(
      "dil",
      "one\n\tfo${c}o\t\nfour",
      "one\n\t$c\t\nfour",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line without surrounding whitespace`() {
    doTest(
      "dil",
      "one\ntw${c}o\nthree",
      "one\n$c\nthree",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line does nothing on a whitespace only line`() {
    doTest(
      "dil",
      "one\n $c  \nfour",
      "one\n $c  \nfour",
    )
  }

  // |d| |il|
  @Test
  fun `test delete inner line does nothing on an empty line`() {
    doTest(
      "dil",
      "one\n$c\nfour",
      "one\n$c\nfour",
    )
  }

  // |c| |al|
  @Test
  fun `test change a line`() {
    doTest(
      "cal",
      "one\n  tw${c}o three  \nfour",
      "one\n$c\nfour",
      Mode.INSERT,
    )
  }

  // |c| |il|
  @Test
  fun `test change inner line`() {
    doTest(
      "cil",
      "one\n  tw${c}o three  \nfour",
      "one\n  $c  \nfour",
      Mode.INSERT,
    )
  }

  // |y| |al|
  @Test
  fun `test yank a line`() {
    configureByText("one\n  tw${c}o three  \nfour")
    typeText("yal")
    assertRegisterString('"', "  two three  ")
  }

  // |y| |il|
  @Test
  fun `test yank inner line`() {
    configureByText("one\n  tw${c}o three  \nfour")
    typeText("yil")
    assertRegisterString('"', "two three")
  }

  // |v| |al|
  @Test
  fun `test visual select a line`() {
    configureByText("one\n  tw${c}o three  \nfour")
    typeText("val")
    assertSelection("  two three  ")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  // |v| |il|
  @Test
  fun `test visual select inner line`() {
    configureByText("one\n  tw${c}o three  \nfour")
    typeText("vil")
    assertSelection("two three")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  // |gU| |il|
  @Test
  fun `test upper case inner line`() {
    doTest(
      "gUil",
      "one\n  tw${c}o three  \nfour",
      "one\n  ${c}TWO THREE  \nfour",
    )
  }

  @Test
  fun `test plug mappings are available`() {
    configureByText("one\n  tw${c}o three  \nfour")
    enterCommand("omap L <Plug>(textobj-line-al)")
    typeText("dL")
    assertState("one\n$c\nfour")
  }

  @Test
  fun `test default mappings can be disabled`() {
    configureByText("one\n  tw${c}o three  \nfour")
    enterCommand("set notextobj-line")
    enterCommand("let g:textobj_line_no_default_key_mappings = 1")
    enterCommand("set textobj-line")

    enterCommand("omap L <Plug>(textobj-line-al)")
    typeText("dL")
    assertState("one\n$c\nfour")

    // "al" is not mapped anymore
    typeText("<Esc>")
    assertState("one\n$c\nfour")
  }
}
