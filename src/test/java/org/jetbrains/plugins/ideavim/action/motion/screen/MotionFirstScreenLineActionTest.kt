/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.screen

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionFirstScreenLineActionTest : VimTestCase() {
  @Test
  fun `test move caret to first line of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText("H")
    assertPosition(0, 4)
  }

  @Test
  fun `test move caret to first line of screen further down file`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText("H")
    assertPosition(40, 4)
  }

  @Test
  fun `test move caret to count line from top of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText("10H")
    assertPosition(9, 4)
  }

  @Test
  fun `test move caret to count line from top of screen further down file`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText("10H")
    assertPosition(49, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret to too large count line from top of screen`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText("100H")
    assertPosition(74, 4)
  }

  @Test
  fun `test move caret ignores scrolloff when top of screen is top of file`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("H")
    assertPosition(0, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret applies scrolloff when top of screen is not top of file`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(1, 20)
    typeText("H")
    assertPosition(11, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret applies scrolloff when top of screen is not top of file 2`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40)
    typeText("H")
    assertPosition(30, 4)
  }

  @Test
  fun `test move caret to first screen line with count and scrolloff at top of file`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("5H")
    assertPosition(4, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret to first screen line with count and scrolloff not at top of file`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40)
    typeText("5H")
    assertPosition(30, 4)
  }

  @Test
  fun `test operator pending acts to first screen line`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText("dH")
    assertPosition(20, 4)
    assertLineCount(79)
  }

  @Test
  fun `test operator pending acts on count line from top of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText("d5H")
    assertPosition(24, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test operator pending acts to first screen line with nostartofline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 40, 10)
    typeText("dH")
    assertPosition(20, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test operator pending acts on count line from top of screen with nostartofline`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 40, 10)
    typeText("d5H")
    assertPosition(24, 10)
  }

  @Test
  fun `test operator pending acts to first screen line and then scrolls scrolloff`() {
    configureByLines(100, "    I found it in a legendary land")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 40)
    typeText("dH")
    assertPosition(20, 4)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret to same column with nostartofline`() {
    configureByLines(50, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(0, 20, 10)
    typeText("H")
    assertPosition(0, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test move caret to end of shorter line with nostartofline`() {
    configureByLines(70, "    I found it in a legendary land")
    enterCommand("set nostartofline")
    setPositionAndScroll(10, 30, 10)
    typeText("A", " extra text", "<Esc>")
    typeText("H")
    assertPosition(10, 33)
  }

  @Test
  fun `test move caret to first line of screen with inlays`() {
    // We're not scrolling, so inlays don't affect anything. Just place the caret on the first visible line
    configureByLines(50, "    I found it in a legendary land")
    ApplicationManager.getApplication().invokeAndWait {
      addBlockInlay(fixture.editor.vim.getOffset(5, 5), true, 10)
    }
    setPositionAndScroll(0, 20, 10)
    typeText("H")
    assertPosition(0, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test keep caret on screen when count is greater than visible lines plus inlays`() {
    kotlin.test.assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(fixture.editor.vim.getOffset(5, 5), true, 10)
    setPositionAndScroll(0, 20, 10)
    // Should move to the 34th visible line. We have space for 35 lines, but we're using some of that for inlays
    typeText("34H")
    assertPosition(24, 4)
  }
}
