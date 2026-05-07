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

class DigraphSetFunctionTest : VimTestCase("\n") {
  @Test
  fun `test digraph_set reports error when given less than two characters`() {
    enterCommand("call digraph_set('a', 'a')")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: a")
  }

  @Test
  fun `test digraph_set reports error when given more than two characters`() {
    enterCommand("call digraph_set('aaa', 'a')")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: aaa")
  }

  @Test
  fun `test digraph_set reports error when digraph is missing`() {
    enterCommand("call digraph_set('a:', '')")
    assertPluginError(true)
    assertPluginErrorMessage("E1215: Digraph must be one character: ")
  }

  @Test
  fun `test digraph_set reports error when digraph is more than one character`() {
    enterCommand("call digraph_set('a:', 'aa')")
    assertPluginError(true)
    assertPluginErrorMessage("E1215: Digraph must be one character: aa")
  }

  @Test
  fun `test digraph_set adds custom digraph`() {
    assertCommandOutput("echo digraph_set('(0', '⓪')", "1")
    typeText("i<C-K>(0<Esc>")
    assertState("⓪\n")
  }

  @Test
  fun `test digraph_set adds custom 32-bit digraph digraph`() {
    assertCommandOutput("echo digraph_set('co', '🟠')", "1")
    typeText("i<C-K>co<Esc>")
    assertState("🟠\n")
  }

  @Test
  fun `test digraph_set overrides builtin digraph`() {
    assertCommandOutput("echo digraph_set('OK', '✗')", "1")
    typeText("i<C-K>OK<Esc>")
    assertState("✗\n")
  }
}
