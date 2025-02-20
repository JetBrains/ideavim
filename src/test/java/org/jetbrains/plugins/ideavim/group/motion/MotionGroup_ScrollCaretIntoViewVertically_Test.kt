/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.helper.EditorHelper
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewVertically_Test : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving up causes scrolling up`() {
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText("12k")
    assertPosition(12, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(19, 24)

    typeText("12k")
    assertPosition(12, 0)
    assertVisibleArea(3, 37)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=5")
    setPositionAndScroll(19, 29)

    typeText("12k")
    assertPosition(17, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with scrolljump and scrolloff 1`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(19, 29)

    typeText("12k")
    assertPosition(17, 0)
    assertVisibleArea(8, 42)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with scrolljump and scrolloff 2`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(29, 39)

    typeText("20k")
    assertPosition(19, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with collapsed folds`() {
    configureByPages(5)
    // TODO: Implement zf
    typeText("40G", "Vjjjj", ":'< +'>action CollapseSelection<CR>", "V")
    setPositionAndScroll(29, 49)

    typeText("30k")
    assertPosition(15, 0)
    assertVisibleArea(15, 53)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up more than half height moves caret to middle 1`() {
    configureByPages(5)
    setPositionAndScroll(115, 149)

    typeText("50k")
    assertPosition(99, 0)
    assertVisualLineAtMiddleOfScreen(99)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up more than half height moves caret to middle with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(99, 109)
    assertPosition(109, 0)

    typeText("21k")
    assertPosition(88, 0)
    assertVisualLineAtMiddleOfScreen(88)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up with less than half height moves caret to top of screen`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(99, 109)

    typeText("20k")
    assertPosition(89, 0)
    assertVisibleArea(80, 114)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test moving down causes scrolling down`() {
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText("12j")
    assertPosition(41, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(0, 29)

    typeText("12j")
    assertPosition(41, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("12j")
    assertPosition(36, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 1`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("12j")
    assertPosition(36, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 2`() {
    configureByPages(5)
    enterCommand("set scrolljump=15")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("20j")
    assertPosition(44, 0)
    assertVisibleArea(17, 51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 3`() {
    configureByPages(5)
    enterCommand("set scrolljump=20")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("25j")
    assertPosition(49, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 4`() {
    configureByPages(5)
    enterCommand("set scrolljump=11")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("12j")
    assertPosition(36, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 5`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 29)

    typeText("12j")
    assertPosition(41, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down with scrolljump and scrolloff 6`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=5")
    setPositionAndScroll(0, 24)

    typeText("20j")
    assertPosition(44, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll down too large cursor is centred`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 19)

    typeText("35j")
    assertPosition(54, 0)
    assertVisualLineAtMiddleOfScreen(54)
  }

  private fun assertVisualLineAtMiddleOfScreen(expected: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      kotlin.test.assertEquals(expected, EditorHelper.getVisualLineAtMiddleOfScreen(fixture.editor))
    }
  }
}
