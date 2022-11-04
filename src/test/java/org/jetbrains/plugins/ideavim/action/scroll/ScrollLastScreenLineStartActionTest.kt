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
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z-*
z-                      Redraw, line [count] at bottom of window (default
                        cursor line).  Put cursor at first non-blank in the
                        line.
 */
class ScrollLastScreenLineStartActionTest : VimTestCase() {
  fun `test scroll current line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("z-"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen and move cursor to first non-blank`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60, 14)
    typeText(injector.parser.parseKeys("z-"))
    assertPosition(60, 4)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("z-"))
    assertPosition(60, 0)
    assertVisibleArea(36, 70)
  }

  fun `test scrolls count line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("100z-"))
    assertPosition(99, 0)
    assertVisibleArea(65, 99)
  }

  fun `test scrolls count line to bottom of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("100z-"))
    assertPosition(99, 0)
    assertVisibleArea(75, 109)
  }

  fun `test scrolls current line to bottom of screen ignoring scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("z-"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scrolls correctly when less than a page to scroll`() {
    configureByPages(5)
    setPositionAndScroll(5, 15)
    typeText(injector.parser.parseKeys("z-"))
    assertPosition(15, 0)
    assertVisibleArea(0, 34)
  }
}
