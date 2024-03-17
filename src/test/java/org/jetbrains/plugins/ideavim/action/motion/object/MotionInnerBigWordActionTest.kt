/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionInnerBigWordActionTest : VimTestCase() {
  @Test
  fun `test at last dot`() {
    doTest(
      "diW",
      """
          Lorem ipsum dolor sit amet,
          consectetur adipiscing elit
          Sed in orci mauris.
          hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
          Lorem ipsum dolor sit amet,
          consectetur adipiscing elit
          Sed in orci mauris.
          hard by the torrent of a mountain$c 
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
