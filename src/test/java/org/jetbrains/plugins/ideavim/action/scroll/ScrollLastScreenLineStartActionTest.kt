/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/*
                                                       *z-*
z-                      Redraw, line [count] at bottom of window (default
                        cursor line).  Put cursor at first non-blank in the
                        line.
 */
class ScrollLastScreenLineStartActionTest : VimTestCase() {
  @Test
  fun `test scroll current line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText("z-")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @Test
  fun `test scroll current line to bottom of screen and move cursor to first non-blank`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(40, 60, 14)
    typeText("z-")
    assertPosition(60, 4)
    assertVisibleArea(26, 60)
  }

  @Test
  fun `test scroll current line to bottom of screen minus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(40, 60)
    typeText("z-")
    assertPosition(60, 0)
    assertVisibleArea(36, 70)
  }

  @Test
  fun `test scrolls count line to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(40, 60)
    typeText("100z-")
    assertPosition(99, 0)
    assertVisibleArea(65, 99)
  }

  @Test
  fun `test scrolls count line to bottom of screen minus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(40, 60)
    typeText("100z-")
    assertPosition(99, 0)
    assertVisibleArea(75, 109)
  }

  @Test
  fun `test scrolls current line to bottom of screen ignoring scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(40, 60)
    typeText("z-")
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @Test
  fun `test scrolls correctly when less than a page to scroll`() {
    configureByPages(5)
    setPositionAndScroll(5, 15)
    typeText("z-")
    assertPosition(15, 0)
    assertVisibleArea(0, 34)
  }
}
