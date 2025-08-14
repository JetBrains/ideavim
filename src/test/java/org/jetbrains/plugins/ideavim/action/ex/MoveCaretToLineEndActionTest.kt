/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class MoveCaretToLineEndActionTest : VimExTestCase() {
  @Test
  fun `test CTRL-B moves caret to end of line`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<C-E>")
    assertExOffset(13)
  }

  @Test
  fun `test End modes caret to end of line`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)

    typeText("<End>")
    assertExOffset(13)
  }
}
