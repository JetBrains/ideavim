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
class ChangeVisualNumberIncActionTest : VimTestCase() {
  @Test
  fun `test inc visual full number`() {
    doTest(
      "V<C-A>",
      "${c}12345",
      "${c}12346",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual multiple numbers`() {
    doTest(
      "v10w<C-A>",
      "11 <- should not be incremented |${c}11| should not be incremented -> 12",
      "11 <- should not be incremented |${c}12| should not be incremented -> 12",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual part of number`() {
    doTest(
      "v4l<C-A>",
      "11111${c}22222111111",
      "11111${c}22223111111",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual multiple lines`() {
    doTest(
      "V2j<C-A>",
      """
                    no inc 1
                    no inc 1
                    ${c}inc    5
                    inc   5
                    inc   5
                    no inc 1
                    no inc 1

      """.trimIndent(),
      """
                    no inc 1
                    no inc 1
                    ${c}inc    6
                    inc   6
                    inc   6
                    no inc 1
                    no inc 1

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual 999 multiple lines`() {
    doTest(
      "V2j<C-A>",
      """
                    ${c}999
                    999
                    999
      """.trimIndent(),
      """
                    ${c}1000
                    1000
                    1000
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test inc visual multiple numbers on line`() {
    doTest(
      "V<C-A>",
      "1 should$c not be incremented -> 2",
      "${c}2 should not be incremented -> 2",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test change number inc visual multiple cursor`() {
    typeTextInFile(
      injector.parser.parseKeys("Vj<C-A>"),
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
                    ${c}2
                    3
                    3
                    ${c}5
                    6
      """.trimIndent(),
    )
  }

  @Test
  fun `test two numbers on the same line`() {
    doTest(
      "v$<C-A>",
      "1 <- should$c not be incremented 2",
      "1 <- should$c not be incremented 3",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test two numbers on the same line with two lines`() {
    doTest(
      "vj<C-A>",
      """1 <- should$c not be incremented 2
        |1 should not be incremented -> 2
      """.trimMargin(),
      """1 <- should$c not be incremented 3
        |2 should not be incremented -> 2
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test two numbers on the same line with three lines`() {
    doTest(
      "vjj<C-A>",
      """1 <- should$c not be incremented 2
        |1 should not be incremented -> 2
        |1 should not be incremented -> 2
      """.trimMargin(),
      """1 <- should$c not be incremented 3
        |2 should not be incremented -> 2
        |2 should not be incremented -> 2
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test block nothing increment`() {
    doTest(
      "<C-V>jjll<C-A>",
      """
        |1 <- should$c not be incremented -> 2
        |1 <- should not be incremented -> 2
        |1 <- should not be incremented -> 2
      """.trimMargin(),
      """
        |1 <- should$c not be incremented -> 2
        |1 <- should not be incremented -> 2
        |1 <- should not be incremented -> 2
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test block increment end`() {
    doTest(
      "<C-V>jj$<C-A>",
      """
        |1 <- should$c not be incremented 2
        |1 <- should not be incremented 2
        |1 <- should not be incremented 2
      """.trimMargin(),
      """
        |1 <- should$c not be incremented 3
        |1 <- should not be incremented 3
        |1 <- should not be incremented 3
      """.trimMargin(),
      Mode.NORMAL(),
    )
  }
}
