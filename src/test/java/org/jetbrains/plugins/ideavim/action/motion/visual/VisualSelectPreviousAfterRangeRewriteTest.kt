/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * `gv` must reselect the last Visual selection after the selected range has been rewritten.
 *
 * Reported scenario (`:'<,'>sort`, `:'<,'>sort u`, then `u`): Vim reselects the affected lines, adjusting the end of
 * the selection for the lines removed by the command, while IdeaVim collapsed the selection to the last line.
 *
 * This is not specific to `:sort`. Everything that rewrites the whole selected range in a single document change is
 * affected, e.g. `gU` or `g?`. Commands that change the lines one by one (`:s`, `:normal`, `:>`, `:m`, `:j`, `r`)
 * restore the selection correctly.
 *
 * Note: entering Command-line mode from Visual mode prefills the range, so `":sort<CR>"` below is `:'<,'>sort`.
 */
class VisualSelectPreviousAfterRangeRewriteTest : VimTestCase() {
  @Test
  fun `test gv reselects sorted lines`() {
    doTest(
      listOf("Vjj", ":sort<CR>", "gv"),
      """
        one
        ${c}two
        two
        three
        four
      """.trimIndent(),
      """
        one
        ${s}three
        two
        ${c}two
        ${se}four
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test gv reselects remaining lines after sort removed duplicates`() {
    doTest(
      listOf("Vjj", ":sort u<CR>", "gv"),
      """
        one
        ${c}three
        two
        two
        four
      """.trimIndent(),
      """
        one
        ${s}three
        ${c}two
        ${se}four
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "one\n${s}three\ntwo\n${c}two\n${se}four",
    description = "Vim stores the Visual area in the undo state and restores it on undo (uh_visual). IdeaVim only " +
      "adjusts the marks for the document change, so '> stays on the line the duplicate was removed from.",
  )
  @Test
  fun `test gv after undoing sort that removed duplicates`() {
    doTest(
      listOf("Vjj", ":sort u<CR>", "u", "gv"),
      """
        one
        ${c}three
        two
        two
        four
      """.trimIndent(),
      """
        one
        ${s}three
        ${c}two
        ${se}two
        four
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test gv reselects lines after uppercasing the selection`() {
    doTest(
      listOf("VjjU", "gv"),
      """
        one
        ${c}two
        two
        three
        four
      """.trimIndent(),
      """
        one
        ${s}TWO
        TWO
        ${c}THREE
        ${se}four
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }

  @Test
  fun `test gv reselects lines after rot13 of the selection`() {
    doTest(
      listOf("Vjjg?", "gv"),
      """
        one
        ${c}two
        two
        three
        four
      """.trimIndent(),
      """
        one
        ${s}gjb
        gjb
        ${c}guerr
        ${se}four
      """.trimIndent(),
      Mode.VISUAL(SelectionType.LINE_WISE),
    )
  }
}
