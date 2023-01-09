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
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *zb*
zb                      Like "z-", but leave the cursor in the same column.
 */
class ScrollLastScreenLineActionTest : VimTestCase() {
  fun `test scroll current line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen and leave cursor in current column`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(40, 60, 14)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(60, 14)
    assertVisibleArea(26, 60)
  }

  fun `test scroll current line to bottom of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(36, 70)
  }

  fun `test scrolls count line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("100zb"))
    assertPosition(99, 0)
    assertVisibleArea(65, 99)
  }

  fun `test scrolls count line to bottom of screen minus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("100zb"))
    assertPosition(99, 0)
    assertVisibleArea(75, 109)
  }

  fun `test scrolls current line to bottom of screen ignoring scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljump, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  fun `test scrolls correctly when less than a page to scroll`() {
    configureByPages(5)
    setPositionAndScroll(5, 15)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(15, 0)
    assertVisibleArea(0, 34)
  }

  fun `test scrolls last line to bottom of screen with virtual space`() {
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(80, 99, 4)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(99, 4)
    assertVisibleArea(65, 99)
  }

  fun `test scrolls last line to bottom of screen with virtual space when caret less than scrolloff from bottom`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloff, VimInt(10))
    configureByLines(100, "    I found it in a legendary land")
    setEditorVirtualSpace()
    setPositionAndScroll(80, 97, 4)
    typeText(injector.parser.parseKeys("zb"))
    assertPosition(97, 4)
    assertVisibleArea(65, 99)
  }
}
