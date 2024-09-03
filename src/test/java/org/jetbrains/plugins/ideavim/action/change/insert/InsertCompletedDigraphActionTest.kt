/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertCompletedDigraphActionTest : VimTestCase() {
  @Test
  fun `test insert digraph`() {
    doTest("i<C-K>OK<Esc>", "", "✓")
  }

  @Test
  fun `test insert same character with different digraphs`() {
    doTest(listOf("i", "<C-K>Ct", "<C-K>c|", "<Esc>"), "", "¢¢")
  }
}
