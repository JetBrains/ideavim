/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/*
                                                       *z+*
z+                      Without [count]: Redraw with the line just below the
                        window at the top of the window.  Put the cursor in
                        that line, at the first non-blank in the line.
                        With [count]: just like "z<CR>".
 */
class ScrollFirstScreenLinePageStartActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolls first line on next page to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText("z+")
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolls to first non-blank in line`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(0, 20)
    typeText("z+")
    assertPosition(35, 4)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolls first line on next page to scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("z+")
    assertPosition(35, 0)
    assertVisibleArea(25, 59)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scrolls first line on next page ignores scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(0, 20)
    typeText("z+")
    assertPosition(35, 0)
    assertVisibleArea(35, 69)
  }

  @Test
  fun `test count z+ scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText("100z+")
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  @Test
  fun `test count z+ scrolls count line to top of screen plus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("100z+")
    assertPosition(99, 0)
    assertVisibleArea(89, 123)
  }

  @VimBehaviorDiffers(description = "Requires virtual space support")
  @Test
  fun `test scroll on penultimate page`() {
    configureByPages(5)
    setPositionAndScroll(130, 145)
    typeText("z+")
    assertPosition(165, 0)
    assertVisibleArea(146, 175)
  }
}
