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
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

/*
<S-Down>        or                             *<S-Down>* *<kPageDown>*
<PageDown>      or                             *<PageDown>* *CTRL-F*
CTRL-F                  Scroll window [count] pages Forwards (downwards) in
                        the buffer.  See also 'startofline' option.
                        When there is only one window the 'window' option
                        might be used.

<S-Down>        move window one page down      *i_<S-Down>*
<PageDown>      move window one page down      *i_<PageDown>*
 */
class ScrollPageDownActionTest : VimTestCase() {
  fun `test scroll single page down with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("<S-Down>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll single page down with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("<PageDown>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll single page down with CTRL-F`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("<C-F>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down in insert mode with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("i", "<S-Down>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down in insert mode with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("i", "<PageDown>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll count pages down with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("3<S-Down>"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test scroll count pages down with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("3<PageDown>"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test scroll count pages down with CTRL-F`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("3<C-F>"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test scroll page down moves cursor to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("<C-F>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down in insert mode moves cursor`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("i", "<S-Down>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down moves cursor with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("<C-F>"))
    assertPosition(43, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down in insert mode moves cursor with scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(parseKeys("i", "<S-Down>"))
    assertPosition(43, 0)
    assertVisibleArea(33, 67)
  }

  fun `test scroll page down ignores scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("<C-F>"))
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @VimBehaviorDiffers(description = "IntelliJ does not have virtual space enabled by default")
  fun `test scroll page down on last page moves cursor to end of file`() {
    configureByPages(5)
    setPositionAndScroll(145, 150)
    typeText(parseKeys("<C-F>"))
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  fun `test scroll page down on penultimate page`() {
    configureByPages(5)
    setPositionAndScroll(110, 130)
    typeText(parseKeys("<C-F>"))
    assertPosition(143, 0)
    assertVisibleArea(143, 175)
  }

  fun `test scroll page down on last line causes beep`() {
    configureByPages(5)
    setPositionAndScroll(146, 175)
    typeText(parseKeys("<C-F>"))
    assertTrue(VimPlugin.isError())
  }
}