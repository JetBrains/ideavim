/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterSentenceActionTest : VimTestCase() {
  @VimBehaviorDiffers(
    originalVimAfter = """
        I found it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        $c
    """,
  )
  @Test
  fun `test on empty last line`() {
    doTest(
      "=as",
      """
        I found it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        $c
      """.trimIndent(),
      """
        ${c}I found it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        I found it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        $c
    """,
  )
  @Test
  fun `test delete on empty last line`() {
    doTest(
      "das",
      """
        I found it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        $c
      """.trimIndent(),
      "\n",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
