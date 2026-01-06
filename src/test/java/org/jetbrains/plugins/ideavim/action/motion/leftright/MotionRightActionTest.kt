/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionRightActionTest : VimTestCase() {
  @Test
  fun `test simple motion`() {
    doTest(
      "l",
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found i${c}t in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test simple motion with repeat`() {
    doTest(
      "3l",
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it ${c}in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test simple motion to the end`() {
    doTest(
      "3l",
      """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
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
      Mode.NORMAL(),
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
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
      Mode.NORMAL(),
    ) {
      enterCommand("set virtualedit=onemore")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @Test
  fun `test simple motion non-ascii`() {
    doTest(
      "l",
      """
            Lorem Ipsum

            I found it in a legendar${c}${"\uD835\uDEC1"} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendar${"\uD835\uDEC1"}${c} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @Test
  fun `test simple motion emoji`() {
    doTest(
      "l",
      """
            Lorem Ipsum

            I found it in a legendar${c}${"\uD83D\uDC14"} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendar${"\uD83D\uDC14"}${c} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @Test
  fun `test simple motion multiple code point grapheme cluster`() {
    doTest(
      "l",
      """
            Lorem Ipsum

            I found it in a legendar${c}${"👩‍👩‍👧‍👧"} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendar${"👩‍👩‍👧‍👧"}${c} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.NON_ASCII)
  @Test
  fun `test simple motion czech`() {
    doTest(
      "l",
      """
            Lorem Ipsum

            I found it in a legendar${c}ž land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendarž${c} land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test simple motion tab`() {
    doTest(
      "l",
      """
        Lorem Ipsum

        I found it in a legendar${c}. land
        consectetur adipiscing elit
        Sed in orci mauris.
        hard by the torrent of a mountain pass
      """.trimIndent().dotToTab(),
      """
        Lorem Ipsum

        I found it in a legendar.${c} land
        consectetur adipiscing elit
        Sed in orci mauris.
        hard by the torrent of a mountain pass
      """.trimIndent().dotToTab(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test char visual mode`() {
    doTest(
      listOf("v", "ll"),
      """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendary lan${s}d${c}${se}
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test block visual mode`() {
    doTest(
      listOf("<C-V>", "ll"),
      """
            Lorem Ipsum

            I found it in a legendary lan${c}d
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
            Lorem Ipsum

            I found it in a legendary lan${s}d${c}${se}
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.BLOCK_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("l"),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      """
          Oh, hi Ma${c}rk
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=l")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file end`() {
    doTest(
      listOf("l"),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=l")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to next line`() {
    doTest(
      listOf("l"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark
          ${c}You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=l")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("l"),
      """
          Oh, hi Mark
          ${c}

          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark

          ${c}
          You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=l")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test d command with whichwrap`() {
    doTest(
      listOf("dl"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Ma${c}r
          You are my favourite customer
      """.trimIndent(),
    ) {
      enterCommand("set whichwrap=l")
    }
  }
}
