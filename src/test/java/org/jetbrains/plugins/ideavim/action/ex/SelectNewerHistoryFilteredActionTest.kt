/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class SelectNewerHistoryFilteredActionTest : VimExTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enterCommand("set digraph")
    enterCommand("digraph")
    enterCommand("set incsearch")
    enterSearch("something cool")
    enterSearch("not cool")
    enterSearch("so cool")
  }

  @Test
  fun `test Down selects newer command history entry filtered by typed prefix`() {
    typeText(":set<S-Up>")  // Not filtered
    assertExText("set incsearch")
    typeText("<S-Up>")  // Not filtered
    assertExText("digraph")
    typeText("<S-Up>")  // Not filtered
    assertExText("set digraph")

    // Skips "digraph" because it doesn't match the "set" prefix
    typeText("<Down>")
    assertExText("set incsearch")
  }

  @Test
  fun `test Down selects last typed command value`() {
    typeText(":set<S-Up>")
    assertExText("set incsearch")

    typeText("<Down>")
    assertExText("set")
  }

  @Test
  fun `test Down selects newer search history entry filtered by typed prefix`() {
    typeText("/so<S-Up>") // Not filtered
    assertExText("so cool")
    typeText("<S-Up>")  // Not filtered
    assertExText("not cool")
    typeText("<S-Up>")  // Not filtered
    assertExText("something cool")

    // Skips "digraph" because it doesn't match the "set" prefix
    typeText("<Down>")
    assertExText("so cool")
  }

  @Test
  fun `test Down selects last typed search value`() {
    typeText("/so<S-Up>")
    assertExText("so cool")

    typeText("<Down>")
    assertExText("so")
  }
}
