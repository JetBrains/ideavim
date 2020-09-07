/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteVisualLinesActionTest : VimTestCase() {
  fun `test remove line in char visual mode`() {
    doTest("vlllX",
      """
                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(originalVimAfter = """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand""")
  fun `test remove line in char visual mode last line`() {
    doTest("vlllX",
      """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
      """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden san${c}d

                """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test remove line in line visual mode`() {
    doTest("VX",
      """
                I found ${c}it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                ${c}all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(originalVimAfter = """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand""")
  fun `test remove line in line visual mode line end`() {
    doTest("VX",
      """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by ${c}the torrent of a mountain pass.""".trimIndent(),
      """
                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden san${c}d
                
                """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }
}
