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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import junit.framework.Assert
import org.jetbrains.plugins.ideavim.VimTestCase

/*
<S-Up>          or                                     *<S-Up>* *<kPageUp>*
<PageUp>        or                                     *<PageUp>* *CTRL-B*
CTRL-B                  Scroll window [count] pages Backwards (upwards) in the
                        buffer.  See also 'startofline' option.
                        When there is only one window the 'window' option
                        might be used.

<S-Up>          move window one page up        *i_<S-Up>*
<PageUp>        move window one page up        *i_<PageUp>*
 */
class ScrollPageUpActionTest : VimTestCase() {
  fun `test scroll single page up with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<S-Up>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll single page up with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<PageUp>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll single page up with CTRL-B`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<C-B>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up in insert mode with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("i", "<S-Up>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up in insert mode with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("i", "<PageUp>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll count pages up with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("3<S-Up>"))
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  fun `test scroll count pages up with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("3<PageUp>"))
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  fun `test scroll count pages up with CTRL-B`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("3<C-B>"))
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  fun `test scroll page up moves cursor to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<C-B>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up in insert mode moves cursor`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("i", "<S-Up>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up moves cursor with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<C-B>"))
    assertPosition(120, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up in insert mode cursor with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("i", "<S-Up>"))
    assertPosition(120, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up ignores scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText(parseKeys("<C-B>"))
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  fun `test scroll page up on first page does not move`() {
    configureByPages(5)
    setPositionAndScroll(0, 25)
    typeText(parseKeys("<C-B>"))
    assertPosition(25, 0)
    assertVisibleArea(0, 34)
  }

  fun `test scroll page up on first page causes beep`() {
    configureByPages(5)
    setPositionAndScroll(0, 25)
    typeText(parseKeys("<C-B>"))
    Assert.assertTrue(VimPlugin.isError())
  }

  fun `test scroll page up on second page moves cursor to previous top`() {
    configureByPages(5)
    setPositionAndScroll(10, 35)
    typeText(parseKeys("<C-B>"))
    assertPosition(11, 0)
    assertVisibleArea(0, 34)
  }
}