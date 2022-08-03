/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjindent

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Shrikant Sharat Kandula (@sharat87)
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimIndentObjectTest : VimTestCase() {
  override fun setUp() {
    super.setUp()
    enableExtensions("textobj-indent")
  }

  fun testSingleLine() {
    doTest(
      "dii",
      """
        one
      """.trimIndent(),
      ""
    )
  }

  fun testDeleteFlatIndent() {
    doTest(
      "dii",
      """
        one
        two
        three
        four
      """.trimIndent(),
      ""
    )
  }

  fun testDeleteOuterFlatIndent() {
    doTest(
      "dai",
      """
        one
        two
        three
        four
      """.trimIndent(),
      ""
    )
  }

  fun testDeleteInnerIndent() {
    doTest(
      "2Gdii",
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
  }

  fun testDeleteOuterIndent() {
    doTest(
      "2Gdai",
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
  }

  fun testDeleteFarOuterIndent() {
    doTest(
      "2GdaI",
      """
        one
          two
          three
        four
      """.trimIndent(),
      ""
    )
  }

  fun testDeleteInnerIndentWithLinesAbove() {
    doTest(
      "5Gdii",
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
  }

  fun testDeleteInnerIndentWithBlankLinesAbove() {
    doTest(
      "6Gdii",
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
  }

  fun testNested1() {
    doTest(
      "2Gdii",
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
  }

  fun testNested1a() {
    doTest(
      "3Gdai",
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
  }

  fun testNested2() {
    doTest(
      "3Gdii",
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
  }

  fun testNested3() {
    doTest(
      "3Gdii",
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
      "3Gdii",
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
  }
}
