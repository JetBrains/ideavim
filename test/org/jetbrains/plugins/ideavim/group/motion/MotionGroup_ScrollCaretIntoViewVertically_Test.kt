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

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

@Suppress("ClassName")
class MotionGroup_ScrollCaretIntoViewVertically_Test : VimTestCase() {
  fun `test moving up causes scrolling up`() {
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(12, 46)
  }

  fun `test scroll up with scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(19, 24)

    typeText(parseKeys("12k"))
    assertPosition(12, 0)
    assertVisibleArea(3, 37)
  }

  fun `test scroll up with scrolloff`() {
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(19, 29)

    typeText(parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(12, 46)
  }

  fun `test scroll up with scrolljump and scrolloff 1`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)

    setPositionAndScroll(19, 29)
    typeText(parseKeys("12k"))
    assertPosition(17, 0)
    assertVisibleArea(8, 42)
  }

  fun `test scroll up with scrolljump and scrolloff 2`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(29, 39)

    typeText(parseKeys("20k"))
    assertPosition(19, 0)
    assertVisibleArea(10, 44)
  }

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

  fun `test scroll up more than half height moves caret to middle 1`() {
    configureByPages(5)
    setPositionAndScroll(115, 149)

    typeText(parseKeys("50k"))
    assertPosition(99, 0)
    assertVisualLineAtMiddleOfScreen(99)
  }

  fun `test scroll up more than half height moves caret to middle with scrolloff`() {
    configureByPages(5)
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    setPositionAndScroll(99, 109)
    assertPosition(109, 0)

    typeText(parseKeys("21k"))
    assertPosition(88, 0)
    assertVisualLineAtMiddleOfScreen(88)
  }

  fun `test scroll up with less than half height moves caret to top of screen`() {
    configureByPages(5)
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    setPositionAndScroll(99, 109)

    typeText(parseKeys("20k"))
    assertPosition(89, 0)
    assertVisibleArea(80, 114)
  }

  fun `test moving down causes scrolling down`() {
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(7, 41)
  }

  fun `test scroll down with scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(11, 45)
  }

  fun `test scroll down with scrolloff`() {
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(7, 41)
  }

  fun `test scroll down with scrolljump and scrolloff 1`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(10, 44)
  }

  fun `test scroll down with scrolljump and scrolloff 2`() {
    OptionsManager.scrolljump.set(15)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(17, 51)
  }

  fun `test scroll down with scrolljump and scrolloff 3`() {
    OptionsManager.scrolljump.set(20)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("25j"))
    assertPosition(49, 0)
    assertVisibleArea(24, 58)
  }

  fun `test scroll down with scrolljump and scrolloff 4`() {
    OptionsManager.scrolljump.set(11)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("12j"))
    assertPosition(36, 0)
    assertVisibleArea(11, 45)
  }

  fun `test scroll down with scrolljump and scrolloff 5`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 29)

    typeText(parseKeys("12j"))
    assertPosition(41, 0)
    assertVisibleArea(12, 46)
  }

  fun `test scroll down with scrolljump and scrolloff 6`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(5)
    configureByPages(5)
    setPositionAndScroll(0, 24)

    typeText(parseKeys("20j"))
    assertPosition(44, 0)
    assertVisibleArea(15, 49)
  }

  fun `test scroll down too large cursor is centred`() {
    OptionsManager.scrolljump.set(10)
    OptionsManager.scrolloff.set(10)
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