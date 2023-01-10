/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertBeforeCursorActionTest : VimTestCase() {
  fun `test check caret shape`() {
    doTest("i", "123", "123", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
    assertCaretsVisualAttributes()
  }
}
