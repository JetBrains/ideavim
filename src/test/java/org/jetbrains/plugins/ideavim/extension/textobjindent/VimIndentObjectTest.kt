/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.textobjindent

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * @author Shrikant Sharat Kandula (@sharat87)
 */
@TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
class VimIndentObjectTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("textobj-indent")
  }

  @Test
  fun testSingleLine() {
    doTest(
      "dii",
      """
        one
      """.trimIndent(),
      "",
    )
  }

  @Test
  fun testDeleteFlatIndent() {
    doTest(
      "dii",
      """
        one
        two
        three
        four
      """.trimIndent(),
      "",
    )
  }

  @Test
  fun testDeleteOuterFlatIndent() {
    doTest(
      "dai",
      """
        one
        two
        three
        four
      """.trimIndent(),
      "",
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteFarOuterIndent() {
    doTest(
      "2GdaI",
      """
        one
          two
          three
        four
      """.trimIndent(),
      "",
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
  }

  @Test
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
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    assertSelection(null)
  }

  @Test
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

      """.trimIndent(),
    )
  }

  @Test
  fun testSelectNestedTabs() {
    doTest(
      "vii",
      """
      {
      .second level with tab. will still select all before fix
      .${c}{
      ..third level with tab. Will select also the second level before fix
      .}
      }
      """.trimIndent().dotToTab(),
      """
      {
      ${s}.second level with tab. will still select all before fix
      .{
      ..third level with tab. Will select also the second level before fix
      .}$se
      }
      """.trimIndent().dotToTab(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
