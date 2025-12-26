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
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for g$ and g<End> motions
 * According to Vim patch 9.0.1753, g<End> should move to the last non-blank character,
 * not to the end of the display line.
 */
class MotionLastScreenColumnActionTest : VimTestCase() {
  @Test
  fun `test g dollar motion`() {
    val keys = "g\$"
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
  fun `test g End motion with trailing whitespace`() {
    val keys = "g<End>"
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
  fun `test g dollar motion with trailing whitespace`() {
    val keys = "g\$"
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
  fun `test g End motion with trailing tabs`() {
    val keys = "g<End>"
    val before = "I ${c}found it in a legendary land\t\t\t"
    val after = "I found it in a legendary lan${c}d\t\t\t"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g End motion with mixed trailing whitespace`() {
    val keys = "g<End>"
    val before = "I ${c}found it in a legendary land  \t  "
    val after = "I found it in a legendary lan${c}d  \t  "
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g End motion on line with only whitespace`() {
    val keys = "g<End>"
    val before = "${c}    \t  "
    val after = "${c}    \t  "
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g End motion on empty line`() {
    val keys = "g<End>"
    val before = "${c}"
    val after = "${c}"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test g End motion without trailing whitespace`() {
    val keys = "g<End>"
    val before = "I ${c}found it in a legendary land"
    val after = "I found it in a legendary lan${c}d"
    doTest(keys, before, after, Mode.NORMAL())
  }
}
