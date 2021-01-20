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
                                                       *CTRL-Y*
CTRL-Y                  Scroll window [count] lines upwards in the buffer.
                        The text moves downwards on the screen.
                        Note: When using the MS-Windows key bindings CTRL-Y is
                        remapped to redo.
 */
class ScrollLineUpActionTest : VimTestCase() {
  fun `test scroll single line up`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText(parseKeys("<C-Y>"))
    assertPosition(29, 0)
    assertVisibleArea(28, 62)
  }

  fun `test scroll line up will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(29, 63)
    typeText(parseKeys("<C-Y>"))
    assertPosition(62, 0)
    assertVisibleArea(28, 62)
  }

  fun `test scroll count lines up`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText(parseKeys("10<C-Y>"))
    assertPosition(29, 0)
    assertVisibleArea(19, 53)
  }

  fun `test scroll count lines up will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(29, 63)
    typeText(parseKeys("10<C-Y>"))
    assertPosition(53, 0)
    assertVisibleArea(19, 53)
  }

  fun `test too many lines up stops at zero`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText(parseKeys("100<C-Y>"))
    assertPosition(29, 0)
    assertVisibleArea(0, 34)
  }

  fun `test too many lines up stops at zero and keeps cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(59, 59)
    typeText(parseKeys("100<C-Y>"))
    assertPosition(34, 0)
    assertVisibleArea(0, 34)
  }

  fun `test scroll up uses scrolloff and moves cursor`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(20, 44)
    typeText(parseKeys("<C-Y>"))
    assertPosition(43, 0)
    assertVisibleArea(19, 53)
  }

  fun `test scroll up is not affected by scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(29, 63)
    typeText(parseKeys("<C-Y>"))
    assertPosition(62, 0)
    assertVisibleArea(28, 62)
  }

  fun `test scroll line up in visual mode`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText(parseKeys("Vjjjj", "<C-Y>"))
    assertVisibleArea(28, 62)
  }
}