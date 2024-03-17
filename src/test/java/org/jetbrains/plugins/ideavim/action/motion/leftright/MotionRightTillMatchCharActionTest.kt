/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.leftright

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionRightTillMatchCharActionTest : VimTestCase() {
  @Test
  fun `test move and repeat`() {
    doTest(
      "tx;",
      "${c}hello x hello x hello",
      "hello x hello$c x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat twice`() {
    doTest(
      "tx;;",
      "${c}hello x hello x hello x hello",
      "hello x hello x hello$c x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat two`() {
    doTest(
      "tx2;",
      "${c}hello x hello x hello x hello",
      "hello x hello$c x hello x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat three`() {
    doTest(
      "tx3;",
      "${c}hello x hello x hello x hello x hello",
      "hello x hello x hello$c x hello x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat backwards`() {
    doTest(
      "tx,",
      "hello x hello x ${c}hello x hello x hello",
      "hello x hello x$c hello x hello x hello",
      Mode.NORMAL(),
    )
  }
}
