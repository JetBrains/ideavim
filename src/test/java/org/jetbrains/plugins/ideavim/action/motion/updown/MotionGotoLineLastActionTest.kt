/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionGotoLineLastActionTest : VimTestCase() {
  @Test
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
    )
  }

  @Test
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
    )
  }

  @Test
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
    )
  }

  @Test
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
    )
  }

  @Test
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
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test moves caret to same column with nostartofline`() {
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
    ) {
      enterCommand("set nostartofline")
    }
  }

  @Test
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
    )
  }

  @Test
  fun `test scrolling positions target line in middle of screen`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    typeText("70G")
    assertPosition(69, 4)
    assertVisibleArea(52, 86)
  }

  @Test
  fun `test go to last line of file puts target line at bottom of screen`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  @Test
  fun `test go to last line of file puts target line at bottom of screen with virtual space enabled`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setEditorVirtualSpace()
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  @Test
  fun `test go to line in last half screen of file puts last line at bottom of screen`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    typeText("90G")
    assertPosition(89, 4)
    assertVisibleArea(65, 99)
  }

  @Test
  fun `test go to line in last half screen of file puts last line at bottom of screen ignoring scrolloff`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    typeText("95G")
    assertPosition(94, 4)
    assertVisibleArea(65, 99)
  }

  @Test
  fun `test go to line does not scroll when default virtual space already at bottom of file`() {
    // Editor has 5 lines of virtual space by default
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(69, 85)
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(69, 99)
  }

  @Test
  fun `test go to line does not scroll when full virtual space already at bottom of file`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setEditorVirtualSpace()
    setPositionAndScroll(85, 85)
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(85, 99)
  }

  @Test
  fun `test go to line does not scroll when last line is less than scrolloff above bottom of file`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setEditorVirtualSpace()
    setPositionAndScroll(67, 97)
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(67, 99)
  }

  @Test
  fun `test go to line does not scroll when last line is less than scrolloff above bottom of file with folds`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setEditorVirtualSpace()
    typeText("20G", "V10j", ":'< +'>action CollapseSelection<CR>", "V")
    setPositionAndScroll(67, 97)
    typeText("G")
    assertPosition(99, 4)
    assertVisibleArea(67, 99)
  }
}
