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

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBigWordActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, description = "Wrong caret position, but in real neovim works fine")
  fun `test on last dot`() {
    doTest(
      "<aW", """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass${c}.
    """.trimIndent(), """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      ${c}hard by the torrent of a mountain pass.
    """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test past end in visual`() {
    doTest(
      "v\$aW", """
      I found it in a ${c}legendary land
      }
    """.trimIndent(), """
      I found it in a ${s}legendary land
      ${c}}${se}
    """.trimIndent(), CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }
}
