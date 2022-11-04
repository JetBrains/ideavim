/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class SelectEnableLineModeActionHandlerTest : VimTestCase() {
  fun `test entering select mode`() {
    doTest(
      "gH",
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                $s${c}I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.SELECT,
      VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test entering select mode at the end of file`() {
    doTest(
      "gH",
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${s}hard by the torrent of a mountain pass$c.$se
      """.trimIndent(),
      VimStateMachine.Mode.SELECT,
      VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  fun `test entering select mode on empty line`() {
    doTest(
      "gH",
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $s$c$se
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.SELECT,
      VimStateMachine.SubMode.VISUAL_LINE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  fun `test entering select mode multicaret`() {
    doTest(
      listOf("gH"),
      """
                A Discovery
                $c
                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on ${c}some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $s$c$se
                $s${c}I found it in a legendary land$se
                all rocks and lavender and tufted grass,
                ${s}where it was ${c}settled on some sodden sand$se
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.SELECT,
      VimStateMachine.SubMode.VISUAL_LINE
    )
  }
}
