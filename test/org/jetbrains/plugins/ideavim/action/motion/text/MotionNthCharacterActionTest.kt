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

package org.jetbrains.plugins.ideavim.action.motion.text

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionNthCharacterActionTest : VimTestCase() {
  fun `test goto without count`() {
    doTest("go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test goto with 0`() {
    doTest("0go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test goto with 1`() {
    doTest("1go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test goto with 2`() {
    doTest("2go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            A${c} Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test goto with 10`() {
    doTest("10go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            A Discove${c}ry

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test goto with 1000`() {
    doTest("1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass${c}.
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.${c}
  """)
  fun `test goto with 1000 in visual mode`() {
    doTest("v1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks ${s}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass${c}.${se}
        """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER
    )
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            $c
  """)
  fun `test goto with 1000 and new line`() {
    doTest("1000go",
      """
            A Discovery

            I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            
        """.trimIndent(),
      """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass$c.
            
        """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }
}
