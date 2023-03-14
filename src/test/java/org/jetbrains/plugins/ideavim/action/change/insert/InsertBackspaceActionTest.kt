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
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
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
    assertVisibleLineBounds(0, 39, 118)
  }
}
