/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
                                                       *CTRL-D*
CTRL-D                  Scroll window Downwards in the buffer.  The number of
                        lines comes from the 'scroll' option (default: half a
                        screen).  If [count] given, first set 'scroll' option
                        to [count].  The cursor is moved the same number of
                        lines down in the file (if possible; when lines wrap
                        and when hitting the end of the file there may be a
                        difference).  When the cursor is on the last line of
                        the buffer nothing happens and a beep is produced.
                        See also 'startofline' option.
 */
class ScrollHalfPageDownActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll half window downwards keeps cursor on same relative line`() {
    configureByPages(5)
    setPositionAndScroll(20, 25)

    typeText("<C-D>")
    assertPosition(42, 0)
    assertVisibleArea(37, 71)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards on last line causes beep`() {
    configureByPages(5)
    setPositionAndScroll(146, 175)

    typeText("<C-D>")
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
    assertTrue(injector.messages.isError())
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards in bottom half of last page moves caret to the last line without scrolling`() {
    configureByPages(5)
    setPositionAndScroll(140, 165)

    typeText("<C-D>")
    assertPosition(175, 0)
    assertVisibleArea(141, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards in bottom half of last page moves caret to the last line with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(140, 164)

    typeText("<C-D>")
    assertPosition(175, 0)
    assertVisibleArea(141, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards at end of file with existing virtual space moves caret without scrolling window`() {
    configureByPages(5)
    setPositionAndScroll(146, 165) // 146 at top line means bottom line is 181 (out of 175)

    typeText("<C-D>")
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards in top half of last page moves cursor down half a page`() {
    configureByPages(5)
    setPositionAndScroll(146, 150)

    typeText("<C-D>")
    assertPosition(167, 0)
    assertVisibleArea(146, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count lines downwards`() {
    configureByPages(5)
    setPositionAndScroll(100, 130)

    typeText("10<C-D>")
    assertPosition(140, 0)
    assertVisibleArea(110, 144)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count downwards modifies scroll option`() {
    configureByPages(5)
    setPositionAndScroll(100, 110)

    typeText("10<C-D>")
    assertEquals(10, options().scroll)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards uses scroll option`() {
    configureByPages(5)
    enterCommand("set scroll=10")
    setPositionAndScroll(100, 110)

    typeText("<C-D>")
    assertPosition(120, 0)
    assertVisibleArea(110, 144)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test count scroll downwards is limited to single page`() {
    configureByPages(5)
    setPositionAndScroll(100, 110)

    typeText("1000<C-D>")
    assertPosition(145, 0)
    assertVisibleArea(135, 169)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards puts cursor on first non-blank column`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(20, 25, 14)

    typeText("<C-D>")
    assertPosition(42, 4)
    assertVisibleArea(37, 71)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll downwards keeps same column with nostartofline`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 25, 14)

    typeText("<C-D>")
    assertPosition(42, 14)
    assertVisibleArea(37, 71)
  }
}
