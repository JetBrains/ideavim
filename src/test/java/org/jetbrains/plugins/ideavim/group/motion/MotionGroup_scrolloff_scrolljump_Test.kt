/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package org.jetbrains.plugins.ideavim.group.motion

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

// These tests are sanity tests for scrolloff and scrolljump, with actions that move the cursor. Other actions that are
// affected by scrolloff or scrolljump should include that in the action specific tests
class MotionGroup_scrolloff_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up shows no context with scrolloff=0`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up shows context line with scrolloff=1`() {
    configureByPages(5)
    enterCommand("set scrolloff=1")
    setPositionAndScroll(25, 26)
    typeText("k")
    assertPosition(25, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up shows context lines with scrolloff=10`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(25, 35)
    typeText("k")
    assertPosition(34, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height`() {
    // Screen height = 35. scrolloff=15. This gives 5 possible caret lines without scrolling (48, 49, 50, 51 + 52)
    configureByPages(5)
    enterCommand("set scrolloff=15")
    setPositionAndScroll(33, 52)

    typeText("k")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("k")
    assertPosition(47, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height 2`() {
    // Screen height = 35. scrolloff=16. This gives 3 possible caret lines without scrolling (49, 50 + 51)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setPositionAndScroll(33, 51)

    typeText("k")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up when scrolloff is slightly less than half screen height 3`() {
    // Screen height = 34. scrolloff=16
    // Even numbers. 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  @VimBehaviorDiffers(description = "Moving up in Vim will always have 16 lines above the caret line. IdeaVim keeps 17")
  fun `test move up when scrolloff is exactly screen height`() {
    // Page height = 34. scrolloff=17
    // 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("k")
    assertPosition(48, 0)
    assertVisibleArea(32, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up when scrolloff is slightly greater than screen height keeps cursor in centre of screen`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up when scrolloff is slightly greater than screen height keeps cursor in centre of screen 2`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=22")
    setPositionAndScroll(33, 50)

    typeText("k")
    assertPosition(49, 0)
    assertVisibleArea(32, 66)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down shows no context with scrolloff=0`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down shows context line with scrolloff=1`() {
    configureByPages(5)
    enterCommand("set scrolloff=1")
    setPositionAndScroll(25, 58)
    typeText("j")
    assertPosition(59, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down shows context lines with scrolloff=10`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(25, 49)
    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height`() {
    // Screen height = 35. scrolloff=15. This gives 5 possible caret lines without scrolling (48, 49, 50, 51 + 52)
    configureByPages(5)
    enterCommand("set scrolloff=15")
    setPositionAndScroll(33, 48)

    typeText("j")
    assertPosition(49, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(52, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("j")
    assertPosition(53, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height 2`() {
    // Screen height = 35. scrolloff=16. This gives 3 possible caret lines without scrolling (49, 50 + 51)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(33, 67)

    // Scroll
    typeText("j")
    assertPosition(52, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is slightly less than half screen height 3`() {
    // Screen height = 34. scrolloff=16
    // Even numbers. 2 possible caret lines without scrolling (49 + 50)
    configureByPages(5)
    enterCommand("set scrolloff=16")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 66)

    // Scroll
    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is exactly screen height`() {
    // Page height = 34. scrolloff=17
    // 2 possible caret lines without scrolling (49 + 50), but moving to line 51 will scroll 2 lines!
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(33, 49)

    typeText("j")
    assertPosition(50, 0)
    assertVisibleArea(33, 66)

    // Scroll. By 2 lines!
    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(35, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is slightly greater than half screen height keeps cursor in centre of screen`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=17")
    setPositionAndScroll(33, 50)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down when scrolloff is slightly greater than half screen height keeps cursor in centre of screen 2`() {
    // Page height = 35. scrolloff=17
    configureByPages(5)
    enterCommand("set scrolloff=22")
    setPositionAndScroll(33, 50)

    typeText("j")
    assertPosition(51, 0)
    assertVisibleArea(34, 68)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolloff=999 keeps cursor in centre of screen`() {
    configureByPages(5)
    enterCommand("set scrolloff=999")
    setPositionAndScroll(25, 42)

    typeText("j")
    assertPosition(43, 0)
    assertVisibleArea(26, 60)

    typeText("k")
    assertPosition(42, 0)
    assertVisibleArea(25, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolloff=999 keeps cursor in centre of screen with even screen height`() {
    configureByPages(5)
    enterCommand("set scrolloff=999")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(26, 42)

    typeText("j")
    assertPosition(43, 0)
    assertVisibleArea(27, 60)

    typeText("k")
    assertPosition(42, 0)
    assertVisibleArea(26, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test reposition cursor when scrolloff is set`() {
    configureByPages(5)
    enterCommand("set scrolloff=0")
    setPositionAndScroll(50, 50)

    enterCommand("set scrolloff=999")
    assertPosition(50, 0)
    assertVisibleArea(33, 67)
  }
}

class MotionGroup_scrolljump_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up scrolls single line with scrolljump=0`() {
    configureByPages(5)
    enterCommand("set scrolljump=0")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up scrolls single line with scrolljump=1`() {
    configureByPages(5)
    enterCommand("set scrolljump=1")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move up scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(25, 25)
    typeText("k")
    assertPosition(24, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down scrolls single line with scrolljump=0`() {
    configureByPages(5)
    enterCommand("set scrolljump=0")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down scrolls single line with scrolljump=1`() {
    configureByPages(5)
    enterCommand("set scrolljump=1")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test move down scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(25, 59)
    typeText("j")
    assertPosition(60, 0)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test negative scrolljump treated as percentage 1`() {
    configureByPages(5)
    enterCommand("set scrolljump=-50")
    setPositionAndScroll(39, 39)
    typeText("k")
    assertPosition(38, 0)
    assertVisibleArea(22, 56)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test negative scrolljump treated as percentage 2`() {
    configureByPages(5)
    enterCommand("set scrolljump=-10")
    setPositionAndScroll(39, 39)
    typeText("k")
    assertPosition(38, 0)
    assertVisibleArea(36, 70)
  }
}

class MotionGroup_scrolloff_scrolljump_Test : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with scrolloff and scrolljump set`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(50, 55)
    typeText("k")
    assertPosition(54, 0)
    assertVisibleArea(40, 74)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolloff and scrolljump set`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(50, 79)
    typeText("j")
    assertPosition(80, 0)
    assertVisibleArea(60, 94)
  }
}
