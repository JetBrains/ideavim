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

class InsertBeforeFirstNonBlankActionTest : VimTestCase() {
  @Test
  fun `test insert multiple carets`() {
    doTest(
      "IHello<esc>",
      """
                ${c}Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consect${c}etur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Hell${c}oLorem Ipsum

                Hell${c}oLorem ipsum dolor sit amet,
                Hell${c}oconsectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertMode(Mode.NORMAL())
  }
}
