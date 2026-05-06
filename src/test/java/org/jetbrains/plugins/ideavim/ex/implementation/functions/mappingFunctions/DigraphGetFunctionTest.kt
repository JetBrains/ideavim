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

class DigraphGetFunctionTest : VimTestCase("\n") {
  @Test
  fun `test digraph_get returns character for known default digraph`() {
    assertCommandOutput("echo digraph_get('a:')", "ä")
  }

  @Test
  fun `test digraph_get returns character for another known default digraph`() {
    assertCommandOutput("echo digraph_get('A:')", "Ä")
  }

  @Test
  fun `test digraph_get returns second character as fallback for unknown digraph`() {
    assertCommandOutput("echo digraph_get('xy')", "y")
  }

  @Test
  fun `test digraph_get works with reversed character order`() {
    // getCharacterForDigraph tries both ch1+ch2 and ch2+ch1
    assertCommandOutput("echo digraph_get(':a')", "ä")
  }

  @Test
  fun `test digraph_get returns custom digraph added via digraph command`() {
    enterCommand("digraph (0 9450")
    assertCommandOutput("echo digraph_get('(0')", "⓪")
  }

  @Test
  fun `test digraph_get returns 32-bit Unicode character`() {
    // Char is 16 bit. We need to handle 32-bit characters
    enterCommand("digraph co 128992") // Orange circle. 128,992 > Char.MAX_VALUE
    assertCommandOutput("echo digraph_get('co')", "🟠")
  }

  @Test
  fun `test digraph_get throws E1214 for empty string`() {
    enterCommand("call digraph_get('')")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: ")
  }

  @Test
  fun `test digraph_get throws E1214 for single character`() {
    enterCommand("call digraph_get('a')")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: a")
  }

  @Test
  fun `test digraph_get throws E1214 for string longer than two characters`() {
    enterCommand("call digraph_get('abc')")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: abc")
  }
}
