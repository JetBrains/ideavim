/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionGotoLineFirstActionTest : VimTestCase() {
  fun `test simple motion`() {
    doTest(
      "gg",
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion with count`() {
    doTest(
      "5gg",
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion with large count`() {
    doTest(
      "100gg",
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test motion with zero count`() {
    doTest(
      "0gg",
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                ${c}A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test moves caret to first non-blank char`() {
    doTest(
      "gg",
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |       ${c}A Discovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test moves caret to same column with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    doTest(
      "gg",
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |       A Di${c}scovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }
}
