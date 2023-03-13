/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertDeleteInsertedTextActionTest : VimTestCase() {
  // VIM-1655
  fun `test deleted text is not yanked`() {
    doTest(
      listOf("yiw", "ea", "Hello", "<C-U>", "<ESC>p"),
      """
            A Discovery

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            A Discovery

            I found iti${c}t in a legendary land
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  // VIM-1655
  @VimBehaviorDiffers(description = "Inserted text is not deleted after <C-U>")
  fun `test deleted text is not yanked after replace`() {
    doTest(
      listOf("yiw", "eR", "Hello", "<C-U>", "<ESC>p"),
      """
            A Discovery

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            A Discovery

            I found ii${c}ta legendary land
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
