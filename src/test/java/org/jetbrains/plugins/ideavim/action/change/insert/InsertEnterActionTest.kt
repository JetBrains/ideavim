/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertEnterActionTest : VimTestCase() {
  fun `test insert enter`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest(listOf("i", "<Enter>"), before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  fun `test insert enter with C-M`() {
    val before = """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    val after = """I found it in a legendary land
        |
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    doTest(listOf("i", "<C-M>"), before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert enter scrolls view up at scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByLines(50, "I found it in a legendary land")
    setPositionAndScroll(5, 29)
    typeText(injector.parser.parseKeys("i" + "<Enter>"))
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }
}
