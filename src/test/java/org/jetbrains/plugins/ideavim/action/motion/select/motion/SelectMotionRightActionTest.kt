/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SelectMotionRightActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test char select simple move`() {
    doTest(
      listOf("viw", "<C-G>", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I found${c} it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set keymodel=stopsel")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test select multiple carets`() {
    doTest(
      listOf("viw", "<C-G>", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                where it was settled on some sodden san${c}d
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I found${c} it in a legendary land
                consectetur adipiscing elit
                where it was settled on some sodden san${c}d
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set keymodel=stopsel")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test without stopsel`() {
    doTest(
      listOf("viw", "<C-G>", "<Right>"),
      """
                Lorem Ipsum

                I ${c}found it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                I ${s}found ${c}${se}it in a legendary land
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set keymodel=")
    }
  }
}
