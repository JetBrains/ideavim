/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class VisualToggleBlockModeActionTest : VimTestCase() {
  @Test
  fun `test enter visual with count`() {
    doTest(
      "1<C-V>",
      """
                    Lorem Ipsum

                    I ${c}found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                    Lorem Ipsum

                    I ${s}${c}f${se}ound it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.BLOCK_WISE),
    )
  }

  @Test
  fun `test enter visual with five count`() {
    doTest(
      "5<C-V>",
      """
                    Lorem Ipsum

                    I ${c}found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                    Lorem Ipsum

                    I ${s}foun${c}d${se} it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.BLOCK_WISE),
    )
  }

  @Test
  fun `test enter visual with 100 count`() {
    doTest(
      "100<C-V>",
      """
                    Lorem Ipsum

                    I ${c}found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                    Lorem Ipsum

                    I ${s}found it in a legendary land${c}${se}
                    consectetur adipiscing elit
                    Sed in orci mauris.
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.VISUAL(SelectionType.BLOCK_WISE),
    )
  }

  @Test
  fun `test on empty file`() {
    doTest(
      "<C-V>",
      "",
      "",
      Mode.VISUAL(SelectionType.BLOCK_WISE),
    )
  }

  @Test
  fun `test selectmode option`() {
    configureByText(
      """
                    Lorem Ipsum

                    I${c} found it in a legendary land
                    consectetur adipiscing elit
                    Sed in orci mauris.[long line]
                    Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    enterCommand("set selectmode=cmd")
    typeText("<C-V>")
    assertState(Mode.SELECT(SelectionType.BLOCK_WISE))
  }
}
