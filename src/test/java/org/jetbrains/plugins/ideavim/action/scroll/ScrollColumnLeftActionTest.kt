/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.scroll

import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/*
z<Right>    or                                         *zl* *z<Right>*
zl                      Move the view on the text [count] characters to the
                        right, thus scroll the text [count] characters to the
                        left.  This only works when 'wrap' is off.
 */
class ScrollColumnLeftActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scrolls column to left`() {
    configureByColumns(200)
    typeText("100|", "zl")
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 60, 139)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scrolls column to left with zRight`() {
    configureByColumns(200)
    typeText("100|", "z<Right>")
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 60, 139)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll first column to left moves cursor`() {
    configureByColumns(200)
    typeText("100|", "zs", "zl")
    assertPosition(0, 100)
    assertVisibleLineBounds(0, 100, 179)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scrolls count columns to left`() {
    configureByColumns(200)
    typeText("100|", "10zl")
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 69, 148)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scrolls count columns to left with zRight`() {
    configureByColumns(200)
    typeText("100|", "10z<Right>")
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 69, 148)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scrolls column to left with sidescrolloff moves cursor`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("100|", "zs", "zl")
    assertPosition(0, 100)
    assertVisibleLineBounds(0, 90, 169)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll column to left ignores sidescroll`() {
    configureByColumns(200)
    enterCommand("set sidescroll=10")
    typeText("100|")
    // Assert we got initial scroll correct
    // sidescroll=10 means we don't get the sidescroll jump of half a screen and the cursor is positioned at the right edge
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 20, 99)

    // Scrolls, but doesn't use sidescroll jump
    typeText("zl")
    assertPosition(0, 99)
    assertVisibleLineBounds(0, 21, 100)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll column to left on last page enters virtual space`() {
    configureByColumns(200)
    typeText("200|", "ze", "zl")
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 121, 200)
    typeText("zl")
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 122, 201)
    typeText("zl")
    assertPosition(0, 199)
    assertVisibleLineBounds(0, 123, 202)
  }

  @VimBehaviorDiffers(description = "Vim has virtual space at end of line")
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll columns to left on last page does not have full virtual space`() {
    configureByColumns(200)
    typeText("200|", "ze", "50zl")
    assertPosition(0, 199)
    // Vim is 179-258
    // See also editor.settings.additionalColumnCount
    assertVisibleLineBounds(0, 123, 202)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll column to left correctly scrolls inline inlay associated with preceding text`() {
    configureByColumns(200)
    addInlay(67, true, 5)
    typeText("100|")
    // Text at start of line is:            456:test7
    assertVisibleLineBounds(0, 64, 138)
    typeText("2zl") // 6:test7
    assertVisibleLineBounds(0, 66, 140)
    typeText("zl") // 7
    assertVisibleLineBounds(0, 67, 146)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test scroll column to left correctly scrolls inline inlay associated with following text`() {
    configureByColumns(200)
    addInlay(67, false, 5)
    typeText("100|")
    // Text at start of line is:            456test:78
    assertVisibleLineBounds(0, 64, 138)
    typeText("2zl") // 6test:78
    assertVisibleLineBounds(0, 66, 140)
    typeText("zl") // test:78
    assertVisibleLineBounds(0, 67, 141)
    typeText("zl") // 8
    assertVisibleLineBounds(0, 68, 147)
  }
}
