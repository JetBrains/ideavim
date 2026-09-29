/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Undo granularity around `.`, in-process.
 *
 * [com.maddyhome.idea.vim.split.RepeatUndoSplitTest] covers this on the JBC backend, where
 * speculative undo turns off the platform's command grouping; there was no in-process equivalent.
 *
 * One change is one undo step, so one `u` after a `.` reverts only the repeat and leaves the
 * original change alone. Verified against nvim (with explicit `let &g:undolevels = &g:undolevels`
 * breaks, because `:normal` otherwise merges the whole sequence into one undo block).
 */
class RepeatUndoTest : VimTestCase() {

  @Test
  fun `test one undo after a dot repeat reverts only the repeat`() {
    doTest(
      "cwfoo<Esc>" + "j0" + "." + "u",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        foo ipsum dolor
        ${c}sit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test a second undo after a dot repeat reverts the original change`() {
    doTest(
      "cwfoo<Esc>" + "j0" + "." + "u" + "u",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test one undo after a dot repeat of a substitute reverts only the repeat`() {
    doTest(
      "sX<Esc>" + "l" + "." + "u",
      "${c}Lorem ipsum dolor",
      "X${c}orem ipsum dolor",
    )
  }
}
