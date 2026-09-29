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
 * `.` with more than one caret. [RepeatChangeActionTest.test multicaret] was the only such test.
 *
 * NOTE: vim has no multiple carets, so there is no oracle for these (the Neovim comparison in
 * [org.jetbrains.plugins.ideavim.NeovimTesting] switches itself off as soon as the editor has more
 * than one caret). The expectation for each caret is the one vim gives for the single-caret case,
 * which is verified in [RepeatInsertCommandsTest] and [RepeatCountTest].
 */
class RepeatMultiCaretTest : VimTestCase() {

  @Test
  fun `test repeat a change word with two carets`() {
    doTest(
      "cwfoo<Esc>" + "j0" + ".",
      """
        ${c}alpha one
        beta two
        ${c}gamma three
        delta four
      """.trimIndent(),
      """
        foo one
        fo${c}o two
        foo three
        fo${c}o four
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat a substitute with two carets`() {
    doTest(
      "sX<Esc>" + "l" + ".",
      """
        ${c}alpha one
        ${c}beta two
      """.trimIndent(),
      """
        X${c}Xpha one
        X${c}Xta two
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat a delete to end of line with two carets`() {
    doTest(
      "D" + "j0" + ".",
      """
        alpha${c} one
        beta two
        gamma${c} three
        delta four
      """.trimIndent(),
      """
        alpha
        ${c}
        gamma
        ${c}
      """.trimIndent(),
    )
  }
}
