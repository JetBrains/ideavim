/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteEndOfLineActionTest : VimTestCase() {
  fun `test delete on empty line`() {
    doTest(
      "D",
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
      """.trimIndent(),
      """
                A Discovery
                $c
                I found it in a legendary land
                all rocks and lavender and tufted grass,
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }
}
