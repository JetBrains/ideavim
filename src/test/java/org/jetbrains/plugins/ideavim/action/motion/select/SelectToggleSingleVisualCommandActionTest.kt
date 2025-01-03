/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.select

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
@TestWithoutNeovim(SkipNeovimReason.SELECT_MODE)
class SelectToggleSingleVisualCommandActionTest : VimTestCase() {
  @Test
  fun `test enter Visual mode for single command`() {
    doTest(
      "gh<C-O>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ${s}${c}i${se}psum dolor sit amet",
      Mode.VISUAL(SelectionType.CHARACTER_WISE, returnTo = Mode.SELECT(SelectionType.CHARACTER_WISE)),
    )
  }

  @Test
  fun `test enter Visual mode for single motion`() {
    doTest(
      "gh<C-O>e",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ${s}ipsum${se}${c} dolor sit amet",
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }

  // AFAICT, all Visual operators remove the selection
  @Test
  fun `test returns to Normal if Visual operator removes selection`() {
    doTest(
      "gh<C-O>U",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ${c}Ipsum dolor sit amet",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test Escape returns to Normal after entering Visual for a single command`() {
    // Escape doesn't "pop the stack", but returns to Normal
    doTest(
      "gh<C-O><Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ${c}ipsum dolor sit amet",
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test exit Visual mode with same shortcut`() {
    doTest(
      "gh<C-O><C-O>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ${s}i${c}${se}psum dolor sit amet",
      Mode.SELECT(SelectionType.CHARACTER_WISE),
    )
  }
}
