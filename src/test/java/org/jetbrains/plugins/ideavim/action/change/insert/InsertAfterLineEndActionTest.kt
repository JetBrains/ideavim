/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
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
                ${c}Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectet${c}ur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem IpsumHell${c}o

                Lorem ipsum dolor sit amet,Hell${c}o
                consectetur adipiscing elitHell${c}o
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
    assertMode(Mode.NORMAL())
  }
}
