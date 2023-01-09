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
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z.*
z.                      Redraw, line [count] at center of window (default
                        cursor line).  Put cursor at first non-blank in the
                        line.
 */
class ScrollMiddleScreenLineStartActionTest : VimTestCase() {
  fun `test scrolls current line to middle of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(injector.parser.parseKeys("z."))
    assertPosition(45, 0)
    assertVisibleArea(28, 62)
  }

  fun `test scrolls current line to middle of screen and moves cursor to first non-blank`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 45, 14)
    typeText(injector.parser.parseKeys("z."))
    assertPosition(45, 4)
    assertVisibleArea(28, 62)
  }

  fun `test scrolls count line to the middle of the screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(injector.parser.parseKeys("100z."))
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  fun `test scrolls count line ignoring scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljump, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(injector.parser.parseKeys("100z."))
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  fun `test scrolls correctly when count line is in first half of first page`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText(injector.parser.parseKeys("10z."))
    assertPosition(9, 0)
    assertVisibleArea(0, 34)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  fun `test scrolls last line of file correctly`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("175z."))
    assertPosition(174, 0)
    assertVisibleArea(146, 175)
  }
}
