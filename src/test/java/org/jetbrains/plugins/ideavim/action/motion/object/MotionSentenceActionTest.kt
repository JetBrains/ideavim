/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for sentence text objects (is, as) which use preserveSelectionAnchor = false.
 *
 * preserveSelectionAnchor = false affects visual mode behavior:
 * - Selection anchor is reset to block start when applying text object
 * - Entire block is selected regardless of selection direction
 */
@Suppress("SpellCheckingInspection")
class MotionSentenceActionTest : VimTestCase() {

  // ============== Inner Sentence (is) ==============

  @Test
  fun `test inner sentence from middle of sentence`() {
    doTest(
      "vis",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${s}Second sentence here${c}.${se} Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner sentence from start of sentence`() {
    doTest(
      "vis",
      "First sentence here. ${c}Second sentence here. Third sentence here.",
      "First sentence here. ${s}Second sentence here${c}.${se} Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner sentence from end of sentence`() {
    doTest(
      "vis",
      "First sentence here. Second sentence her${c}e. Third sentence here.",
      "First sentence here. ${s}Second sentence here${c}.${se} Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(shouldBeFixed = true)
  fun `test inner sentence with backwards selection`() {
    // Start visual mode, move backwards, then apply is
    // Text object selects entire sentence regardless of prior selection direction
    doTest(
      listOf("v", "b", "is"),
      "First sentence here. Second sent${c}ence here. Third sentence here.",
      "First sentence here. S${s}econd sent${c}e${se}nce here. Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(shouldBeFixed = true)
  fun `test inner sentence with backwards selection crossing sentence boundary`() {
    // Move cursor back multiple words (into first sentence), then apply is
    // Text object selects the sentence where cursor currently is
    doTest(
      listOf("v", "b", "b", "b", "is"),
      "First sentence here. Second sent${c}ence here. Third sentence here.",
      "F${s}irst sentence here. Second sent${c}e${se}nce here. Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner sentence on first sentence`() {
    doTest(
      "vis",
      "Fi${c}rst sentence here. Second sentence here.",
      "${s}First sentence here${c}.${se} Second sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner sentence on last sentence`() {
    doTest(
      "vis",
      "First sentence here. Second sen${c}tence here.",
      "First sentence here.${s} Second sentence here${c}.${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test inner sentence with multiline`() {
    doTest(
      "vis",
      """
        |First sentence here.
        |Sec${c}ond sentence
        |continues here. Third sentence.
      """.trimMargin(),
      """
        |First sentence here.
        |${s}Second sentence
        |continues here${c}.${se} Third sentence.
      """.trimMargin(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Outer Sentence (as) ==============

  @Test
  fun `test outer sentence from middle of sentence`() {
    doTest(
      "vas",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${s}Second sentence here.${c} ${se}Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(shouldBeFixed = true)
  fun `test outer sentence with backwards selection`() {
    // Text object selects entire sentence regardless of prior selection direction
    doTest(
      listOf("v", "b", "as"),
      "First sentence here. Second sent${c}ence here. Third sentence here.",
      "First sentence here. ${s}Second sent${c}e${se}nce here. Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer sentence on first sentence`() {
    doTest(
      "vas",
      "Fi${c}rst sentence here. Second sentence here.",
      "${s}First sentence here.${c} ${se}Second sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test outer sentence on last sentence`() {
    doTest(
      "vas",
      "First sentence here. Second sen${c}tence here.",
      "First sentence here.${s} Second sentence here${c}.${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Repeated Sentence Text Object ==============

  @Test
  fun `test repeated inner sentence expands selection`() {
    // Current IdeaVim behavior: second is just selects trailing space, not next sentence
    doTest(
      listOf("vis", "is"),
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${s}Second sentence here.${c} ${se}Third sentence here.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `test repeated outer sentence expands selection`() {
    doTest(
      listOf("vas", "as"),
      "First sentence here. Sec${c}ond sentence here. Third sentence here. Fourth sentence.",
      "First sentence here. ${s}Second sentence here. Third sentence here.${c} ${se}Fourth sentence.",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ============== Delete with Sentence Text Object ==============

  @Test
  fun `test delete inner sentence`() {
    doTest(
      "dis",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${c} Third sentence here.",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test delete outer sentence`() {
    doTest(
      "das",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${c}Third sentence here.",
      Mode.NORMAL(),
    )
  }

  // ============== Change with Sentence Text Object ==============

  @Test
  fun `test change inner sentence`() {
    doTest(
      "cis",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${c} Third sentence here.",
      Mode.INSERT,
    )
  }

  // ============== Yank with Sentence Text Object ==============

  @Test
  fun `test yank inner sentence`() {
    doTest(
      "yis",
      "First sentence here. Sec${c}ond sentence here. Third sentence here.",
      "First sentence here. ${c}Second sentence here. Third sentence here.",
      Mode.NORMAL(),
    )
  }

  // ============== Edge Cases (from MotionOuterSentenceActionTest) ==============

  @VimBehaviorDiffers(
    originalVimAfter = """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
    """,
  )
  @Test
  fun `test indent outer sentence on empty last line`() {
    doTest(
      "=as",
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
      """.trimIndent(),
      """
        ${c}Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.

      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
    """,
  )
  @Test
  fun `test delete outer sentence on empty last line`() {
    doTest(
      "das",
      """
        Lorem ipsum dolor sit amet,
        consectetur adipiscing elit
        Sed in orci mauris.
        $c
      """.trimIndent(),
      "\n",
      Mode.NORMAL(),
    )
  }
}
