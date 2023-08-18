/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionSentencePreviousStartActionTest : VimTestCase() {
  @Test
  fun `test with two empty lines`() {
    doTest(
      "(",
      """
        
        
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass$c.
      """.trimIndent(),
      """
        
        
        ${c}all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
