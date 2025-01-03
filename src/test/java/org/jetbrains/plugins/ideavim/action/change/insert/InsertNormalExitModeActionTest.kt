/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertNormalExitModeActionTest : VimTestCase() {
  @Test
  fun `test exit insert normal mode`() {
    doTest("i<C-O><Esc>", "12${c}3", "12${c}3", Mode.INSERT)
  }
}
