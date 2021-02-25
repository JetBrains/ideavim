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

package org.jetbrains.plugins.ideavim.action.motion.screen

import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionFirstScreenLineActionTest : VimTestCase() {
  fun `test move caret to first line of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(parseKeys("H"))
    assertPosition(0, 4)
  }

  fun `test move caret to first line of screen further down file`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText(parseKeys("H"))
    assertPosition(40, 4)
  }

  fun `test move caret to count line from top of screen`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(parseKeys("10H"))
    assertPosition(9, 4)
  }

  fun `test move caret to count line from top of screen further down file`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText(parseKeys("10H"))
    assertPosition(49, 4)
  }

  fun `test move caret to too large count line from top of screen`() {
    assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60)
    typeText(parseKeys("100H"))
    assertPosition(74, 4)
  }

  fun `test move caret ignores scrolloff when top of screen is top of file`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(parseKeys("H"))
    assertPosition(0, 4)
  }

  fun `test move caret applies scrolloff when top of screen is not top of file`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(1, 20)
    typeText(parseKeys("H"))
    assertPosition(11, 4)
  }

  fun `test move caret applies scrolloff when top of screen is not top of file 2`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40)
    typeText(parseKeys("H"))
    assertPosition(30, 4)
  }

  fun `test move caret to first screen line with count and scrolloff at top of file`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(parseKeys("5H"))
    assertPosition(4, 4)
  }

  fun `test move caret to first screen line with count and scrolloff not at top of file`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40)
    typeText(parseKeys("5H"))
    assertPosition(30, 4)
  }

  fun `test operator pending acts to first screen line`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText(parseKeys("dH"))
    assertPosition(20, 4)
    assertLineCount(79)
  }

  fun `test operator pending acts on count line from top of screen`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText(parseKeys("d5H"))
    assertPosition(24, 4)
  }

  fun `test operator pending acts to first screen line with nostartofline`() {
    OptionsManager.startofline.reset()
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText(parseKeys("dH"))
    assertPosition(20, 10)
  }

  fun `test operator pending acts on count line from top of screen with nostartofline`() {
    OptionsManager.startofline.reset()
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40, 10)
    typeText(parseKeys("d5H"))
    assertPosition(24, 10)
  }

  fun `test operator pending acts to first screen line and then scrolls scrolloff`() {
    OptionsManager.scrolloff.set(10)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(20, 40)
    typeText(parseKeys("dH"))
    assertPosition(20, 4)
    assertVisibleArea(10, 44)
  }

  fun `test move caret to same column with nostartofline`() {
    OptionsManager.startofline.reset()
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 20, 10)
    typeText(parseKeys("H"))
    assertPosition(0, 10)
  }

  fun `test move caret to end of shorter line with nostartofline`() {
    OptionsManager.startofline.reset()
    configureByLines(70, "    I found it in a legendary land")
    setPositionAndScroll(10, 30, 10)
    typeText(parseKeys("A", " extra text", "<Esc>"))
    typeText(parseKeys("H"))
    assertPosition(10, 33)
  }

  fun `test move caret to first line of screen with inlays`() {
    // We're not scrolling, so inlays don't affect anything. Just place the caret on the first visible line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 5, 5), true, 10)
    setPositionAndScroll(0, 20, 10)
    typeText(parseKeys("H"))
    assertPosition(0, 4)
  }

  fun `test keep caret on screen when count is greater than visible lines plus inlays`() {
    assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 5, 5), true, 10)
    setPositionAndScroll(0, 20, 10)
    // Should move to the 34th visible line. We have space for 35 lines, but we're using some of that for inlays
    typeText(parseKeys("34H"))
    assertPosition(24, 4)
  }
}
