/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeNumberDecActionTest : VimTestCase() {
  fun `test decrement hex to negative value`() {
    doTest("<C-X>", "0x0000", "0xffffffffffffffff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test decrement hex to negative value by 10`() {
    doTest("10<C-X>", "0x0005", "0xfffffffffffffffb", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test decrement oct to negative value`() {
    doTest(
      ":set nrformats+=octal<CR><C-X>",
      "00000",
      "01777777777777777777777",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun `test decrement incorrect octal`() {
    doTest(":set nrformats+=octal<CR><C-X>", "008", "7", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test decrement oct to negative value by 10`() {
    doTest(
      ":set nrformats+=octal<CR>10<C-X>",
      "00005",
      "01777777777777777777773",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
