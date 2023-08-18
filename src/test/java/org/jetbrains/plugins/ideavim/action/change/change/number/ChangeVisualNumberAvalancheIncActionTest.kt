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

/**
 * @author Alex Plate
 */
class ChangeVisualNumberAvalancheIncActionTest : VimTestCase() {
  @Test
  fun `test inc visual avalanche`() {
    doTest(
      "VGg<C-A>",
      """
                    ${c}number 1
                    number 1
                    number 1
      """.trimIndent(),
      """
                    ${c}number 2
                    number 3
                    number 4
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual avalanche multiple times`() {
    doTest(
      "VG2g<C-A>",
      """
                    ${c}number 1
                    number 1
                    number 1
      """.trimIndent(),
      """
                    ${c}number 3
                    number 5
                    number 7
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }
}
