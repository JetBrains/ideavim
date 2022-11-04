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
                                                       *CTRL-U*
CTRL-U                  Scroll window Upwards in the buffer.  The number of
                        lines comes from the 'scroll' option (default: half a
                        screen).  If [count] given, first set the 'scroll'
                        option to [count].  The cursor is moved the same
                        number of lines up in the file (if possible; when
                        lines wrap and when hitting the end of the file there
                        may be a difference).  When the cursor is on the first
                        line of the buffer nothing happens and a beep is
                        produced.  See also 'startofline' option.
 */
class ScrollHalfPageUpActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll half window upwards keeps cursor on same relative line`() {
    configureByPages(5)
    setPositionAndScroll(50, 60)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(43, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll upwards on first line causes beep`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(0, 0)
    assertVisibleArea(0, 34)
    assertTrue(VimPlugin.isError())
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll upwards in first half of first page moves to first line`() {
    configureByPages(5)
    setPositionAndScroll(5, 10)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(0, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll upwards in first half of first page moves to first line with scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(5, 15)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(0, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll count lines upwards`() {
    configureByPages(5)
    setPositionAndScroll(50, 53)
    typeText(injector.parser.parseKeys("10<C-U>"))
    assertPosition(43, 0)
    assertVisibleArea(40, 74)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll count modifies scroll option`() {
    configureByPages(5)
    setPositionAndScroll(50, 53)
    typeText(injector.parser.parseKeys("10<C-U>"))
    assertEquals((VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.scrollName) as VimInt).value, 10)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll upwards uses scroll option`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrollName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(50, 53)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(43, 0)
    assertVisibleArea(40, 74)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test count scroll upwards is limited to a single page`() {
    configureByPages(5)
    setPositionAndScroll(100, 134)
    typeText(injector.parser.parseKeys("50<C-U>"))
    assertPosition(99, 0)
    assertVisibleArea(65, 99)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll up puts cursor on first non-blank column`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(50, 60, 14)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(43, 4)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun`test scroll upwards keeps same column with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(50, 60, 14)
    typeText(injector.parser.parseKeys("<C-U>"))
    assertPosition(43, 14)
    assertVisibleArea(33, 67)
  }
}
