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

class SelectEnableLineModeActionHandlerTest : VimTestCase() {
  @Test
  fun `test entering select mode`() {
    doTest(
      "gH",
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                $s${c}Lorem ipsum dolor sit amet,$se
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test entering select mode at the end of file`() {
    doTest(
      "gH",
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                ${s}hard by the torrent of a mountain pass$c.$se
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test entering select mode on empty line`() {
    doTest(
      "gH",
      """
                Lorem Ipsum
                $c
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum
                $s$c$se
                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
  @Test
  fun `test entering select mode multicaret`() {
    doTest(
      listOf("gH"),
      """
                Lorem Ipsum
                $c
                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                where it was ${c}settled on ${c}some sodden sand
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum
                $s$c$se
                $s${c}Lorem ipsum dolor sit amet,$se
                consectetur adipiscing elit
                ${s}where it was ${c}settled on some sodden sand$se
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.SELECT(SelectionType.LINE_WISE),
    )
  }
}
