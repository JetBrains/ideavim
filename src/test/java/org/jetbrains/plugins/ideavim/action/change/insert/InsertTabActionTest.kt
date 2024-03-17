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

class InsertTabActionTest : VimTestCase() {
  @Test
  fun `test insert tab`() {
    setupChecks {
      keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
    }
    val before = "I fo${c}und it in a legendary land"
    val after = "I fo    ${c}und it in a legendary land"
    configureByText(before)

    typeText("i", "<Tab>")

    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert tab scrolls at end of line`() {
    setupChecks {
      keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
    }
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")

    // TODO: This works for tests, but not in real life. See VimShortcutKeyAction.isEnabled
    typeText("70|", "i", "<Tab>")
    assertVisibleLineBounds(0, 32, 111)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test insert tab scrolls at end of line 2`() {
    configureByColumns(200)
    enterCommand("set sidescrolloff=10")
    typeText("70|", "i", "<C-I>")
    assertVisibleLineBounds(0, 32, 111)
  }
}
