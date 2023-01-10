/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewVertically_Test : VimTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test moving up causes scrolling up`() {
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(injector.parser.parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(injector.parser.parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(3, 37)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(19, 29)

    typeText(injector.parser.parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump and scrolloff 1`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)

    setPositionAndScroll(19, 29)
    typeText(injector.parser.parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(8, 42)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with scrolljump and scrolloff 2`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(29, 39)

    typeText(injector.parser.parseKeys("20k"))
    assertPosition(19, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with collapsed folds`() {
    configureByPages(5)
    // TODO: Implement zf
    typeText(injector.parser.parseKeys("40G" + "Vjjjj" + ":'< +'>action CollapseSelection<CR>" + "V"))
    setPositionAndScroll(29, 49)

    typeText(injector.parser.parseKeys("30k"))
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

    typeText(injector.parser.parseKeys("50k"))
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

    typeText(injector.parser.parseKeys("21k"))
    assertPosition(88, 0)
    assertVisualLineAtMiddleOfScreen(88)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll up with less than half height moves caret to top of screen`() {
    configureByPages(5)
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    setPositionAndScroll(99, 109)

    typeText(injector.parser.parseKeys("20k"))
    assertPosition(89, 0)
    assertVisibleArea(80, 114)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test moving down causes scrolling down`() {
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(7, 41)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 1`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(10, 44)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 2`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(15))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(17, 51)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 3`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(20))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("25j"))
    assertPosition(49, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 4`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(11))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(11, 45)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 5`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(injector.parser.parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(12, 46)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down with scrolljump and scrolloff 6`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(5))
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(injector.parser.parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.SCROLL)
  fun `test scroll down too large cursor is centred`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 19)

    typeText(injector.parser.parseKeys("35j"))
    assertPosition(54, 0)
    assertVisualLineAtMiddleOfScreen(54)
  }

  private fun assertVisualLineAtMiddleOfScreen(expected: Int) {
    assertEquals(expected, EditorHelper.getVisualLineAtMiddleOfScreen(myFixture.editor))
  }
}
