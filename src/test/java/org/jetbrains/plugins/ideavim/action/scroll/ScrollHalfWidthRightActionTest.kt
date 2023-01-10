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
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/*
For the following four commands the cursor follows the screen.  If the
character that the cursor is on is moved off the screen, the cursor is moved
to the closest character that is on the screen.  The value of 'sidescroll' is
not used.

                                                       *zH*
zH                      Move the view on the text half a screenwidth to the
                        left, thus scroll the text half a screenwidth to the
                        right.  This only works when 'wrap' is off.

[count] is used but undocumented.
 */
class ScrollHalfWidthRightActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("200|" + "ze" + "zH"))
    assertPosition(0, 159)
    assertVisibleLineBounds(0, 80, 159)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll keeps cursor in place if already in scrolled area`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "zs" + "zH"))
    assertPosition(0, 99)
    // Scroll right 40 characters 99 -> 59
    assertVisibleLineBounds(0, 59, 138)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll moves cursor if moves off screen`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("100|" + "ze" + "zH"))
    assertPosition(0, 79)
    assertVisibleLineBounds(0, 0, 79)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll count half page widths`() {
    configureByColumns(400)
    typeText(injector.parser.parseKeys("350|" + "ze" + "3zH"))
    assertPosition(0, 229)
    assertVisibleLineBounds(0, 150, 229)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width with sidescrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloffName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("150|" + "ze" + "zH"))
    assertPosition(0, 109)
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width ignores sidescroll`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrollName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("200|" + "ze" + "zH"))
    assertPosition(0, 159)
    assertVisibleLineBounds(0, 80, 159)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll at start of line does nothing`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("zH"))
    assertPosition(0, 0)
    assertVisibleLineBounds(0, 0, 79)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll near start of line does nothing`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("10|" + "zH"))
    assertPosition(0, 9)
    assertVisibleLineBounds(0, 0, 79)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll includes inlay visual column in half page width`() {
    configureByColumns(200)
    addInlay(180, true, 5)
    typeText(injector.parser.parseKeys("190|" + "ze" + "zH"))
    // The inlay is included in the count of scrolled visual columns
    assertPosition(0, 150)
    assertVisibleLineBounds(0, 71, 150)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll with inlay and cursor in scrolled area`() {
    configureByColumns(200)
    addInlay(180, true, 5)
    typeText(injector.parser.parseKeys("170|" + "ze" + "zH"))
    // The inlay is after the cursor, and does not affect scrolling
    assertPosition(0, 129)
    assertVisibleLineBounds(0, 50, 129)
  }
}
