/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import org.jetbrains.plugins.ideavim.VimTestCaseBase
import org.junit.jupiter.api.Test

class RedrawActionTest : VimTestCaseBase() {
  @Test
  fun `test redraw action clears status line`() {
    configureByText("lorem ipsum")
    enterSearch("dolor")  // Pattern not found: dolor
    assertStatusLineMessageContains("Pattern not found: dolor")
    typeText("<C-L>")
    assertStatusLineCleared()
  }
}
