/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.mappingFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class DigraphGetListFunctionTest : VimTestCase("\n") {
  @Test
  fun `test digraph_getlist returns empty list when no custom digraphs defined`() {
    assertCommandOutput("echo digraph_getlist()", "[]")
  }

  @Test
  fun `test digraph_getlist returns custom digraph`() {
    enterCommand("digraph (0 9450")
    assertCommandOutput("echo digraph_getlist()", "[['(0', '⓪']]")
  }


  @Test
  fun `test digraph_getlist returns multiple custom digraphs`() {
    enterCommand("digraph (0 9450")
    enterCommand("digraph (z 9449")
    assertCommandOutput("echo digraph_getlist()", "[['(0', '⓪'], ['(z', 'ⓩ']]")
  }

  @Test
  fun `test digraph_getlist returns 32-bit unicode digraph`() {
    enterCommand("digraph co 128992") // Orange circle. 128,992 > Char.MAX_VALUE
    assertCommandOutput("echo digraph_getlist()", "[['co', '🟠']]")
  }

  @Test
  fun `test digraph_getlist with false parameter returns custom digraphs`() {
    enterCommand("digraph (0 9450")
    enterCommand("digraph (z 9449")
    assertCommandOutput("echo digraph_getlist(0)", "[['(0', '⓪'], ['(z', 'ⓩ']]")
  }

  @Test
  fun `test digraph_getlist with true parameter returns all builtin digraphs`() {
    // There are too many digraphs to check the whole output. We know this test will fail if we add more custom digraphs
    assertCommandOutput("echo len(digraph_getlist(1))", "1363")
    assertCommandOutput("echo slice(digraph_getlist(1), 0, 5)", "[['!!', '|'], ['!)', '}'], ['!2', '‖'], ['!:', 'ἆ'], ['!<', '≮']]")
    assertCommandOutput("echo filter(digraph_getlist(1), 'v:val[0] == \"OK\"')", "[['OK', '✓']]")
  }

  @Test
  fun `test digraph_getlist with true parameter returns builtin and custom digraphs`() {
    enterCommand("digraph (0 9450")
    assertCommandOutput("echo len(digraph_getlist(1))", "1364")
    assertCommandOutput("echo filter(digraph_getlist(1), 'v:val[0] == \"OK\"')", "[['OK', '✓']]")
    assertCommandOutput("echo filter(digraph_getlist(1), 'v:val[0] == \"(0\"')", "[['(0', '⓪']]")
  }
}
