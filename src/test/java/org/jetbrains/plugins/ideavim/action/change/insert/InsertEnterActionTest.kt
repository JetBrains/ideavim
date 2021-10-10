/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
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
    doTest(listOf("i", "<Enter>"), before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

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
    doTest(listOf("i", "<C-M>"), before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert enter scrolls view up at scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "I found it in a legendary land")
    setPositionAndScroll(5, 29)
    typeText(parseKeys("i", "<Enter>"))
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }
}
