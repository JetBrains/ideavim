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
class ScrollHalfWidthLeftActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("zL"))
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll keeps cursor in place if already in scrolled area`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("50|" + "zL"))
    assertPosition(0, 49)
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll moves cursor if moves off screen 1`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll moves cursor if moves off screen 2`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("10|" + "zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll count half page widths`() {
    configureByColumns(300)
    typeText(injector.parser.parseKeys("3zL"))
    assertPosition(0, 120)
    assertVisibleLineBounds(0, 120, 199)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width with sidescrolloff`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloffName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("zL"))
    assertPosition(0, 50)
    assertVisibleLineBounds(0, 40, 119)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll half page width ignores sidescroll`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrollName, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("zL"))
    assertPosition(0, 40)
    assertVisibleLineBounds(0, 40, 119)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at end of line")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll at end of line does not use full virtual space`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("200|" + "ze" + "zL"))
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 123, 202)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at end of line")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll near end of line does not use full virtual space`() {
    configureByColumns(200)
    typeText(injector.parser.parseKeys("190|" + "ze" + "zL"))
    assertPosition(0, 189)
    assertVisibleLineBounds(0, 123, 202)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll includes inlay visual column in half page width`() {
    configureByColumns(200)
    addInlay(20, true, 5)
    typeText(injector.parser.parseKeys("zL"))
    // The inlay is included in the count of scrolled visual columns
    assertPosition(0, 39)
    assertVisibleLineBounds(0, 39, 118)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  fun `test scroll with inlay in scrolled area and left of the cursor`() {
    configureByColumns(200)
    addInlay(20, true, 5)
    typeText(injector.parser.parseKeys("30|" + "zL"))
    // The inlay is included in the count of scrolled visual columns
    assertPosition(0, 39)
    assertVisibleLineBounds(0, 39, 118)
  }
}
