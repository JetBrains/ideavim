/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjindent

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
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
    assertMode(VimStateMachine.Mode.COMMAND)
    assertSelection(null)
  }
}
