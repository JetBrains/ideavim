/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeNumberDecActionTest : VimTestCase() {
  @Test
  fun `test decrement hex to negative value`() {
    doTest("<C-X>", "0x0000", "0xffffffffffffffff", Mode.NORMAL())
  }

  @Test
  fun `test decrement hex to negative value by 10`() {
    doTest("10<C-X>", "0x0005", "0xfffffffffffffffb", Mode.NORMAL())
  }

  @Test
  fun `test decrement oct to negative value`() {
    doTest(
      ":set nrformats+=octal<CR><C-X>",
      "00000",
      "01777777777777777777777",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test decrement incorrect octal`() {
    doTest(":set nrformats+=octal<CR><C-X>", "008", "7", Mode.NORMAL())
  }

  @Test
  fun `test decrement oct to negative value by 10`() {
    doTest(
      ":set nrformats+=octal<CR>10<C-X>",
      "00005",
      "01777777777777777777773",
      Mode.NORMAL(),
    )
  }
}
