/*
 * Copyright 2003-2022 The IdeaVim authors
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
                                                       *z<CR>*
z<CR>                   Redraw, line [count] at top of window (default
                        cursor line).  Put cursor at first non-blank in the
                        line.
 */
class ScrollFirstScreenLineStartActionTest : VimTestCase() {
  fun `test scroll current line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  fun `test scroll current line to top of screen and move to first non-blank`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(0, 19, 0)
    typeText(injector.parser.parseKeys("z<CR>"))
    assertPosition(19, 4)
    assertVisibleArea(19, 53)
  }

  fun `test scroll current line to top of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(9, 43)
  }

  fun `test scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("100z<CR>"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test scrolls count line to top of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  fun `test invalid count scrolls last line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("1000z<CR>"))
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  fun `test scroll current line to top of screen ignoring scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText(injector.parser.parseKeys("z<CR>"))
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }
}
