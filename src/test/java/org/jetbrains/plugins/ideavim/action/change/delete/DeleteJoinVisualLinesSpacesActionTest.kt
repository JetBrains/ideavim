/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DeleteJoinVisualLinesSpacesActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test join via idea`() {
    doTest(
      "VjJ",
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet, consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set ideajoin")
    }
  }
}
