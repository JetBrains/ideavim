/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertDeleteInsertedTextActionTest : VimTestCase() {
  // VIM-1655
  @Test
  fun `test deleted text is not yanked`() {
    doTest(
      listOf("yiw", "ea", "Hello", "<C-U>", "<ESC>p"),
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            Lorem Ipsum

            I found iti${c}t in a legendary land
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  // VIM-1655
  @VimBehaviorDiffers(description = "Inserted text is not deleted after <C-U>")
  @Test
  fun `test deleted text is not yanked after replace`() {
    doTest(
      listOf("yiw", "eR", "Hello", "<C-U>", "<ESC>p"),
      """
            Lorem Ipsum

            I found ${c}it in a legendary land
      """.trimIndent(),
      """
            Lorem Ipsum

            I found ii${c}ta legendary land
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
