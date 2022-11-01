/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionRightTillMatchCharActionTest : VimTestCase() {
  fun `test move and repeat`() {
    doTest(
      "tx;",
      "${c}hello x hello x hello",
      "hello x hello$c x hello",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun `test move and repeat twice`() {
    doTest(
      "tx;;",
      "${c}hello x hello x hello x hello",
      "hello x hello x hello$c x hello",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun `test move and repeat two`() {
    doTest(
      "tx2;",
      "${c}hello x hello x hello x hello",
      "hello x hello$c x hello x hello",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun `test move and repeat three`() {
    doTest(
      "tx3;",
      "${c}hello x hello x hello x hello x hello",
      "hello x hello x hello$c x hello x hello",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun `test move and repeat backwards`() {
    doTest(
      "tx,",
      "hello x hello x ${c}hello x hello x hello",
      "hello x hello x$c hello x hello x hello",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }
}
