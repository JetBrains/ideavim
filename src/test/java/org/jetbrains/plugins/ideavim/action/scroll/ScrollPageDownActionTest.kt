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
<S-Down>        or                             *<S-Down>* *<kPageDown>*
<PageDown>      or                             *<PageDown>* *CTRL-F*
CTRL-F                  Scroll window [count] pages Forwards (downwards) in
                        the buffer.  See also 'startofline' option.
                        When there is only one window the 'window' option
                        might be used.

<S-Down>        move window one page down      *i_<S-Down>*
<PageDown>      move window one page down      *i_<PageDown>*
 */
class ScrollPageDownActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page down with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("<S-Down>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page down with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("<PageDown>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page down with CTRL-F`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("<C-F>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single page down with S-Enter`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("<S-Enter>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down in insert mode with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("i", "<S-Down>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down in insert mode with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("i", "<PageDown>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages down with S-Down`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("3<S-Down>")
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages down with PageDown`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("3<PageDown>")
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count pages down with CTRL-F`() {
    configureByPages(5)
    setPositionAndScroll(0, 0)
    typeText("3<C-F>")
    assertPosition(99, 0)
    assertVisibleArea(99, 133)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down moves cursor to top of screen`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText("<C-F>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down in insert mode moves cursor`() {
    configureByPages(5)
    setPositionAndScroll(0, 20)
    typeText("i", "<S-Down>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down moves cursor with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("<C-F>")
    assertPosition(43, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down in insert mode moves cursor with scrolloff`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(0, 20)
    typeText("i", "<S-Down>")
    assertPosition(43, 0)
    assertVisibleArea(33, 67)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down ignores scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(0, 0)
    typeText("<C-F>")
    assertPosition(33, 0)
    assertVisibleArea(33, 67)
  }

  @VimBehaviorDiffers(description = "IntelliJ does not have virtual space enabled by default")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last page moves cursor to end of file`() {
    configureByPages(5)
    setPositionAndScroll(145, 150)
    typeText("<C-F>")
    assertPosition(175, 0)
    assertVisibleArea(146, 175)
  }

  @VimBehaviorDiffers(description = "IntelliJ keeps 2 lines at the top of a file even with virtual space")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last page with virtual space`() {
    configureByPages(5)
    setEditorVirtualSpace()
    setPositionAndScroll(145, 150)
    typeText("<C-F>")
    assertPosition(175, 0)
    assertVisibleArea(174, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on penultimate page`() {
    configureByPages(5)
    setPositionAndScroll(110, 130)
    typeText("<C-F>")
    assertPosition(143, 0)
    assertVisibleArea(143, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last line scrolls up by default virtual space`() {
    configureByPages(5)
    setPositionAndScroll(146, 175)
    typeText("<C-F>")
    assertPosition(175, 0)
    // 146+35 = 181 -> 6 lines of virtual space
    assertVisibleArea(146, 175)
  }

  @VimBehaviorDiffers(description = "IntelliJ keeps 2 lines at the top of a file even with virtual space")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last line scrolls up by virtual space`() {
    configureByPages(5)
    setEditorVirtualSpace()
    setPositionAndScroll(146, 175)
    typeText("<C-F>")
    assertPosition(175, 0)
    assertVisibleArea(174, 175)
  }

  @VimBehaviorDiffers(description = "IntelliJ keeps 2 lines at the top of a file even with virtual space")
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on fully scrolled last line does not move`() {
    configureByPages(5)
    setEditorVirtualSpace()
    // This would be 175 in Vim
    setPositionAndScroll(174, 175)
    typeText("<C-F>")
    assertPosition(175, 0)
    assertVisibleArea(174, 175)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last line causes beep with default lines of virtual space`() {
    configureByPages(5)
    // 146 is 5 lines of virtual space
    setPositionAndScroll(146, 175)
    typeText("<C-F>")
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down on last line causes beep with virtual space`() {
    configureByPages(5)
    setEditorVirtualSpace()
    setPositionAndScroll(174, 175)
    typeText("<C-F>")
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down too far causes error bell`() {
    configureByPages(5)
    setPositionAndScroll(146, 175)
    typeText("10<C-F>")
    assertPluginError(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down puts cursor on first non-blank column`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setPositionAndScroll(20, 25, 14)
    typeText("<C-F>")
    assertPosition(53, 4)
    assertVisibleArea(53, 87)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll page down keeps same column with nostartofline`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set nostartofline")
    setPositionAndScroll(20, 25, 14)
    typeText("<C-F>")
    assertPosition(53, 14)
    assertVisibleArea(53, 87)
  }
}
