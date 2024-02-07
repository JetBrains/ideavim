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
                                                       *zz*
zz                      Like "z.", but leave the cursor in the same column.
                        Careful: If caps-lock is on, this command becomes
                        "ZZ": write buffer and exit!
 */
class ScrollMiddleScreenLineActionTest : VimTestCase() {
  @Test
  fun `test scrolls current line to middle of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText("zz")
    assertPosition(45, 0)
    assertVisibleArea(28, 62)
  }

  @Test
  fun `test scrolls current line to middle of screen and keeps cursor in the same column`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(40, 45, 14)
    typeText("zz")
    assertPosition(45, 14)
    assertVisibleArea(28, 62)
  }

  @Test
  fun `test scrolls count line to the middle of the screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText("100zz")
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  @Test
  fun `test scrolls count line ignoring scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(40, 45)
    typeText("100zz")
    assertPosition(99, 0)
    assertVisibleArea(82, 116)
  }

  @Test
  fun `test scrolls correctly when count line is in first half of first page`() {
    configureByPages(5)
    setPositionAndScroll(40, 45)
    typeText("10zz")
    assertPosition(9, 0)
    assertVisibleArea(0, 34)
  }

  @VimBehaviorDiffers(description = "Virtual space at end of file")
  @Test
  fun `test scrolls last line of file correctly`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("175zz")
    assertPosition(174, 0)
    assertVisibleArea(146, 175)
  }

  @Test
  fun `test scrolls last line of file correctly with full virtual space`() {
    configureByPages(5)
    setEditorVirtualSpace()
    setPositionAndScroll(0, 0)
    typeText("175zz")
    assertPosition(174, 0)
    assertVisibleArea(157, 175)
  }
}
