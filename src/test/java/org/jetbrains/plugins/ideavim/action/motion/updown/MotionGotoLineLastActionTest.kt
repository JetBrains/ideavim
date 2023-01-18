/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test moves caret to same column with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startofline)
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
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
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test scrolling positions target line in middle of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(injector.parser.parseKeys("70G"))
    assertPosition(69, 4)
    assertVisibleArea(52, 86)
  }

  fun `test go to last line of file puts target line at bottom of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to last line of file puts target line at bottom of screen with virtual space enabled`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line in last half screen of file puts last line at bottom of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    typeText(injector.parser.parseKeys("90G"))
    assertPosition(89, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line in last half screen of file puts last line at bottom of screen ignoring scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    typeText(injector.parser.parseKeys("95G"))
    assertPosition(94, 4)
    assertVisibleArea(65, 99)
  }

  fun `test go to line does not scroll when default virtual space already at bottom of file`() {
    // Editor has 5 lines of virtual space by default
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(69, 85)
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(69, 99)
  }

  fun `test go to line does not scroll when full virtual space already at bottom of file`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(85, 85)
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(85, 99)
  }

  fun `test go to line does not scroll when last line is less than scrolloff above bottom of file`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(67, 97)
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(67, 99)
  }

  fun `test go to line does not scroll when last line is less than scrolloff above bottom of file with folds`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    typeText(injector.parser.parseKeys("20G" + "V10j" + ":'< +'>action CollapseSelection<CR>" + "V"))
    setPositionAndScroll(67, 97)
    typeText(injector.parser.parseKeys("G"))
    assertPosition(99, 4)
    assertVisibleArea(67, 99)
  }
}
