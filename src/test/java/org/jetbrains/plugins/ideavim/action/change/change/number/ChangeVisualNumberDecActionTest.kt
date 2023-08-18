/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class ChangeVisualNumberDecActionTest : VimTestCase() {
  @Test
  fun `test dec visual full number`() {
    doTest(
      "V<C-X>",
      "${c}12345",
      "${c}12344",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test dec visual multiple numbers`() {
    doTest(
      "v10w<C-X>",
      "11 <- should not be decremented |${c}11| should not be decremented -> 12",
      "11 <- should not be decremented |${c}10| should not be decremented -> 12",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test dec visual part of number`() {
    doTest(
      "v4l<C-X>",
      "11111${c}33333111111",
      "11111${c}33332111111",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test dec visual multiple lines`() {
    doTest(
      "V2j<C-X>",
      """
                    no dec 1
                    no dec 1
                    ${c}dec    5
                    dec   5
                    dec   5
                    no dec 1
                    no dec 1

      """.trimIndent(),
      """
                    no dec 1
                    no dec 1
                    ${c}dec    4
                    dec   4
                    dec   4
                    no dec 1
                    no dec 1

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test dec visual 1000 multiple lines`() {
    doTest(
      "V2j<C-X>",
      """
                    ${c}1000
                    1000
                    1000
      """.trimIndent(),
      """
                    ${c}999
                    999
                    999
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test dec visual multiple numbers on line`() {
    doTest(
      "V<C-X>",
      "1 should$c not be decremented -> 2",
      "${c}0 should not be decremented -> 2",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test change number dec visual action`() {
    typeTextInFile(
      injector.parser.parseKeys("Vj<C-X>"),
      """
                    ${c}1
                    2
                    3
                    ${c}4
                    5
      """.trimIndent(),
    )
    assertState(
      """
                ${c}0
                1
                3
                ${c}3
                4
      """.trimIndent(),
    )
  }
}
