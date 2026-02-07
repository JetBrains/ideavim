/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertBackspaceActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.NEOVIM_RPC_SPECIAL_KEYS_INSERT_MODE)
  @Test
  fun `test insert backspace`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    typeText("i", "<BS>")

    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert backspace scrolls start of line`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")

    typeText("70zl", "i", "<BS>")

    // Note that because 'sidescroll' has the default value of 0, we scroll the caret to the middle of the screen, as
    // well as applying sidescrolloff. Leftmost column was 69 (zero-based), and the caret is on column 80. Deleting a
    // character moves the caret to column 79, which is within 'sidescrolloff' of the left edge of the screen. The
    // screen is scrolled by 'sidescroll', which has the default value of 0, so we scroll until the caret is in the
    // middle of the screen, which is 80 characters wide: 79-(80/2)=39
    assertVisibleLineBounds(0, 39, 118)
  }
}
