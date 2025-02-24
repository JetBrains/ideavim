/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class SelectMotionBigWordRightActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter =
    """
      |${s}Lorem ${c}I${se}psum
      |
      |Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Vim uses 'selection', with a default value of inclusive." +
      "IdeaVim treats Select as exclusive, because it's more intuitive"
  )
  @Test
  fun `test big word right in Select mode`() {
    doTest(
      listOf("gh", "<C-Right>"),
      """
        |${c}Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @Test
  fun `test big word right in Select mode with exclusive selection`() {
    doTest(
      listOf("gh", "<C-Right>"),
      """
        |${c}Lorem Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |${s}Lorem ${c}${se}Ipsum
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set selection=exclusive")
    }
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem Ipsu${s}m
      |${c}
      |${se}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Vim uses 'selection', with a default value of inclusive." +
      "IdeaVim treats Select as exclusive, because it's more intuitive"
  )
  @Test
  fun `test motion at end of line`() {
    doTest(
      listOf("gh", "<C-Right>"),
      """
        |Lorem Ipsu${c}m
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsu${s}m
        |${c}${se}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @Test
  fun `test motion at end of line with exclusive selection`() {
    doTest(
      listOf("gh", "<C-Right>"),
      """
        |Lorem Ipsu${c}m
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsu${s}m
        |${c}${se}
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    ) {
      enterCommand("set selection=exclusive")
    }
  }
}
