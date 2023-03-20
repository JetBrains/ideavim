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
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

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
  @Test
  fun `test scrolls last line on previous page to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(99, 119)
    typeText("z^")
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  @Test
  fun `test scrolls to first non-blank in line`() {
    configureByLines(200, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(99, 119)
    typeText("z^")
    assertPosition(98, 4)
    assertVisibleArea(64, 98)
  }

  @Test
  fun `test scrolls last line on previous page to scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(99, 119)
    typeText("z^")
    assertPosition(98, 0)
    assertVisibleArea(74, 108)
  }

  @Test
  fun `test scrolls last line on previous page ignores scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(99, 119)
    typeText("z^")
    assertPosition(98, 0)
    assertVisibleArea(64, 98)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test count z^ puts count line at bottom of screen then scrolls back a page`() {
    configureByPages(5)
    setPositionAndScroll(140, 150)
    typeText("100z^")
    // Put 100 at the bottom of the page. Top is 66. Scroll back a page so 66 is at bottom of page
    assertPosition(65, 0)
    assertVisibleArea(31, 65)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test z^ on first page puts cursor on first line 1`() {
    configureByLines(50, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(0, 25)
    typeText("z^")
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  @Test
  fun `test z^ on first page puts cursor on first line 2`() {
    configureByLines(50, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(0, 6)
    typeText("z^")
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  @Test
  fun `test z^ on first page ignores scrolloff and puts cursor on last line of previous page`() {
    configureByLines(50, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 6)
    typeText("z^")
    assertPosition(0, 4)
    assertVisibleArea(0, 34)
  }

  @Test
  fun `test z^ on second page puts cursor on previous last line`() {
    configureByLines(50, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(19, 39)
    typeText("z^")
    assertPosition(18, 4)
    assertVisibleArea(0, 34)
  }
}
