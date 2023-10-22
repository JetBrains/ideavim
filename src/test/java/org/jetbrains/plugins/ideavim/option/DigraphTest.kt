/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
// TODO: 2019-06-18 VimOptionsTestCase
class DigraphTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  @Test
  fun `test digraph`() {
    doTest(
      "i B<BS>B",
      """
            Lorem Ipsum

            I found it$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it ¦$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  @Test
  fun `test digraph stops`() {
    doTest(
      "i B<BS>BHello",
      """
            Lorem Ipsum

            I found it$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it ¦Hello$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  @Test
  fun `test digraph double backspace`() {
    doTest(
      "i B<BS><BS>B",
      """
            Lorem Ipsum

            I found it$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found itB$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set digraph")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "backspace works strange")
  @Test
  fun `test digraph backspace digraph`() {
    doTest(
      "i B<BS>B<BS>B",
      """
            Lorem Ipsum

            I found it$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it B$c in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.INSERT,
    ) {
      enterCommand("set digraph")
    }
  }
}
