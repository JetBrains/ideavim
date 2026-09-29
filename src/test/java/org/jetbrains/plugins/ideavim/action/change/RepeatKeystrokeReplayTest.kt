/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.keys
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * `.` replays the keys of the last change, so the things that can get between a change and its repeat - another `.`,
 * a mapping, a macro, a command line - all need pinning down.
 */
class RepeatKeystrokeReplayTest : VimTestCase() {

  @Test
  fun `test a second dot repeats the change again, not the first dot`() {
    doTest(
      "cwfoo<Esc>" + "w" + "." + "w" + ".",
      "${c}alpha beta gamma",
      "foo foo fo${c}o",
    )
  }

  @Test
  fun `test dot repeats an insert change again`() {
    // `i` inserts before the caret, and Escape leaves it on the character just inserted, so each repeat adds one more
    // in front of the previous one
    doTest(
      "ix<Esc>" + "." + ".",
      "${c}alpha",
      "${c}xxxalpha",
    )
  }

  @Test
  fun `test the recorded change survives motions and a search in between`() {
    doTest(
      "x" + "/gamma<CR>" + ".",
      """
        ${c}alpha
        beta
        gamma
      """.trimIndent(),
      """
        lpha
        beta
        ${c}amma
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  @Test
  fun `test dot repeats a change made through a mapping by running the mapping again`() {
    doTest(
      "Q" + ".",
      "${c}alpha beta gamma",
      "${c}gamma",
    ) {
      injector.keyGroup.putKeyMapping(MappingMode.N, keys("Q"), MappingOwner.IdeaVim.Other, keys("dw"), false)
    }
  }

  @TestWithoutNeovim(
    SkipNeovimReason.SEE_DESCRIPTION,
    description = "Macro recording is driven through IdeaVim's own key handler"
  )
  @Test
  fun `test dot repeats the last change a macro made`() {
    doTest(
      "qacwfoo<Esc>q" + "j0" + "@a" + "j0" + ".",
      """
        ${c}alpha one
        beta two
        gamma three
      """.trimIndent(),
      """
        foo one
        foo two
        fo${c}o three
      """.trimIndent(),
    )
  }

  @Test
  fun `test dot does not repeat a motion`() {
    // `w` is not a change, so `.` still repeats the `x`
    doTest(
      "x" + "w" + ".",
      "${c}alpha beta",
      "lpha ${c}eta",
    )
  }
}
