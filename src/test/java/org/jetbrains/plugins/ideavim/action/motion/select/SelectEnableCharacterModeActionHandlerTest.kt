/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SelectEnableCharacterModeActionHandlerTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test entering select mode`() {
    doTest(
      listOf("gh"),
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                ${s}I$c$se found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test entering select mode at the end of file`() {
    doTest(
      listOf("gh"),
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
                hard by the torrent of a mountain pass$s.$c$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @Test
  fun `test entering select mode on empty line`() {
    doTest(
      "gh",
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
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test entering select mode multicaret`() {
    doTest(
      listOf("gh"),
      """
                A Discovery
                $c
                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${c}settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery
                $s$c$se
                ${s}I$c$se found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was ${s}s$c${se}ettled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }
}
