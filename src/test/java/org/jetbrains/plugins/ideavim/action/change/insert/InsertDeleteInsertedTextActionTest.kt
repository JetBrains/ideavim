/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertDeleteInsertedTextActionTest : VimTestCase() {
  // VIM-1655
  @Test
  fun `test deleted text is not yanked`() {
    doTest(
      listOf("yiw", "ea", "Hello", "<C-U>", "<ESC>p"),
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            Lorem Ipsum

            I found iti${c}t in a legendary land
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-1655
  @VimBehaviorDiffers(description = "Inserted text is not deleted after <C-U>")
  @Test
  fun `test deleted text is not yanked after replace`() {
    doTest(
      listOf("yiw", "eR", "Hello", "<C-U>", "<ESC>p"),
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            Lorem Ipsum

            I found ii${c}ta legendary land
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test undo after delete inserted text in insert mode`() {
    configureByText("Hello ${c}world")
    typeText("i")
    typeText("beautiful ")
    assertState("Hello beautiful ${c}world")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("Hello ${c}world")
    assertMode(Mode.INSERT)
    typeText("<Esc>")
    typeText("u")
    assertState("Hello ${c}world")
  }

  @Test
  fun `test undo after delete inserted text in insert mode with oldundo`() {
    configureByText("Hello ${c}world")
    try {
      enterCommand("set oldundo")
      typeText("i")
      typeText("beautiful ")
      assertState("Hello beautiful ${c}world")
      assertMode(Mode.INSERT)
      typeText("<C-U>")
      assertState("Hello ${c}world")
      assertMode(Mode.INSERT)
      typeText("<Esc>")
      typeText("u")
      assertState("Hello ${c}world")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo preserves text before insertion point`() {
    configureByText("Start${c}End")
    typeText("i")
    typeText(" Middle ")
    assertState("Start Middle ${c}End")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("Start${c}End")
    assertMode(Mode.INSERT)
    typeText("<Esc>")
    assertState("Star${c}tEnd")
    typeText("u")
    assertState("Start${c}End")
  }

  @Test
  fun `test undo preserves text before insertion point with oldundo`() {
    configureByText("Start${c}End")
    try {
      enterCommand("set oldundo")
      typeText("i")
      typeText(" Middle ")
      assertState("Start Middle ${c}End")
      assertMode(Mode.INSERT)
      typeText("<C-U>")
      assertState("Start${c}End")
      assertMode(Mode.INSERT)
      typeText("<Esc>")
      assertState("Star${c}tEnd")
      typeText("u")
      assertState("Start${c}End")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test C-U at start of insert on empty line`() {
    configureByText("${c}")
    typeText("i")
    assertState("${c}")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("${c}")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test C-U at start of insert on line with only whitespace`() {
    configureByText("    ${c}")
    typeText("i")
    assertState("    ${c}")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("    ${c}")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test C-U with text inserted after existing content`() {
    configureByText("    text${c}")
    typeText("A")
    typeText(" more")
    assertState("    text more${c}")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("    text${c}")
    assertMode(Mode.INSERT)
  }

  @Test
  fun `test C-U in middle of line after inserting text`() {
    configureByText("prefix${c}suffix")
    typeText("i")
    typeText("inserted")
    assertState("prefixinserted${c}suffix")
    assertMode(Mode.INSERT)
    typeText("<C-U>")
    assertState("prefix${c}suffix")
    assertMode(Mode.INSERT)
  }
}
