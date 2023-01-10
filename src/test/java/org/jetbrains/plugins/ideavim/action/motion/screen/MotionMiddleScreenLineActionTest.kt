/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.screen

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.getOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionMiddleScreenLineActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of full screen with odd number of lines`() {
    assertEquals(35, screenHeight)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of full screen with even number of lines`() {
    configureByLines(50, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of scrolled down screen`() {
    assertEquals(35, screenHeight)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(50, 50)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(67, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line when file is shorter than screen`() {
    assertEquals(35, screenHeight)
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line when file is shorter than screen 2`() {
    assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line when file is shorter than screen 3`() {
    configureByLines(20, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line when file is shorter than screen 4`() {
    configureByLines(21, "    I found it in a legendary land")
    setEditorVisibleSize(screenWidth, 34)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(10, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of visible lines with virtual space enabled`() {
    configureByLines(30, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(20, 20)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(25, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to same column with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 0, 10)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(17, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to end of shorter line with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    configureByLines(70, "    I found it in a legendary land")
    setPositionAndScroll(0, 0, 10)
    typeText(injector.parser.parseKeys("A" + " extra text" + "<Esc>"))
    typeText(injector.parser.parseKeys("M"))
    assertPosition(17, 33)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test operator pending acts to middle line`() {
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 4, 10)
    typeText(injector.parser.parseKeys("dM"))
    assertPosition(4, 4)
    assertLineCount(13)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test operator pending acts to middle line with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    configureByLines(20, "    I found it in a legendary land")
    setPositionAndScroll(0, 4, 10)
    typeText(injector.parser.parseKeys("dM"))
    assertPosition(4, 10)
    assertLineCount(13)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of screen with block inlays above`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(myFixture.editor).getOffset(5, 5), true, 5)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(12, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of screen with block inlays below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(myFixture.editor).getOffset(25, 5), true, 5)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(17, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of screen with block inlays above and below`() {
    // Move the caret to the line that is closest to the middle of the screen, rather than the numerically middle line
    configureByLines(50, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(myFixture.editor).getOffset(5, 5), true, 5)
    addBlockInlay(IjVimEditor(myFixture.editor).getOffset(25, 5), true, 5)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(12, 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test move caret to middle line of screen with block inlays and a file shorter than the screen`() {
    assertEquals(35, screenHeight)
    configureByLines(21, "    I found it in a legendary land")
    addBlockInlay(IjVimEditor(myFixture.editor).getOffset(5, 5), true, 5)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("M"))
    assertPosition(8, 4)
  }
}
