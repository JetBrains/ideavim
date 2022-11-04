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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z^*
z^                      Without [count]: Redraw with the line just above the
                        window at the bottom of the window.  Put the cursor in
                        that line, at the first non-blank in the line.
                        With [count]: First scroll the text to put the [count]
                        line at the bottom of the window, then redraw with the
                        line which is now at the top of the window at the
                        bottom of the window.  Put the cursor in that line, at
                        the first non-blank in the line.
 */
class ScrollLastScreenLinePageStartActionTest : VimTestCase() {
  fun `test scrolls last line on previous page to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  fun `test scrolls to first non-blank in line`() {
    configureByLines(200, "    I found it in a legendary land")
    setPositionAndScroll(99, 119)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(98, 4)
    assertVisibleArea(64, 98)
  }

  fun `test scrolls last line on previous page to scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(74, 108)
  }

  fun `test scrolls last line on previous page ignores scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test count z^ puts count line at bottom of screen then scrolls back a page`() {
    configureByPages(5)
    setPositionAndScroll(140, 150)
    typeText(injector.parser.parseKeys("100z^"))
    // Put 100 at the bottom of the page. Top is 66. Scroll back a page so 66 is at bottom of page
    assertPosition(65, 0)
    assertVisibleArea(31, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test z^ on first page puts cursor on first line 1`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 25)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on first page puts cursor on first line 2`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 6)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on first page ignores scrolloff and puts cursor on last line of previous page`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(0, 6)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  fun `test z^ on second page puts cursor on previous last line`() {
    configureByLines(50, "    I found it in a legendary land")
    setPositionAndScroll(19, 39)
    typeText(injector.parser.parseKeys("z^"))
    assertPosition(18, 4)
    assertVisibleArea(0, 34)
  }
}
