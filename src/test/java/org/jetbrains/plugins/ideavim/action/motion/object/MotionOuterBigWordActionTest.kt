/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterBigWordActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, description = "Wrong caret position, but in real neovim works fine")
  @Test
  fun `test on last dot`() {
    doTest(
      "<aW",
      """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test past end in visual`() {
    doTest(
      "v\$aW",
      """
      I found it in a ${c}legendary land
      }
      """.trimIndent(),
      """
      I found it in a ${s}legendary land
      $c}$se
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
