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
                                                       *CTRL-Y*
CTRL-Y                  Scroll window [count] lines upwards in the buffer.
                        The text moves downwards on the screen.
                        Note: When using the MS-Windows key bindings CTRL-Y is
                        remapped to redo.
 */
class ScrollLineUpActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll single line up`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText("<C-Y>")
    assertPosition(29, 0)
    assertVisibleArea(28, 62)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(29, 63)
    typeText("<C-Y>")
    assertPosition(62, 0)
    assertVisibleArea(28, 62)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up will maintain current column at start of line with sidescrolloff`() {
    configureByPages(5)
    enterCommand("set sidescrolloff=10")
    setPositionAndScroll(29, 63, 5)
    typeText("<C-Y>")
    assertPosition(62, 5)
    assertVisibleArea(28, 62)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count lines up`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText("10<C-Y>")
    assertPosition(29, 0)
    assertVisibleArea(19, 53)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll count lines up will keep cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(29, 63)
    typeText("10<C-Y>")
    assertPosition(53, 0)
    assertVisibleArea(19, 53)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test too many lines up stops at zero`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText("100<C-Y>")
    assertPosition(29, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test too many lines up stops at zero and keeps cursor on screen`() {
    configureByPages(5)
    setPositionAndScroll(59, 59)
    typeText("100<C-Y>")
    assertPosition(34, 0)
    assertVisibleArea(0, 34)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up uses scrolloff and moves cursor`() {
    configureByPages(5)
    enterCommand("set scrolloff=10")
    setPositionAndScroll(20, 44)
    typeText("<C-Y>")
    assertPosition(43, 0)
    assertVisibleArea(19, 53)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll up is not affected by scrolljump`() {
    configureByPages(5)
    enterCommand("set scrolljump=10")
    setPositionAndScroll(29, 63)
    typeText("<C-Y>")
    assertPosition(62, 0)
    assertVisibleArea(28, 62)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up in visual mode`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    typeText("Vjjjj", "<C-Y>")
    assertVisibleArea(28, 62)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up with virtual space`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    setEditorVirtualSpace()
    setPositionAndScroll(85, 90, 4)
    typeText("<C-Y>")
    assertVisibleArea(84, 99)
  }

  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up with virtual space and scrolloff`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setEditorVirtualSpace()
    // Last line is scrolloff from top. <C-Y> should just move last line down
    setPositionAndScroll(89, 99, 4)
    typeText("<C-Y>")
    assertVisibleArea(88, 99)
    assertVisualPosition(99, 4)
  }

  // This actually works, but the set up puts us in the wrong position
  @TestWithoutNeovim(SkipNeovimReason.SCROLL)
  @Test
  fun `test scroll line up on last line with scrolloff`() {
    configureByLines(100, "    Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setEditorVirtualSpace()
    setPositionAndScroll(65, 99, 4)
    typeText("<C-Y>")
    assertVisibleArea(64, 98)
    assertVisualPosition(88, 4) // Moves caret up by scrolloff
  }

  @Test
  fun `test scroll clears status line`() {
    configureByPages(5)
    setPositionAndScroll(29, 29)
    enterSearch("egestas")
    assertStatusLineMessageContains("Pattern not found: egestas")
    typeText("<C-Y>")
    assertStatusLineCleared()
  }
}
