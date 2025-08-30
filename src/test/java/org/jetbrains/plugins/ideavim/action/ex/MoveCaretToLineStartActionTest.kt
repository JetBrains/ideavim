/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class MoveCaretToLineStartActionTest : VimExTestCase() {
  @Test
  fun `test CTRL-B moves caret to beginning of line`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)
  }

  @Test
  fun `test Home moves caret to beginning of line`() {
    typeText(":set incsearch<Home>")
    assertExOffset(0)
  }
}
