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

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class SearchWholeWordForwardActionTest : VimTestCase() {
  fun `test with tabs`() {
    val before = """
  .he${c}llo 1
  .hello 2
  .hello 3
""".trimIndent().dotToTab()
    val keys = "**"
    val after = """
  .hello 1
  .hello 2
  .${c}hello 3
""".trimIndent().dotToTab()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test backward search on empty string`() {
    doTest("*", "", "", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertPluginError(false)
  }

  @VimBehaviorDiffers(originalVimAfter =
        """
          I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass${c}.
        """
  )
  fun `test last dot`() {
    doTest("*",
      """
          I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass${c}.
        """.trimIndent(),
      """
          ${c}I found it in a legendary land
          all rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass.
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test last word`() {
    doTest("*",
      """
          I found it in a legendary land
          all |pass| rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pas${c}s
        """.trimIndent(),
      """
          I found it in a legendary land
          all |${c}pass| rocks and lavender and tufted grass,
          where it was settled on some sodden sand
          hard by the torrent of a mountain pass
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
