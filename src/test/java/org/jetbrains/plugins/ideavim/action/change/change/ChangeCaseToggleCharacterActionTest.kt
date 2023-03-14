/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeCaseToggleCharacterActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("~"),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      """
          Oh, hi MA${c}rk
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file end`() {
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
      """
          Oh, hi Mar${c}K
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to next line`() {
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi MarK
          ${c}You are my favourite customer
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("~"),
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
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }
}
