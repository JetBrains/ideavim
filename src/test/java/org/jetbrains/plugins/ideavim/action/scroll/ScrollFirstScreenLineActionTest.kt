/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/*
                                                       *zt*
zt                      Like "z<CR>", but leave the cursor in the same
                        column.
 */
class ScrollFirstScreenLineActionTest : VimTestCase() {
  @Test
  fun `test scroll current line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText("zt")
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  @Test
  fun `test scroll current line to top of screen and leave cursor in current column`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(0, 19, 14)
    typeText("zt")
    assertPosition(19, 14)
    assertVisibleArea(19, 53)
  }

  @Test
  fun `test scroll current line to top of screen minus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 19)
    typeText("zt")
    assertPosition(19, 0)
    assertVisibleArea(9, 43)
  }

  @Test
  fun `test scrolls count line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText("100zt")
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  @Test
  fun `test scrolls count line to top of screen minus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(0, 19)
    typeText("zt")
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  @Test
  fun `test invalid count scrolls last line to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 19)
    typeText("1000zt")
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  @Test
  fun `test scroll current line to top of screen ignoring scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(0, 19)
    typeText("zt")
    assertPosition(19, 0)
    assertVisibleArea(19, 53)
  }
}
