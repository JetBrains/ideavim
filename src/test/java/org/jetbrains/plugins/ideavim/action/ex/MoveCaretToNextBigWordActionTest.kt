/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.junit.jupiter.api.Test

class MoveCaretToNextBigWordActionTest : VimExTestCase() {
  @VimBehaviorDiffers(description = "Moving one word right positions caret at end of previous word")
  @Test
  fun `test Shift-Right move caret one WORD right`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<S-Right>")
    // TODO: Vim moves caret to "set| ", while we move it to "set |"
    assertExOffset(4)

    typeText("<S-Right>")
    assertExOffset(13)
  }

  @Test
  fun `test CTRL-Right moves caret one WORD right`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<C-Right>")
    // TODO: Vim moves caret to "set| ", while we move it to "set |"
    assertExOffset(4)

    typeText("<C-Right>")
    assertExOffset(13)
  }

  // TODO: Add tests to confirm if Vim uses word or WORD
}
