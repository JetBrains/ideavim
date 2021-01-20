/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class ChangeVisualNumberIncActionTest : VimTestCase() {
  fun `test inc visual full number`() {
    doTest("V<C-A>",
      "${c}12345",
      "${c}12346",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test inc visual multiple numbers`() {
    doTest("v10w<C-A>",
      "11 <- should not be incremented |${c}11| should not be incremented -> 12",
      "11 <- should not be incremented |${c}12| should not be incremented -> 12",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test inc visual part of number`() {
    doTest("v4l<C-A>",
      "11111${c}22222111111",
      "11111${c}22223111111",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test inc visual multiple lines`() {
    doTest("V2j<C-A>",
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  fun `test inc visual 999 multiple lines`() {
    doTest("V2j<C-A>",
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test inc visual multiple numbers on line`() {
    doTest("V<C-A>",
      "1 should$c not be incremented -> 2",
      "${c}2 should not be incremented -> 2",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test change number inc visual multiple cursor`() {
    typeTextInFile(parseKeys("Vj<C-A>"),
      """
                    ${c}1
                    2
                    3
                    ${c}4
                    5
                    """.trimIndent())
    myFixture.checkResult(
      """
                    ${c}2
                    3
                    3
                    ${c}5
                    6
                    """.trimIndent())
  }

  fun `test two numbers on the same line`() {
    doTest("v$<C-A>",
      "1 <- should$c not be incremented 2",
      "1 <- should$c not be incremented 3",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test two numbers on the same line with two lines`() {
    doTest("vj<C-A>",
      """1 <- should$c not be incremented 2
        |1 should not be incremented -> 2
      """.trimMargin(),
      """1 <- should$c not be incremented 3
        |2 should not be incremented -> 2
      """.trimMargin(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test two numbers on the same line with three lines`() {
    doTest("vjj<C-A>",
      """1 <- should$c not be incremented 2
        |1 should not be incremented -> 2
        |1 should not be incremented -> 2
      """.trimMargin(),
      """1 <- should$c not be incremented 3
        |2 should not be incremented -> 2
        |2 should not be incremented -> 2
      """.trimMargin(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test block nothing increment`() {
    doTest("<C-V>jjll<C-A>",
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun `test block increment end`() {
    doTest("<C-V>jj$<C-A>",
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }
}
