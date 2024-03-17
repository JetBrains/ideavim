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

class MotionLeftTillMatchCharActionTest : VimTestCase() {
  @Test
  fun `test move and repeat`() {
    doTest(
      "Tx;",
      "hello x hello x hello$c",
      "hello x$c hello x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat twice`() {
    doTest(
      "Tx;;",
      "hello x hello x hello x hello$c",
      "hello x$c hello x hello x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat two`() {
    doTest(
      "Tx2;",
      "hello x hello x hello x hello$c",
      "hello x hello x$c hello x hello",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test move and repeat three`() {
    doTest(
      "Tx3;",
      "hello x hello x hello x hello$c",
      "hello x$c hello x hello x hello",
      Mode.NORMAL(),
    )
  }
}
