/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class AddFunctionTest : VimTestCase("\n") {
  @Test
  fun `test add value to list`() {
    enterCommand("let myList = [1, 2, 3]")
    enterCommand("call add(myList, 4)")
    assertCommandOutput("echo myList", "[1, 2, 3, 4]")
  }

  @Test
  fun `test add value to list modifies current list`() {
    enterCommand("let myList = [1, 2, 3]")
    enterCommand("let b = myList")
    enterCommand("call add(b, 4)")
    assertCommandOutput("echo myList", "[1, 2, 3, 4]")
    assertCommandOutput("echo b", "[1, 2, 3, 4]")
  }

  @Test
  fun `test add value does not enforce item type`() {
    enterCommand("let myList = [1, 2, 3]")
    enterCommand("call add(myList, 'four')")
    assertCommandOutput("echo myList", "[1, 2, 3, 'four']")
  }

  @Test
  fun `test add value to String raises error`() {
    enterCommand("let a = \"hello\"")
    enterCommand("call add(a, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E897: List or Blob required")
  }

  @Test
  fun `test add value to Number raises error`() {
    enterCommand("let a = 42")
    enterCommand("call add(a, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E897: List or Blob required")
  }

  @VimBehaviorDiffers(description = "Vim reports two errors: E121 and E116")
  @Test
  fun `test add to undefined variable raises error`() {
    enterCommand("call add(a, 1)")
    assertPluginError(true)
    assertPluginErrorMessage("E121: Undefined variable: a")
    // Vim also reports "E116: Invalid arguments for function: add"
  }

  @Test
  fun `test add will not add if variable is locked`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar a")
    enterCommand("call add(a, 4)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: add() argument")
  }

  @Test
  fun `test call add as method`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("call a->add(4)->add(5)")
    assertCommandOutput("echo a", "[1, 2, 3, 4, 5]")
  }
}
