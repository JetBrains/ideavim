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
                                                       *CTRL-E*
CTRL-E                  Scroll window [count] lines downwards in the buffer.
                        The text moves upwards on the screen.
                        Mnemonic: Extra lines.
 */
class ScrollLineDownActionTest : VimTestCase() {
  fun `test scroll single line down`() {
    configureByPages(5)
    setPositionAndScroll(0, 34)
    typeText(parseKeys("<C-E>"))
    assertPosition(34, 0)
    assertVisibleArea(1, 35)
  }

  fun `test scroll line down will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("<C-E>"))
    assertPosition(1, 0)
    assertVisibleArea(1, 35)
  }

  fun `test scroll count lines down`() {
    configureByPages(5)
    setPositionAndScroll(0, 34)
    typeText(parseKeys("10<C-E>"))
    assertPosition(34, 0)
    assertVisibleArea(10, 44)
  }

  fun `test scroll count lines down will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("10<C-E>"))
    assertPosition(10, 0)
    assertVisibleArea(10, 44)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of the file, IntelliJ (by default) does not")
  fun `test too many lines down stops at last line`() {
    configureByPages(5) // 5 * 35 = 175
    setPositionAndScroll(100, 100)
    typeText(parseKeys("100<C-E>"))

    // TODO: Enforce virtual space
    // Vim will put the caret on line 174, and put that line at the top of the screen
    // See com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToTopOfScreen
    assertPosition(146, 0)
    assertVisibleArea(146, 175)
  }

  fun `test scroll down uses scrolloff and moves cursor`() {
    OptionsManager.scrolloff.set(10)
    configureByPages(5)
    setPositionAndScroll(20, 30)
    typeText(parseKeys("<C-E>"))
    assertPosition(31, 0)
    assertVisibleArea(21, 55)
  }

  fun `test scroll down is not affected by scrolljump`() {
    OptionsManager.scrolljump.set(10)
    configureByPages(5)
    setPositionAndScroll(20, 20)
    typeText(parseKeys("<C-E>"))
    assertPosition(21, 0)
    assertVisibleArea(21, 55)
  }

  fun `test scroll down in visual mode`() {
    configureByPages(5)
    setPositionAndScroll(20, 30)
    typeText(parseKeys("Vjjjj", "<C-E>"))
    assertVisibleArea(21, 55)
  }
}