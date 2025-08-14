/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class MoveCaretToPreviousBigWordActionTest : VimExTestCase() {
  @Test
  fun `test Shift-Left moves caret one WORD left`() {
    typeText(":set incsearch<S-Left>")
    assertExOffset(4)
    typeText("<S-Left>")
    assertExOffset(0)
  }

  @Test
  fun `test CTRL-Left moves caret one WORD left`() {
    typeText(":set incsearch<C-Left>")
    assertExOffset(4)
    typeText("<C-Left>")
    assertExOffset(0)
  }
}
