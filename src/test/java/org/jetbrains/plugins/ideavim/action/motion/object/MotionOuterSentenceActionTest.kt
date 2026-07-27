/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterSentenceActionTest : VimTestCase() {
  @Test
  fun `test on empty last line`() {
    doTest(
      "=as",
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
      """.trimIndent(),
      """
        ${c}Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
    """,
    description = "Vim has no empty last line, so `as` there selects nothing and the buffer is left alone. " +
      "IdeaVim selects the whole file instead, and the delete is promoted to linewise",
  )
  @Test
  fun `test delete on empty last line`() {
    doTest(
      "das",
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
      """.trimIndent(),
      "",
      Mode.NORMAL(),
    )
  }
}
