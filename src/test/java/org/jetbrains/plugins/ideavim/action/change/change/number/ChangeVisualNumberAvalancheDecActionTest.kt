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

/**
 * @author Alex Plate
 */
class ChangeVisualNumberAvalancheDecActionTest : VimTestCase() {
  fun `test dec visual avalanche`() {
    doTest(
      "VGg<C-X>",
      """
                    ${c}number 2
                    number 3
                    number 4
      """.trimIndent(),
      """
                    ${c}number 1
                    number 1
                    number 1
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun `test dec visual avalanche multiple times`() {
    doTest(
      "VG2g<C-X>",
      """
                    ${c}number 3
                    number 5
                    number 7
      """.trimIndent(),
      """
                    ${c}number 1
                    number 1
                    number 1
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }
}
