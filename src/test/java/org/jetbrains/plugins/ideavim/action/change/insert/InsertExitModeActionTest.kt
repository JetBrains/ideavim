/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertExitModeActionTest : VimTestCase() {
  @Test
  fun `test exit visual mode`() {
    doTest("i<Esc>", "12${c}3", "1${c}23", Mode.NORMAL())
  }

  @Test
  fun `test exit visual mode on line start`() {
    doTest("i<Esc>", "${c}123", "${c}123", Mode.NORMAL())
  }
}
