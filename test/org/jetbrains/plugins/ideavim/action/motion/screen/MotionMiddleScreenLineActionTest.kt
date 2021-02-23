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
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionMiddleScreenLineActionTest : VimTestCase() {
  fun `test move caret to middle line of full screen with odd number of lines`() {
    assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(17, 4)
  }

  fun `test move caret to middle line of full screen with even number of lines`() {
    configureByLines(50, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(17, 4)
  }

  fun `test move caret to middle line of scrolled down screen`() {
    assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(50, 50)
    typeText(parseKeys("M"))
    assertPosition(67, 4)
  }

  fun `test move caret to middle line when file is shorter than screen`() {
    assertEquals(35, screenHeight)
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(10, 4)
  }

  fun `test move caret to middle line when file is shorter than screen 2`() {
    assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(11, 4)
  }

  fun `test move caret to middle line when file is shorter than screen 3`() {
    configureByLines(20, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(10, 4)
  }

  fun `test move caret to middle line when file is shorter than screen 4`() {
    configureByLines(21, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(11, 4)
  }

  fun `test move caret to middle line of visible lines with virtual space enabled`() {
    configureByLines(30, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(20, 20)
    typeText(parseKeys("M"))
    assertPosition(25, 4)
  }

  fun `test move caret to middle line of screen with block inlays above`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 5, 5), true, 5)
    typeText(parseKeys("M"))
    assertPosition(12, 4)
  }

  fun `test move caret to middle line of screen with block inlays below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 25, 5), true, 5)
    typeText(parseKeys("M"))
    assertPosition(17, 4)
  }

  fun `test move caret to middle line of screen with block inlays above and below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 5, 5), true, 5)
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 25, 5), true, 5)
    typeText(parseKeys("M"))
    assertPosition(12, 4)
  }

  fun `test move caret to middle line of screen with block inlays and a file shorter than the screen`() {
    assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    addBlockInlay(EditorHelper.getOffset(myFixture.editor, 5, 5), true, 5)
    setPositionAndScroll(0, 0)
    typeText(parseKeys("M"))
    assertPosition(8, 4)
  }
}