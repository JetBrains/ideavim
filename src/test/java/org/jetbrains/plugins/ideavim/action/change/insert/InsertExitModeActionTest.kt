/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertExitModeActionTest : VimTestCase() {
  fun `test exit visual mode`() {
    doTest("i<Esc>", "12${c}3", "1${c}23", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test exit visual mode on line start`() {
    doTest("i<Esc>", "${c}123", "${c}123", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }
}
