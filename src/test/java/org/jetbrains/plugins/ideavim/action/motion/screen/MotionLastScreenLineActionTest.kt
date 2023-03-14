/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.screen

import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionLastScreenLineActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last line of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText("L")
    assertPosition(34, 4)
  }

  @Test
  fun `test move caret to last line when last line of file is less than screen`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(20, "    I found it in a legendary land")
    typeText("L")
    assertPosition(19, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last line of screen when bottom of file is scrolled up`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(38, "    I found it in a legendary land")
    setPositionAndScroll(3, 5)
    typeText("L")
    assertPosition(37, 4)
    assertTopLogicalLine(3)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last line of screen when bottom of file is scrolled up with virtual space`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(38, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(15, 20)
    typeText("L")
    assertPosition(37, 4)
    assertTopLogicalLine(15)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to count line from bottom of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText("10L")
    assertPosition(25, 4)
  }

  @Test
  fun `test move caret to too large count line from bottom of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText("100L")
    assertPosition(0, 4)
  }

  @Test
  fun `test move caret to too large count line from bottom of screen 2`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40)
    typeText("100L")
    assertPosition(20, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret ignores scrolloff when bottom of screen is bottom of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(35, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    typeText("L")
    assertPosition(34, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret applies scrolloff when bottom of screen is not bottom of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    typeText("L")
    assertPosition(24, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last screen line with count and scrolloff at bottom of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(35, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    typeText("5L")
    assertPosition(30, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last screen line with count and scrolloff not at bottom of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    typeText("5L")
    assertPosition(24, 4)
  }

  @Test
  fun `test move caret ignores scrolloff with large count at top of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("100L")
    assertPosition(0, 4)
    assertTopLogicalLine(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret applies scrolloff with large count when not at top of file`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40)
    typeText("100L")
    assertPosition(30, 4)
    assertTopLogicalLine(20)
  }

  @Test
  fun `test operator pending acts to last screen line`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText("dL")
    assertPosition(40, 4)
    assertLineCount(85)
  }

  @Test
  fun `test operator pending acts to last screen line with scrolloff`() {
    // Current caret location is the start of the operator range and doesn't get moved to the end, so there is no
    // scrolling, and scrolloff does not apply
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40, 10)
    typeText("dL")
    assertTopLogicalLine(20)
    assertPosition(40, 4)
    assertLineCount(85)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on count line from bottom of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText("d5L")
    assertPosition(40, 4)
    assertLineCount(89)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on large count line from bottom of screen`() {
    // Operator range is from current line to bottom of screen minus count.
    // 35 high screen, 100 high file. Top line is 20, caret is 40, bottom is 54. d25L will delete from 40 to 54-25=29.
    // Range gets reversed, so we delete :29-40d. Caret stays at 40.
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText("d25L")
    assertPosition(30, 4)
    assertLineCount(89)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on count line from bottom of screen with scrolloff`() {
    // Current caret location is the start of the operator range and doesn't get moved to the end, so there is no
    // scrolling, and scrolloff does not apply
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40, 10)
    typeText("d5L")
    assertTopLogicalLine(20)
    assertPosition(40, 4)
    assertLineCount(89)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on large count from bottom of screen with scrolloff`() {
    // Current caret location is the start of the operator range and doesn't get moved to the end, so there is no
    // scrolling, and scrolloff does not apply
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40, 10)
    typeText("d35L")
    assertTopLogicalLine(10)
    assertPosition(20, 4)
    assertLineCount(79)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on large count line from bottom of screen with scrolloff and without virtual space`() {
    // When using a large count, the range is effectively reversed, and the current caret location becomes the end of
    // the range, and is moved, so scrolloff can apply
    // 50 high file. Top line 20, caret at 40. d35L will delete from current line to 35 lines up from the bottom of the
    // screen. There are only 10 actual lines below, so this will delete from current line up to current line - 10 = 30
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40, 10)
    typeText("d35L")
    assertTopLogicalLine(0)
    assertPosition(15, 4)
    assertLineCount(24)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on large count line from bottom of screen with scrolloff and virtual space`() {
    // When using a large count, the range is effectively reversed, and the current caret location becomes the end of
    // the range, and is moved, so scrolloff can apply
    // 50 high file. Top line 20, caret at 40. d35L will delete from current line to 35 lines up from the bottom of the
    // screen. There are only 10 actual lines below, so this will delete from current line up to current line - 10 = 30
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setEditorVirtualSpace()
    setPositionAndScroll(20, 40, 10)
    typeText("d35L")
    assertTopLogicalLine(5)
    assertPosition(15, 4)
    assertLineCount(24)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts to last screen line with nostartofline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 40, 10)
    typeText("dL")
    assertPosition(40, 10)
    assertLineCount(85)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test operator pending acts on count line from bottom of screen with nostartofline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 40, 10)
    typeText("d5L")
    assertPosition(40, 10)
    assertLineCount(89)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to same column with nostartofline`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(10, 30, 10)
    typeText("L")
    assertPosition(44, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to end of shorter line with nostartofline`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(10, 30, 10)
    typeText("A", " extra text", "<Esc>")
    typeText("L")
    assertPosition(44, 33)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last line of screen with inlays`() {
    // 35 high, with an inlay that is 10 rows high. Bottom line will be 25 (1 based)
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(fixture.editor).getOffset(20, 5), true, 10)
    setPositionAndScroll(0, 10, 10)
    typeText("L")
    assertPosition(24, 4)
    assertBottomLogicalLine(24)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move caret to last line of screen with inlays and scrolloff`() {
    // 35 high, with an inlay that is 10 rows high. Bottom line will be 25 (1 based), scrolloff of 10 puts caret at 15
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    addBlockInlay(IjVimEditor(fixture.editor).getOffset(20, 5), true, 10)
    setPositionAndScroll(0, 10, 10)
    typeText("L")
    assertPosition(14, 4)
    assertBottomLogicalLine(24)
  }

  @Test
  fun `test keep caret on screen when count is greater than visible lines plus inlays`() {
    // Screen is 35 high. Top line is 21 (1 based), inlay starts at 26, is 10 rows high and bottom line is 45.
    // Caret is at line 31. 35L should go to 34 lines above bottom line (L == 1L), which would be 11, which would be off
    // screen. Screen doesn't scroll, caret remains on screen at existing top line - 21
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(fixture.editor).getOffset(25, 5), true, 10)
    setPositionAndScroll(20, 30, 10)
    typeText("35L")
    assertPosition(20, 4)
    assertBottomLogicalLine(44)
  }
}
