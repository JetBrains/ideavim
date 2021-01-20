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
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z<CR>*
z<CR>                   Redraw, line [count] at top of window (default
                        cursor line).  Put cursor at first non-blank in the
                        line.
 */
class ScrollFirstScreenLineStartActionTest : VimTestCase() {
  fun `test scroll current line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  fun `test scroll current line to top of screen and move to first non-blank`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(0, 19, 0)
    typeText(StringHelper.parseKeys("z<CR>"))
    assertPosition(19, 4)
    assertVisibleArea(19, 53)
  }

  fun `test scroll current line to top of screen minus scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(9, 43)
  }

  fun `test scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("100z<CR>"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test scrolls count line to top of screen minus scrolloff`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  fun `test invalid count scrolls last line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("1000z<CR>"))
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  fun `test scroll current line to top of screen ignoring scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(StringHelper.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }
}