/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

// Backspace in Replace mode is covered by InsertBackspaceActionTest, which tests the action that handles it.
class ChangeReplaceActionTest : VimTestCase() {
  @Test
  fun `test enter in replace mode does not delete the character under the caret`() {
    // Vim only pushes a separator onto the replace stack for a new line - the character the caret was on is kept and
    // moved to the new line.
    doTest(
      listOf("ll", "R", "<CR>"),
      """
        ${c}grzyb
        next
      """.trimIndent(),
      """
        gr
        ${c}zyb
        next
      """.trimIndent(),
      Mode.REPLACE,
    )
  }

  @Test
  fun `test replace mode past the end of the line appends instead of replacing the next line`() {
    doTest(
      listOf("R", "mushroom"),
      """
        ${c}grzyb
        next
      """.trimIndent(),
      """
        mushroom${c}
        next
      """.trimIndent(),
      Mode.REPLACE,
    )
  }
}
