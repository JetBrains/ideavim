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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/*
                                                       *z+*
z+                      Without [count]: Redraw with the line just below the
                        window at the top of the window.  Put the cursor in
                        that line, at the first non-blank in the line.
                        With [count]: just like "z<CR>".
 */
class ScrollFirstScreenLinePageStartActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scrolls first line on next page to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scrolls to first non-blank in line`() {
    configureByLines(100, "    I found it in a legendary land")
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("z+"))
    assertPosition(35, 4)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scrolls first line on next page to scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(25, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scrolls first line on next page ignores scrolljump`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolljumpName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("z+"))
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  fun `test count z+ scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("100z+"))
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  fun `test count z+ scrolls count line to top of screen plus scrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.scrolloffName, VimInt(10))
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText(injector.parser.parseKeys("100z+"))
    assertPosition(99, 0)
    assertVisibleArea(89, 123)
  }

  @VimBehaviorDiffers(description = "Requires virtual space support")
  fun `test scroll on penultimate page`() {
    configureByPages(5)
    setPositionAndScroll(130, 145)
    typeText(injector.parser.parseKeys("z+"))
    assertPosition(165, 0)
    assertVisibleArea(146, 175)
  }
}
