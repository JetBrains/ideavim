/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
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
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(34, 0)
    assertVisibleArea(1, 35)
  }

  fun `test scroll line down will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(1, 0)
    assertVisibleArea(1, 35)
  }

  fun `test scroll line down will maintain current column at start of line with sidescrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(30, 50, 5)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(50, 5)
    assertTopLogicalLine(31)
  }

  fun `test scroll count lines down`() {
    configureByPages(5)
    setPositionAndScroll(0, 34)
    typeText(injector.parser.parseKeys("10<C-E>"))
    assertPosition(34, 0)
    assertVisibleArea(10, 44)
  }

  fun `test scroll count lines down will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("10<C-E>"))
    assertPosition(10, 0)
    assertVisibleArea(10, 44)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at the end of the file, IntelliJ (by default) does not")
  fun `test too many lines down stops at last line`() {
    configureByPages(5) // 5 * 35 = 175
    setPositionAndScroll(100, 100)
    typeText(injector.parser.parseKeys("100<C-E>"))

    // TODO: Enforce virtual space
    // Vim will put the caret on line 174, and put that line at the top of the screen
    // See com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToTopOfScreen
    assertPosition(146, 0)
    assertVisibleArea(146, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll down uses scrolloff and moves cursor`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(20, 30)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(31, 0)
    assertVisibleArea(21, 55)
  }

  fun `test scroll down is not affected by scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljump, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(20, 20)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(21, 0)
    assertVisibleArea(21, 55)
  }

  fun `test scroll down in visual mode`() {
    configureByPages(5)
    setPositionAndScroll(20, 30)
    typeText(injector.parser.parseKeys("Vjjjj" + "<C-E>"))
    assertVisibleArea(21, 55)
  }

  fun `test scroll last line down at end of file with virtual space`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(75, 99, 4)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(99, 4)
    assertVisibleArea(76, 99)
  }

  fun `test scroll line down at end of file with virtual space and scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(75, 95, 4)
    typeText(injector.parser.parseKeys("<C-E>"))
    assertPosition(95, 4)
    assertVisibleArea(76, 99)
  }
}
