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

package org.jetbrains.plugins.ideavim.action.scroll

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *zz*
zz                      Like "z.", but leave the cursor in the same column.
                        Careful: If caps-lock is on, this command becomes
                        "ZZ": write buffer and exit!
 */
class ScrollMiddleScreenLineActionTest : VimTestCase() {
  fun `test scrolls current line to middle of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(parseKeys("zz"))
    assertPosition(45, 0)
    assertVisibleArea(28, 62)
  }

  fun `test scrolls current line to middle of screen and keeps cursor in the same column`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 45, 14)
    typeText(parseKeys("zz"))
    assertPosition(45, 14)
    assertVisibleArea(28, 62)
  }

  fun `test scrolls count line to the middle of the screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(parseKeys("100zz"))
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  fun `test scrolls count line ignoring scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(parseKeys("100zz"))
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  fun `test scrolls correctly when count line is in first half of first page`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(parseKeys("10zz"))
    assertPosition(9, 0)
    assertVisibleArea(0, 34)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  fun `test scrolls last line of file correctly`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("175zz"))
    assertPosition(174, 0)
    assertVisibleArea(146, 175)
  }
}