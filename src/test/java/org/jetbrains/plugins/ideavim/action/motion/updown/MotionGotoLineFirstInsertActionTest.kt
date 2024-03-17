/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionGotoLineFirstInsertActionTest : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test simple motion`() {
    doTest(
      listOf("i", "<C-Home>", "<Esc>"),
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |$c       A Discovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test simple motion ignores nostartofline`() {
    doTest(
      listOf("i", "<C-Home>", "<Esc>"),
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |$c       A Discovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
    ) {
      enterCommand("set nostartofline")
    }
  }
}
