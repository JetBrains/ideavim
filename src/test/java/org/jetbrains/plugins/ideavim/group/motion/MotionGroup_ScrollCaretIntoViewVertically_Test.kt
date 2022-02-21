/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewVertically_Test : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test moving up causes scrolling up`() {
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(3, 37)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(19, 29)

    typeText(parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump and scrolloff 1`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)

    setPositionAndScroll(19, 29)
    typeText(parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(8, 42)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump and scrolloff 2`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(29, 39)

    typeText(parseKeys("20k"))
    assertPosition(19, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with collapsed folds`() {
    configureByPages(5)
    // TODO: Implement zf
    typeText(parseKeys("40G", "Vjjjj", ":'<,'>action CollapseSelection<CR>", "V"))
    setPositionAndScroll(29, 49)

    typeText(parseKeys("30k"))
    assertPosition(15, 0)
    assertVisibleArea(15, 53)
  }

  // TODO: Handle soft wraps
//  fun `test scroll up with soft wraps`() {
//  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up more than half height moves caret to middle 1`() {
    configureByPages(5)
    setPositionAndScroll(115, 149)

    typeText(parseKeys("50k"))
    assertPosition(99, 0)
    assertVisualLineAtMiddleOfScreen(99)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up more than half height moves caret to middle with scrolloff`() {
    configureByPages(5)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    setPositionAndScroll(99, 109)
    assertPosition(109, 0)

    typeText(parseKeys("21k"))
    assertPosition(88, 0)
    assertVisualLineAtMiddleOfScreen(88)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with less than half height moves caret to top of screen`() {
    configureByPages(5)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    setPositionAndScroll(99, 109)

    typeText(parseKeys("20k"))
    assertPosition(89, 0)
    assertVisibleArea(80, 114)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test moving down causes scrolling down`() {
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 1`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 2`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(15))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(17, 51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 3`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(20))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("25j"))
    assertPosition(49, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 4`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(11))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 5`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 6`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down too large cursor is centred`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 19)

    typeText(parseKeys("35j"))
    assertPosition(54, 0)
    assertVisualLineAtMiddleOfScreen(54)
  }

  private fun assertVisualLineAtMiddleOfScreen(expected: Int) {
    assertEquals(expected, EditorHelper.getVisualLineAtMiddleOfScreen(myFixture.editor))
  }
}
