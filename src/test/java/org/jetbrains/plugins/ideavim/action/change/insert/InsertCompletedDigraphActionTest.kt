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
    doTest(listOf("i", "<C-K>OK", "<Esc>"), "", "✓")
  }

  @Test
  fun `test insert digraph with reversed characters`() {
    doTest(listOf("i", "<C-K>KO", "<Esc>"), "", "✓")
  }

  @Test
  fun `test insert same character with different digraphs`() {
    doTest(listOf("i", "<C-K>Ct", "<C-K>c|", "<Esc>"), "", "¢¢")
  }

  @Test
  fun `test insert custom digraph`() {
    doTest(listOf("i", "<C-K>(0", "<Esc>"), "", "⓪") {
      enterCommand("digraph (0 9450")
    }
  }

  @Test
  fun `test insert custom digraph with reversed characters`() {
    doTest(listOf("i", "<C-K>0(", "<Esc>"), "", "⓪") {
      enterCommand("digraph (0 9450")
    }
  }

  @Test
  fun `test insert custom digraph overriding existing custom digraph`() {
    doTest(listOf("i", "<C-K>(0", "<Esc>"), "", "✓") {
      enterCommand("digraph (0 9450")
      enterCommand("digraph (0 10003")
    }
  }

  @Test
  fun `test insert custom digraph overriding existing default digraph`() {
    doTest(listOf("i", "<C-K>OK", "<Esc>"), "", "⓪") {
      enterCommand("digraph OK 9450")
    }
  }
}
