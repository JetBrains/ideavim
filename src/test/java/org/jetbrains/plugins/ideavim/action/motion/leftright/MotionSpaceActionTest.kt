/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionSpaceActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("<Space>"),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=s")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file end`() {
    doTest(
      listOf("<Space>"),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=s")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to next line`() {
    doTest(
      listOf("<Space>"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=s")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("<Space>"),
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
      enterCommand("set whichwrap=s")
    }
  }

  @Suppress("SpellCheckingInspection")
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test space motion with operator`() {
    doTest(
      "d<Space>",
      """
        lorem ${c}ipsum dolor sit amet
      """.trimIndent(),
      """
        lorem ${c}psum dolor sit amet
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test space motion with operator at end of line`() {
    doTest(
      "d<Space>",
      """
        lorem ipsum dolor sit ame${c}t
        lorem ipsum dolor sit amet
      """.trimIndent(),
      """
        lorem ipsum dolor sit am${c}e
        lorem ipsum dolor sit amet
      """.trimIndent(),
    )
  }
}
