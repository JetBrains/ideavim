/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionLeftInsertTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("i", "<Left>"),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set whichwrap=[")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file start`() {
    doTest(
      listOf("i", "<Left>"),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
      """
          ${c}Oh, hi Mark
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set whichwrap=[")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to previous line`() {
    doTest(
      listOf("i", "<Left>"),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark$c
          You are my favourite customer
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set whichwrap=[")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("i", "<Left>"),
      """
          Oh, hi Mark
          
          $c
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark
          $c
          
          You are my favourite customer
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set whichwrap=[")
    }
  }
}
