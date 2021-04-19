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

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionGotoLineLastActionTest : VimTestCase() {
  fun `test simple motion`() {
    doTest(
      "G",
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion with count`() {
    doTest(
      "5G",
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                ${c}where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion with large count`() {
    doTest(
      "100G",
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test motion with zero count`() {
    doTest(
      "0G",
      """
                A ${c}Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                ${c}hard by the torrent of a mountain pass.
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test moves caret to first non-blank char`() {
    doTest(
      "G",
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       ${c}hard by the torrent of a mountain pass.
      """.trimMargin(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test moves caret to same column with nostartofline`() {
    OptionsManager.startofline.reset()
    doTest(
      "G",
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all ${c}rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard by the torrent of a mountain pass.
      """.trimMargin(),
      """
        |       A Discovery
        |
        |       I found it in a legendary land
        |       all rocks and lavender and tufted grass,
        |       where it was settled on some sodden sand
        |       hard$c by the torrent of a mountain pass.
      """.trimMargin(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test with last empty line`() {
    doTest(
      "G",
      """
                A Discovery

                I found it in a legendary land
                all ${c}rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                
      """.trimIndent(),
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                $c
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test scrolling positions target line in middle of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("70G"))
    assertPosition(69, 4)
    assertVisibleArea(52, 86)
  }

  fun `test go to last line of file puts target line at bottom of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to last line of file puts target line at bottom of screen with virtual space enabled`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    typeText(parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line in last half screen of file puts last line at bottom of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("90G"))
    assertPosition(89, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line in last half screen of file puts last line at bottom of screen ignoring scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    typeText(parseKeys("95G"))
    assertPosition(94, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line does not scroll when default virtual space already at bottom of file`() {
    // Editor has 5 lines of virtual space by default
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(69, 85)
    typeText(parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(69, 99)
  }

  fun `test go to line does not scroll when full virtual space already at bottom of file`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(85, 85)
    typeText(parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(85, 99)
  }

  fun `test go to line does not scroll when last line is less than scrolloff above bottom of file`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(67, 97)
    typeText(parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(67, 99)
  }
}
