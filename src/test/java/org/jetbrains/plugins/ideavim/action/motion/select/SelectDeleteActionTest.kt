/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SelectDeleteActionTest : VimTestCase() {
  @Test
  fun `test Delete removes text and returns to Normal mode`() {
    doTest(
      listOf("ve", "<C-G>", "<Del>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.NORMAL()
    )
  }

  @Test
  fun `test Delete removes text and returns to Insert mode when invoked from Insert Select`() {
    doTest(
      listOf("i", "<S-Right>".repeat(5), "<Del>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.INSERT
    ) {
      enterCommand("set selectmode=key keymodel=startsel")
    }
  }

  @Test
  fun `test Delete removes text and returns to Replace mode when invoked from Select with pending Replace mode`() {
    doTest(
      listOf("R", "<S-Right>".repeat(5), "<Del>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.REPLACE
    ) {
      enterCommand("set selectmode=key keymodel=startsel")
    }
  }

  @Test
  fun `test Backspace deletes text and returns to Normal mode`() {
    doTest(
      listOf("ve", "<C-G>", "<Del>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.NORMAL()
    )
  }

  @Test
  fun `test Backspace removes text and returns to Insert mode when invoked from Insert Select`() {
    doTest(
      listOf("i", "<S-Right>".repeat(5), "<BS>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.INSERT
    ) {
      enterCommand("set selectmode=key keymodel=startsel")
    }
  }

  @Test
  fun `test Backspace removes text and returns to Replace mode when invoked from Select with pending Replace mode`() {
    doTest(
      listOf("R", "<S-Right>".repeat(5), "<BS>"),
      """
        |Lorem Ipsum
        |
        |I ${c}found it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      """
        |Lorem Ipsum
        |
        |I $c it in a legendary land
        |consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
      Mode.REPLACE
    ) {
      enterCommand("set selectmode=key keymodel=startsel")
    }
  }
}
