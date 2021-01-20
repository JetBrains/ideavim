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
                                                       *z+*
z+                      Without [count]: Redraw with the line just below the
                        window at the top of the window.  Put the cursor in
                        that line, at the first non-blank in the line.
                        With [count]: just like "z<CR>".
 */
class ScrollFirstScreenLinePageStartActionTest : VimTestCase() {
  fun `test scrolls first line on next page to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  fun `test scrolls to first non-blank in line`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(parseKeys("z+"))
    assertPosition(35, 4)
    assertVisibleArea(35, 69)
  }

  fun `test scrolls first line on next page to scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(25, 59)
  }

  fun `test scrolls first line on next page ignores scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  fun `test count z+ scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("100z+"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test count z+ scrolls count line to top of screen plus scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("100z+"))
    assertPosition(99, 0)
    assertVisibleArea(89, 123)
  }

  @VimBehaviorDiffers(description = "Requires virtual space support")
  fun `test scroll on penultimate page`() {
    configureByPages(5)
    setPositionAndScroll(130, 145)
    typeText(parseKeys("z+"))
    assertPosition(165, 0)
    assertVisibleArea(146, 175)
  }
}