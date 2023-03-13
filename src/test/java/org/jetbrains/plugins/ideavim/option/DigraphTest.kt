/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
// TODO: 2019-06-18 VimOptionsTestCase
class DigraphTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph`() {
    doTest(
      "i B<BS>B",
      """
            A Discovery

            I found it$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it ¦$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph stops`() {
    doTest(
      "i B<BS>BHello",
      """
            A Discovery

            I found it$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it ¦Hello$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph double backspace`() {
    doTest(
      "i B<BS><BS>B",
      """
            A Discovery

            I found it$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found itB$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  fun `test digraph backspace digraph`() {
    doTest(
      "i B<BS>B<BS>B",
      """
            A Discovery

            I found it$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
            A Discovery

            I found it B$c in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE,
    ) {
      enterCommand("set digraph")
    }
  }
}
