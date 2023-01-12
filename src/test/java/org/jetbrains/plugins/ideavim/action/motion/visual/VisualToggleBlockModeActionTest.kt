/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class VisualToggleBlockModeActionTest : VimTestCase() {
  fun `test enter visual with count`() {
    doTest(
      "1<C-V>",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    I ${s}${c}f${se}ound it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_BLOCK
    )
  }

  fun `test enter visual with five count`() {
    doTest(
      "5<C-V>",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    I ${s}foun${c}d${se} it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_BLOCK
    )
  }

  fun `test enter visual with 100 count`() {
    doTest(
      "100<C-V>",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    A Discovery

                    I ${s}found it in a legendary land${c}${se}
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_BLOCK
    )
  }

  fun `test on empty file`() {
    doTest(
      "<C-V>", "", "",
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_BLOCK
    )
  }

  fun `test selectmode option`() {
    configureByText(
      """
                    A Discovery

                    I${c} found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand[long line]
                    hard by the torrent of a mountain pass.
      """.trimIndent()
    )
    enterCommand("set selectmode=cmd")
    typeText("<C-V>")
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_BLOCK)
  }
}
