/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class InsertRegisterTest : VimTestCase() {
  //  todo test cursor position VIM-2732
  @Test
  @Disabled
  fun `test multiline insert from expression register`() {
    val keys = "VjyGo<C-r>=@\"<CR>"
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            $c
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @Test
  fun `test insert named register with CTRL-R`() {
    configureByText("\n")
    enterCommand("let @a=\"hello world\"")
    typeText("i<C-R>a")
    assertState("hello world$c\n")
  }

  @Test
  fun `test insert named register literally with CTRL-R CTRL-R`() {
    configureByText("\n")
    enterCommand("let @a=\"hello world\"")
    typeText("i<C-R><C-R>a")
    assertState("hello world$c\n")
  }

  @Test
  fun `test insert named register literally with CTRL-R CTRL-O`() {
    configureByText("\n")
    enterCommand("let @a=\"hello world\"")
    typeText("i<C-R><C-O>a")
    assertState("hello world$c\n")
  }
}
