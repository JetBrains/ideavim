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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class InsertFunctionTest : VimTestCase("\n") {
  @Test
  fun `test insert at start of list if index not specified`() {
    assertCommandOutput("echo insert([1, 2, 3], 4)", "[4, 1, 2, 3]")
  }

  @Test
  fun `test insert at index 0 inserts at start of List`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, 0)", "[4, 1, 2, 3]")
  }

  @Test
  fun `test insert called as method`() {
    assertCommandOutput("echo [1,2,3]->insert(4, 0)", "[4, 1, 2, 3]")
  }

  @Test
  fun `test insert at index`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, 2)", "[1, 2, 4, 3]")
  }

  @Test
  fun `test insert any datatype at index`() {
    assertCommandOutput("echo insert([1, 2, 3], 'hello', 2)", "[1, 2, 'hello', 3]")
  }

  @Test
  fun `test insert modifies list in place`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = a")
    assertCommandOutput("echo insert(a, 4, 2)", "[1, 2, 4, 3]")
    assertCommandOutput("echo b", "[1, 2, 4, 3]")
  }

  @Test
  fun `test insert with String index converts to Number`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, '2')", "[1, 2, 4, 3]")
  }

  @Test
  fun `test insert with invalid String index converts to 0`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, 'foo')", "[4, 1, 2, 3]")
  }

  @Test
  fun `test insert with index equal to List length adds to end of List`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, 3)", "[1, 2, 3, 4]")
  }

  @Test
  fun `test insert at index -1 inserts from end of List`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, -1)", "[1, 2, 4, 3]")
  }

  @Test
  fun `test insert with negative index inserts from end of List`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, -2)", "[1, 4, 2, 3]")
  }

  @Test
  fun `test insert with negative index matching List length inserts at start of List`() {
    assertCommandOutput("echo insert([1, 2, 3], 4, -3)", "[4, 1, 2, 3]")
  }

  @VimBehaviorDiffers(description = "Vim reports an error and returns 0")
  @Test
  fun `test insert with index out of range reports error`() {
    enterCommand("echo insert([1, 2, 3], 4, 4)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: 4")
  }

  @VimBehaviorDiffers(description = "Vim reports an error and returns 0")
  @Test
  fun `test insert with negative index out of range reports error`() {
    enterCommand("echo insert([1, 2, 3], 4, -4)")
    assertPluginError(true)
    assertPluginErrorMessage("E684: List index out of range: -4")
  }

  @Test
  fun `test insert with Float index reports error`() {
    enterCommand("echo insert([1, 2, 3], 4, 2.3)")
    assertPluginError(true)
    assertPluginErrorMessage("E805: Using a Float as a Number")
  }

  @Test
  fun `test insert with List index reports error`() {
    enterCommand("echo insert([1, 2, 3], 2, [1, 2])")
    assertPluginError(true)
    assertPluginErrorMessage("E745: Using a List as a Number")
  }

  @Test
  fun `test insert with locked variable reports error`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar a")
    enterCommand("echo insert(a, 4, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: insert() argument")
  }

  // TODO: Fix locking
  @Disabled("IdeaVim's lock implementation doesn't differentiate between a locked list variable and a locked list")
  @Test
  fun `test insert with locked variable depth 0 can add new items`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar 0 a") // Lock the List variable. We can still change the list
    assertCommandOutput("echo insert(a, 4, 2)", "[1, 2, 4, 3]")
  }

  @Test
  fun `test insert with locked variable depth 1 reports error`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("lockvar 1 a") // Lock the List. We can't change the value of `a` and we can't add/remove items
    enterCommand("echo insert(a, 4, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: insert() argument")
  }

  @Test
  fun `test insert into List locked by another variable reports error`() {
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = a")
    enterCommand("lockvar b")
    enterCommand("echo insert(a, 4, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: insert() argument")
  }

  @Test
  fun `test insert into List locked as an item of another List reports error`() {
    // This is the same as "test insert into List locked by another variable reports error" above, but accessing through
    // the list index, rather than a variable
    enterCommand("let a = [1, 2, 3]")
    enterCommand("let b = [a]")
    enterCommand("lockvar b")
    enterCommand("echo insert(b[0], 4, 2)")
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: insert() argument")
  }
}
