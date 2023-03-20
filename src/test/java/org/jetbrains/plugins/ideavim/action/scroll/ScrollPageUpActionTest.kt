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
<S-Up>          or                                     *<S-Up>* *<kPageUp>*
<PageUp>        or                                     *<PageUp>* *CTRL-B*
CTRL-B                  Scroll window [count] pages Backwards (upwards) in the
                        buffer.  See also 'startofline' option.
                        When there is only one window the 'window' option
                        might be used.

<S-Up>          move window one page up        *i_<S-Up>*
<PageUp>        move window one page up        *i_<PageUp>*
 */
class ScrollPageUpActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page up with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("<S-Up>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page up with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("<PageUp>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page up with CTRL-B`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("<C-B>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up in insert mode with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("i", "<S-Up>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up in insert mode with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("i", "<PageUp>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages up with S-Up`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("3<S-Up>")
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages up with PageUp`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("3<PageUp>")
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages up with CTRL-B`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("3<C-B>")
    assertPosition(64, 0)
    assertVisibleArea(30, 64)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up moves cursor to bottom of screen`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("<C-B>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up in insert mode moves cursor`() {
    configureByPages(5)
    setPositionAndScroll(129, 149)
    typeText("i", "<S-Up>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up moves cursor with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(129, 149)
    typeText("<C-B>")
    assertPosition(120, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up in insert mode cursor with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(129, 149)
    typeText("i", "<S-Up>")
    assertPosition(120, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up ignores scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(129, 149)
    typeText("<C-B>")
    assertPosition(130, 0)
    assertVisibleArea(96, 130)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up on first page does not move`() {
    configureByPages(5)
    setPositionAndScroll(0, 25)
    typeText("<C-B>")
    assertPosition(25, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up on first page causes beep`() {
    configureByPages(5)
    setPositionAndScroll(0, 25)
    typeText("<C-B>")
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up too many times causes beep`() {
    configureByPages(5)
    setPositionAndScroll(40, 65)
    typeText("20<C-B>")
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up too many times moves caret to correct location`() {
    configureByPages(5)
    setPositionAndScroll(40, 65)
    typeText("20<C-B>")
    // Essentially, move top line up a multiple of (window height minus 2) +1. Not sure where the +1 comes from, but it
    // matches Vim behaviour
    assertPosition(8, 0)
    assertVisibleArea(0, 34)
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up too many times moves caret to correct location 2`() {
    configureByPages(5)
    setPositionAndScroll(50, 65)
    typeText("20<C-B>")
    // Essentially, move top line up a multiple of (window height minus 2) +1. Not sure where the +1 comes from, but it
    // matches Vim behaviour
    assertPosition(18, 0) // Hard to explain, but matches Vim
    assertVisibleArea(0, 34)
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up too many times moves caret to correct location 3`() {
    configureByPages(5)
    setPositionAndScroll(66, 90)
    typeText("20<C-B>")
    // Essentially, move top line up a multiple of (window height minus 2) +1. Not sure where the +1 comes from, but it
    // matches Vim behaviour
    assertPosition(34, 0) // Hard to explain, but matches Vim
    assertVisibleArea(0, 34)
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up too many times moves caret to bottom of screen plus scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(40, 60)
    typeText("20<C-B>")
    assertPosition(8, 0)
    assertVisibleArea(0, 34)
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up positions last page with only two lines correctly`() {
    // Vim normally scrolls up window height minus two. When there is a last page with only one or two lines, due to
    // virtual space, it scrolls up window height minus one, or windows height.
    configureByPages(5)
    setEditorVirtualSpace()
    // Vim allows top line to be 175. IntelliJ doesn't. We match the behaviour of Vim at 174, so with 2 lines
    setPositionAndScroll(174, 175)
    typeText("<C-B>")
    assertPosition(174, 0)
    assertVisibleArea(140, 174)
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up positions last page with only two lines correctly 2`() {
    configureByPages(5)
    setEditorVirtualSpace()
    // Vim allows top line to be 175. IntelliJ doesn't. We match the behaviour of Vim at 174, so with 2 lines
    setPositionAndScroll(174, 174)
    typeText("<C-B>")
    assertPosition(174, 0)
    assertVisibleArea(140, 174)
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up on second page moves cursor to previous top`() {
    configureByPages(5)
    setPositionAndScroll(10, 35)
    typeText("<C-B>")
    assertPosition(11, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up puts cursor on first non-blank column`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(50, 60, 14)
    typeText("<C-B>")
    assertPosition(51, 4)
    assertVisibleArea(17, 51)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page up keeps same column with nostartofline`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set nostartofline")
    setPositionAndScroll(50, 60, 14)
    typeText("<C-B>")
    assertPosition(51, 14)
    assertVisibleArea(17, 51)
  }
}
