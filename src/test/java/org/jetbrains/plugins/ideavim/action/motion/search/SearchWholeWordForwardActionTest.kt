/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SearchWholeWordForwardActionTest : VimTestCase() {
  @Test
  fun `test with tabs`() {
    val before = """
  .he${c}llo 1
  .hello 2
  .hello 3
    """.trimIndent().dotToTab()
    val keys = "**"
    val after = """
  .hello 1
  .hello 2
  .${c}hello 3
    """.trimIndent().dotToTab()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test backward search on empty string`() {
    doTest("*", "", "", Mode.NORMAL())
    assertPluginError(false)
  }

  @VimBehaviorDiffers(
    originalVimAfter =
      """
          I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass$c.
        """,
  )
  @Test
  fun `test last dot`() {
    doTest(
      "*",
      """
          I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
          ${c}I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test last word`() {
    doTest(
      "*",
      """
          I found it in a legendary land
          all |pass| rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pas${c}s
      """.trimIndent(),
      """
          I found it in a legendary land
          all |${c}pass| rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
