/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertEnterActionTest : VimTestCase() {
  @Test
  fun `test insert enter`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<Enter>"), before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @Test
  fun `test insert enter with C-M`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<C-M>"), before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert enter scrolls view up at scrolloff`() {
    configureByLines(50, "Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 29)
    typeText("i", "<Enter>")
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }
}
