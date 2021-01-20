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

import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *zb*
zb                      Like "z-", but leave the cursor in the same column.
 */
class ScrollLastScreenLineActionTest : VimTestCase() {
  fun `test scroll current line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(StringHelper.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen and leave cursor in current column`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60, 14)
    typeText(StringHelper.parseKeys("zb"))
    assertPosition(60, 14)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen minus scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(StringHelper.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(36, 70)
  }

  fun `test scrolls count line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(StringHelper.parseKeys("100zb"))
    assertPosition(99, 0)
    assertVisibleArea(65, 99)
  }

  fun `test scrolls count line to bottom of screen minus scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(StringHelper.parseKeys("100zb"))
    assertPosition(99, 0)
    assertVisibleArea(75, 109)
  }

  fun `test scrolls current line to bottom of screen ignoring scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(StringHelper.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scrolls correctly when less than a page to scroll`() {
    configureByPages(5)
    setPositionAndScroll(5, 15)
    typeText(StringHelper.parseKeys("zb"))
    assertPosition(15, 0)
    assertVisibleArea(0, 34)
  }
}