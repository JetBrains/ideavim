/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertAfterLineEndActionTest : VimTestCase() {
  @Test
  fun `test insert after line end action`() {
    typeTextInFile(
      injector.parser.parseKeys("A" + " four" + "<ESC>"),
      """
                    one two ${c}three
                    sev${c}en si${c}x five

      """.trimIndent(),
    )
    assertState(
      """
    one two three fou${c}r
    seven six five fou${c}r

      """.trimIndent(),
    )
  }

  @Test
  fun `test multiple carets`() {
    doTest(
      "AHello<esc>",
      """
                ${c}A Discovery

                ${c}I found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                A DiscoveryHell${c}o

                I found it in a legendary landHell${c}o
                all rocks and lavender and tufted grass,Hell${c}o
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertMode(VimStateMachine.Mode.COMMAND)
  }
}
