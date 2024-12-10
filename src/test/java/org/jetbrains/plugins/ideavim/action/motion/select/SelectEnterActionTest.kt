/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SelectEnterActionTest : VimTestCase() {
  @Test
  fun `test enter in Select`() {
    doTest(
      "gh<CR>",
      """
        |Lorem ipsum
        |
        |Lorem ip${c}sum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum
        |
        |Lorem ip
        |${c}um dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test CTRL-J in Select`() {
    doTest(
      "gh<C-J>",
      """
        |Lorem ipsum
        |
        |Lorem ip${c}sum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum
        |
        |Lorem ip
        |${c}um dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test CTRL-M in Select`() {
    doTest(
      "gh<C-M>",
      """
        |Lorem ipsum
        |
        |Lorem ip${c}sum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ipsum
        |
        |Lorem ip
        |${c}um dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }
}
