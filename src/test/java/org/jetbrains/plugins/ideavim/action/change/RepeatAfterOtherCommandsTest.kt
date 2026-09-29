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
 * What `.` repeats when the preceding command is not a plain operator or insert. All expectations
 * were taken from nvim rather than from IdeaVim's current output.
 */
class RepeatAfterOtherCommandsTest : VimTestCase() {

  @Test
  fun `test dot does not repeat an ex command`() {
    // `:h .` - "Does not repeat a command-line command." So the `.` repeats the `x`, not the `:s`
    doTest(
      "x" + ":s/lpha/GAMMA/<CR>" + "2j0" + ".",
      """
        ${c}alpha beta
        alpha beta
        alpha beta
      """.trimIndent(),
      """
        GAMMA beta
        alpha beta
        ${c}lpha beta
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat uppercase text object`() {
    doTest(
      "gUiw" + "w" + ".",
      """
        ${c}lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        LOREM ${c}IPSUM dolor
        sit amet consectetur
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat join`() {
    doTest(
      "J" + ".",
      """
        ${c}Lorem ipsum
        dolor sit
        amet consectetur
        adipiscing elit
      """.trimIndent(),
      """
        Lorem ipsum dolor sit${c} amet consectetur
        adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  fun `test dot executed from insert mode with CTRL-O`() {
    // `i_CTRL-O` runs one normal-mode command and returns to insert; that command can be `.`
    doTest(
      "x" + "j0" + "i<C-O>.<Esc>",
      """
        ${c}Lorem ipsum dolor
        sit amet consectetur
      """.trimIndent(),
      """
        orem ipsum dolor
        ${c}it amet consectetur
      """.trimIndent(),
    )
  }
}
