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
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z^*
z^                      Without [count]: Redraw with the line just above the
                        window at the bottom of the window.  Put the cursor in
                        that line, at the first non-blank in the line.
                        With [count]: First scroll the text to put the [count]
                        line at the bottom of the window, then redraw with the
                        line which is now at the top of the window at the
                        bottom of the window.  Put the cursor in that line, at
                        the first non-blank in the line.
 */
class ScrollLastScreenLinePageStartActionTest : VimTestCase() {
  fun `test scrolls last line on previous page to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  fun `test scrolls to first non-blank in line`() {
    configureByLines(200, "    I found it in a legendary land")
    setPositionAndScroll(99, 119)
    typeText(parseKeys("z^"))
    assertPosition(98, 4)
    assertVisibleArea(64, 98)
  }

  fun `test scrolls last line on previous page to scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(74, 108)
  }

  fun `test scrolls last line on previous page ignores scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  fun `test count z^ puts count line at bottom of screen then scrolls back a page`() {
    configureByPages(5)
    setPositionAndScroll(140, 150)
    typeText(parseKeys("100z^"))
    // Put 100 at the bottom of the page. Top is 66. Scroll back a page so 66 is at bottom of page
    assertPosition(65, 0)
    assertVisibleArea(31, 65)
  }

  fun `test z^ on first page puts cursor on first line 1`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 25)
    typeText(parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on first page puts cursor on first line 2`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 6)
    typeText(parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on first page ignores scrolloff and puts cursor on last line of previous page`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 6)
    typeText(parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on second page puts cursor on previous last line`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(19, 39)
    typeText(parseKeys("z^"))
    assertPosition(18, 4)
    assertVisibleArea(0, 34)
  }
}