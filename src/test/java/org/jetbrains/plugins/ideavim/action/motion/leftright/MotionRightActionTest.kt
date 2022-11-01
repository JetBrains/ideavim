/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import org.jetbrains.plugins.ideavim.OptionValueType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption

class MotionRightActionTest : VimOptionTestCase(OptionConstants.virtualeditName) {
  @VimOptionDefaultAll
  fun `test simple motion`() {
    doTest(
      "l",
      """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found i${c}t in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimOptionDefaultAll
  fun `test simple motion with repeat`() {
    doTest(
      "3l",
      """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it ${c}in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimOptionDefaultAll
  fun `test simple motion to the end`() {
    doTest(
      "3l",
      """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
  fun `test virtual edit motion to the end`() {
    doTest(
      "3l",
      """
            Yesterday it worke${c}d
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it worked${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(OptionConstants.virtualeditName, OptionValueType.STRING, OptionConstants.virtualedit_onemore))
  fun `test virtual edit motion after dollar`() {
    doTest(
      "\$l",
      """
            Yesterday it ${c}worked
            Today it is not working
            The test is like that.
      """.trimIndent(),
      """
            Yesterday it worked${c}
            Today it is not working
            The test is like that.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion non-ascii`() {
    doTest(
      "l",
      """
            A Discovery

            I found it in a legendar${c}𝛁 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendar𝛁${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion emoji`() {
    doTest(
      "l",
      """
            A Discovery

            I found it in a legendar${c}🐔 land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendar🐔${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @VimOptionDefaultAll
  fun `test simple motion czech`() {
    doTest(
      "l",
      """
            A Discovery

            I found it in a legendar${c}ž land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendarž${c} land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimOptionDefaultAll
  fun `test simple motion tab`() {
    doTest(
      "l",
      """
        A Discovery

        I found it in a legendar${c}. land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass
      """.trimIndent().dotToTab(),
      """
        A Discovery

        I found it in a legendar.${c} land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass
      """.trimIndent().dotToTab(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimOptionDefaultAll
  fun `test char visual mode`() {
    doTest(
      listOf("v", "ll"),
      """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary lan${s}d${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER
    )
  }

  @VimOptionDefaultAll
  fun `test block visual mode`() {
    doTest(
      listOf("<C-V>", "ll"),
      """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it in a legendary lan${s}d${c}${se}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_BLOCK
    )
  }
}
