/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.screen

import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionMiddleScreenLineActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of full screen with odd number of lines`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of full screen with even number of lines`() {
    configureByLines(50, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of scrolled down screen`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(50, 50)
    typeText("M")
    assertPosition(67, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line when file is shorter than screen`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line when file is shorter than screen 2`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line when file is shorter than screen 3`() {
    configureByLines(20, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line when file is shorter than screen 4`() {
    configureByLines(21, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of visible lines with virtual space enabled`() {
    configureByLines(30, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(20, 20)
    typeText("M")
    assertPosition(25, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to same column with nostartofline`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(0, 0, 10)
    typeText("M")
    assertPosition(17, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to end of shorter line with nostartofline`() {
    configureByLines(70, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(0, 0, 10)
    typeText("A", " extra text", "<Esc>")
    typeText("M")
    assertPosition(17, 33)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test operator pending acts to middle line`() {
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 4, 10)
    typeText("dM")
    assertPosition(4, 4)
    assertLineCount(13)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test operator pending acts to middle line with nostartofline`() {
    configureByLines(20, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(0, 4, 10)
    typeText("dM")
    assertPosition(4, 10)
    assertLineCount(13)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of screen with block inlays above`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(5, 5), true, 5)
    typeText("M")
    assertPosition(12, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of screen with block inlays below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(25, 5), true, 5)
    typeText("M")
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of screen with block inlays above and below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(5, 5), true, 5)
    addBlockInlay(fixture.editor.vim.getOffset(25, 5), true, 5)
    typeText("M")
    assertPosition(12, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test move caret to middle line of screen with block inlays and a file shorter than the screen`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(5, 5), true, 5)
    setPositionAndScroll(0, 0)
    typeText("M")
    assertPosition(8, 4)
  }
}
