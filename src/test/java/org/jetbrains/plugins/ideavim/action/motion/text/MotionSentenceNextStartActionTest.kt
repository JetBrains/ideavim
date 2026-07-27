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

class MotionSentenceNextStartActionTest : VimTestCase() {
  @Test
  fun `test with two empty lines`() {
    doTest(
      "C<CR><S-Left><C-Right><C-O>)",
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        Cras id tellus in ${c}ex imperdiet egestas.
      """.trimIndent(),
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        Cras id tellus in 
        
      """.trimIndent(),
      Mode.INSERT,
    )
  }

  @Test
  fun `test sentence motion at end of file with trailing new line`() {
    doTest("))", "${c}One. Two.\n", "One. Two${c}.\n")
  }

  @Test
  fun `test delete sentence at end of file keeps trailing new line`() {
    doTest("d)", "One. ${c}Two.\n", "One.${c} \n")
  }
}
