/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.model.VimTestCase
import org.junit.jupiter.api.Test

interface InsertDeleteActionTest : VimTestCase {
  @Test
  fun `test insert delete`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I fo${c}nd it in a legendary land"
    configureByText(before)
    typeText("i", "<Del>")

    assertState(after)
  }
}
