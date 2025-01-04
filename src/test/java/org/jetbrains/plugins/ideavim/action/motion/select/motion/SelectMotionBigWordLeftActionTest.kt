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
class SelectMotionBigWordLeftActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem ${s}${c}Ipsu${se}m
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
  fun `test big word left in Select mode`() {
    doTest(
      listOf("gh", "<C-Left>"),
      """
        |Lorem Ips${c}um
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ${s}${c}Ips${se}um
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
  fun `test big word left in Select mode with exclusive selection`() {
    doTest(
      listOf("gh", "<C-Left>"),
      """
        |Lorem Ips${c}um
        |
        |Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem ${s}${c}Ips${se}um
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
      |Lorem Ipsum
      |${s}${c}
      |L${se}orem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "Vim uses 'selection', with a default value of inclusive." +
      "IdeaVim treats Select as exclusive, because it's more intuitive." +
      "Also, IdeaVim moves the caret when entering Select, Vim does not"
  )
  @Test
  fun `test motion at start of line`() {
    doTest(
      // TODO: This should only be one <C-Left> to get the behaviour above
      // IdeaVim moves the caret one character right when entering Select mode. So the first <C-Left> moves it to where
      // it should be if that was working correctly. The second <C-Left> should select the same range as Vim, except
      // IdeaVim is always exclusive, while Vim's range is inclusive
      listOf("gh", "<C-Left>", "<C-Left>"),
      """
        |Lorem Ipsum
        |
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |${s}${c}
        |${se}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.SELECT(SelectionType.CHARACTER_WISE)
    )
  }

  @VimBehaviorDiffers(originalVimAfter =
    """
      |Lorem Ipsum
      |${s}${c}
      |${se}Lorem ipsum dolor sit amet,
      |consectetur adipiscing elit
      |Sed in orci mauris.
      |Cras id tellus in ex imperdiet egestas.
    """,
    description = "IdeaVim moves the caret when entering Select"
  )
  @Test
  fun `test motion at start of line with exclusive selection`() {
    doTest(
      // TODO: This should only be one <C-Left> to get the behaviour above
      // IdeaVim moves the caret one character right when entering Select mode. So the first <C-Left> moves it to where
      // it should be if that was working correctly. The second <C-Left> should select the same range as Vim.
      listOf("gh", "<C-Left>", "<C-Left>"),
      """
        |Lorem Ipsum
        |
        |${c}Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |${s}${c}
        |${se}Lorem ipsum dolor sit amet,
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
