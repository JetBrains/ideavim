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

class InsertSingleCommandActionTest : VimTestCase() {
  fun `test enter visual`() {
    doTest(
      listOf("i", "<C-O>", "vlll", "<Esc>"),
      "I found ${c}it in a legendary land",
      "I found it ${c}in a legendary land",
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE
    )
  }
}
