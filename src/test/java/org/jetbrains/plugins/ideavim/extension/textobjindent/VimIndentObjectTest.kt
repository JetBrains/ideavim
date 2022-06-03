/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.extension.textobjindent

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.JavaVimTestCase

/**
 * @author Shrikant Sharat Kandula (@sharat87)
 */
class VimIndentObjectTest : JavaVimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-indent")
  }

  fun testSingleLine() {
    doTest(
      injector.parser.parseKeys("dii"),
      """
        one
      """.trimIndent(),
      ""
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteFlatIndent() {
    doTest(
      injector.parser.parseKeys("dii"),
      """
        one
        two
        three
        four
      """.trimIndent(),
      ""
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteOuterFlatIndent() {
    doTest(
      injector.parser.parseKeys("dai"),
      """
        one
        two
        three
        four
      """.trimIndent(),
      ""
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteInnerIndent() {
    doTest(
      injector.parser.parseKeys("2Gdii"),
      """
        one
          two
          three
        four
      """.trimIndent(),
      """
        one
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteOuterIndent() {
    doTest(
      injector.parser.parseKeys("2Gdai"),
      """
        one
          two
          three
        four
      """.trimIndent(),
      """
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteFarOuterIndent() {
    doTest(
      injector.parser.parseKeys("2GdaI"),
      """
        one
          two
          three
        four
      """.trimIndent(),
      ""
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteInnerIndentWithLinesAbove() {
    doTest(
      injector.parser.parseKeys("5Gdii"),
      """
        all
        negatives
        go hear
        one
          two
          three
        four
      """.trimIndent(),
      """
        all
        negatives
        go hear
        one
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testDeleteInnerIndentWithBlankLinesAbove() {
    doTest(
      injector.parser.parseKeys("6Gdii"),
      """
        all
        negatives
        go hear

        one
          two
          three
        four
      """.trimIndent(),
      """
        all
        negatives
        go hear

        one
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testNested1() {
    doTest(
      injector.parser.parseKeys("2Gdii"),
      """
        one
          two
            three
        four
      """.trimIndent(),
      """
        one
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testNested1a() {
    doTest(
      injector.parser.parseKeys("3Gdai"),
      """
        one
          two
            three
        four
      """.trimIndent(),
      """
        one
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testNested2() {
    doTest(
      injector.parser.parseKeys("3Gdii"),
      """
        one
          two
            three
        four
      """.trimIndent(),
      """
        one
          two
        four
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testNested3() {
    doTest(
      injector.parser.parseKeys("3Gdii"),
      """
        one
          two
            three
        four
        five
      """.trimIndent(),
      """
        one
          two
        four
        five
      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }

  fun testNested4() {
    doTest(
      injector.parser.parseKeys("3Gdii"),
      """
        one
          two
            three
        four

      """.trimIndent(),
      """
        one
          two
        four

      """.trimIndent()
    )
    assertMode(CommandState.Mode.COMMAND)
    assertSelection(null)
  }
}
