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

class DigraphSetListFunctionTest : VimTestCase("\n") {
  @Test
  fun `test digraph_setlist with non-list argument reports error`() {
    enterCommand("call digraph_setlist('oops')")
    assertPluginError(true)
    assertPluginErrorMessage("E1216: digraph_setlist() argument must be a list of lists with two items")
  }

  @Test
  fun `test digraph_setlist with simple list argument reports error`() {
    enterCommand("call digraph_setlist([1])")
    assertPluginError(true)
    assertPluginErrorMessage("E1216: digraph_setlist() argument must be a list of lists with two items")
  }

  @Test
  fun `test digraph_setlist sets single digraph`() {
    assertCommandOutput("echo digraph_setlist([['(0', '⓪']])", "1")
    typeText("i", "<C-K>(0", "<Esc>")
    assertState("⓪\n")
  }

  @Test
  fun `test digraph_setlist sets multiple digraphs`() {
    assertCommandOutput("echo digraph_setlist([['(0', '⓪'], ['(z', 'ⓩ']])", "1")
    typeText("i", "<C-K>(0", "<C-K>(z", "<Esc>")
    assertState("⓪ⓩ\n")
  }

  @Test
  fun `test digraph_setlist overrides builtin digraphs`() {
    assertCommandOutput("echo digraph_setlist([['OK', '∥'], ['XX', '¶']])", "1")
    typeText("i", "<C-K>OK", "<C-K>XX", "<Esc>")
    assertState("∥¶\n")
  }

  @Test
  fun `test digraph_setlist sets 32-bit Unicode digraph`() {
    assertCommandOutput("echo digraph_setlist([['co', '🟠']])", "1")
    typeText("i", "<C-K>co", "<Esc>")
    assertState("🟠\n")
  }

  @Test
  fun `test digraph_setlist with list item with too few elements reports error`() {
    enterCommand("call digraph_setlist([ ['a'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1216: digraph_setlist() argument must be a list of lists with two items")
  }

  @Test
  fun `test digraph_setlist with list item with too many elements reports error`() {
    enterCommand("call digraph_setlist([ ['a', 'b', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1216: digraph_setlist() argument must be a list of lists with two items")
  }

  @Test
  fun `test digraph_setlist with list item with too few characters reports error`() {
    enterCommand("call digraph_setlist([ ['a', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: a")
  }

  @Test
  fun `test digraph_setlist with list item with too many characters reports error`() {
    enterCommand("call digraph_setlist([ ['abc', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: abc")
  }

  @Test
  fun `test digraph_setlist with list item with too many characters for digraph reports error`() {
    enterCommand("call digraph_setlist([ ['ab', 'cc'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1215: Digraph must be one character: cc")
  }

  @Test
  fun `test digraph_setlist with subsequent list item with too many elements reports error`() {
    enterCommand("call digraph_setlist([ ['(0', '⓪'], ['a', 'b', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1216: digraph_setlist() argument must be a list of lists with two items")
  }

  @Test
  fun `test digraph_setlist with subsequent list item with too few characters reports error`() {
    enterCommand("call digraph_setlist([ ['(0', '⓪'], ['a', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: a")
  }

  @Test
  fun `test digraph_setlist with subsequent list item with too many characters reports error`() {
    enterCommand("call digraph_setlist([ ['(0', '⓪'], ['abc', 'c'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1214: Digraph must be just two characters: abc")
  }

  @Test
  fun `test digraph_setlist with subsequent list item with too many characters for digraph reports error`() {
    enterCommand("call digraph_setlist([ ['(0', '⓪'], ['ab', 'cc'] ])")
    assertPluginError(true)
    assertPluginErrorMessage("E1215: Digraph must be one character: cc")
  }
}
