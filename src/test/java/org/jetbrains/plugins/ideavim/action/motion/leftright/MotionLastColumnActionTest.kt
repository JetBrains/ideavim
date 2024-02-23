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
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionLastColumnActionTest : VimTestCase() {
  @Test
  fun `test dollar motion`() {
    val keys = "$"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test dollar motion with motion to longer line`() {
    val keys = "\$j"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,[ additional symbols]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,[ additional symbols${c}]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test dollar motion in visual block mode`() {
    val keys = "<C-V>jj\$"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,[ additional symbols]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}found it in a legendary lan${c}d${se}
            al${s}l rocks and lavender and tufted grass,[ additional symbols${c}]${se}
            wh${s}ere it was settled on some sodden sand${c}${se}
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
  }

  @Test
  fun `test dollar motion resets intended location after motion`() {
    doTest(
      "\$hlj",
      """
          A Discovery

          I ${c}found it in a legendary land
          all rocks and lavender and tufted grass,[ additional symbols]
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
          A Discovery

          I found it in a legendary land
          all rocks and lavender and tu${c}fted grass,[ additional symbols]
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I ${s}found it in a legendary lan${c}d${se}
            al${s}l rocks and lavender and tufted grass${c},${se}[ additional symbols]
            wh${s}ere it was settled on some sodden san${c}d${se}
            hard by the torrent of a mountain pass.
    """,
  )
  @Test
  fun `test dollar motion in visual block mode with left motion`() {
    val keys = "<C-V>jj\$h"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,[ additional symbols]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I ${s}found it in a legendary lan${c}d${se}
            al${s}l rocks and lavender and tufted grass,[ additional symbols${c}]${se}
            wh${s}ere it was settled on some sodden san${c}d${se}
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.VISUAL(SelectionType.BLOCK_WISE))
  }

  @Test
  fun `test dollar motion from insert mode`() {
    val keys = "i<C-O>$"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land${c}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test dollar motion from insert mode with deletion`() {
    val keys = "i<C-O>d$"
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I${c} 
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }
}
